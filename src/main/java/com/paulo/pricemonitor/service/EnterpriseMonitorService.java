package com.paulo.pricemonitor.service;

import com.paulo.pricemonitor.entity.EnterpriseProduct;
import com.paulo.pricemonitor.entity.PriceViolation;
import com.paulo.pricemonitor.repository.EnterpriseProductRepository;
import com.paulo.pricemonitor.repository.PriceViolationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EnterpriseMonitorService {

    private final EnterpriseProductRepository enterpriseProductRepository;
    private final PriceViolationRepository priceViolationRepository;
    private final MlEanSearchService mlEanSearchService;

    /**
     * Verifica todos os produtos Enterprise ativos — chamado pelo job agendado.
     */
    public void checkAllEnterpriseProducts() {
        List<EnterpriseProduct> products = enterpriseProductRepository.findByActiveTrue();
        for (EnterpriseProduct p : products) {
            try {
                checkProduct(p);
            } catch (Exception e) {
                log.error("Erro ao verificar produto Enterprise id={} ean={}: {}",
                        p.getId(), p.getEan(), e.getMessage());
            }
        }
    }

    /**
     * Verifica um produto específico manualmente.
     */
    @Transactional
    public List<PriceViolation> checkProductById(Long userId, Long productId) {
        EnterpriseProduct product = enterpriseProductRepository
                .findByIdAndUserId(productId, userId)
                .orElseThrow(() -> new RuntimeException("Produto não encontrado"));

        return checkProduct(product);
    }

    @Transactional
    public List<PriceViolation> checkProduct(EnterpriseProduct product) {
        List<MlEanSearchService.MlListingSnapshot> listings =
                mlEanSearchService.searchByEan(product.getEan());

        List<PriceViolation> violations = new ArrayList<>();

        for (MlEanSearchService.MlListingSnapshot listing : listings) {
            BigDecimal mapPrice = product.getMapPrice();
            BigDecimal listedPrice = listing.price();

            // Calcula % abaixo do MAP
            BigDecimal diff = mapPrice.subtract(listedPrice);
            if (diff.compareTo(BigDecimal.ZERO) <= 0) continue; // preço ok ou acima do MAP

            BigDecimal percentBelow = diff
                    .divide(mapPrice, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(2, RoundingMode.HALF_UP);

            // Só alerta se ultrapassar a tolerância configurada
            if (percentBelow.compareTo(product.getTolerancePercent()) <= 0) continue;

            PriceViolation violation = PriceViolation.builder()
                    .enterpriseProduct(product)
                    .mlItemId(listing.itemId())
                    .listingUrl(listing.permalink())
                    .sellerName(listing.sellerName())
                    .sellerId(listing.sellerId())
                    .listingTitle(listing.title())
                    .listedPrice(listedPrice)
                    .mapPrice(mapPrice)
                    .percentBelow(percentBelow)
                    .build();

            priceViolationRepository.save(violation);
            violations.add(violation);

            log.info("Violação MAP detectada: EAN={} vendedor={} preço={} MAP={} abaixo={}%",
                    product.getEan(), listing.sellerName(), listedPrice, mapPrice, percentBelow);
        }

        // Atualiza lastCheckAt
        product.setLastCheckAt(LocalDateTime.now());
        enterpriseProductRepository.save(product);

        return violations;
    }
}
