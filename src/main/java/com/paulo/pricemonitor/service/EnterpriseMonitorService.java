package com.paulo.pricemonitor.service;

import com.paulo.pricemonitor.entity.EnterpriseProduct;
import com.paulo.pricemonitor.entity.PriceViolation;
import com.paulo.pricemonitor.repository.EnterpriseProductRepository;
import com.paulo.pricemonitor.repository.PriceViolationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

@Service
@RequiredArgsConstructor
public class EnterpriseMonitorService {

    private static final Logger log = Logger.getLogger(EnterpriseMonitorService.class.getName());

    private final EnterpriseProductRepository enterpriseProductRepository;
    private final PriceViolationRepository priceViolationRepository;
    private final MlEanSearchService mlEanSearchService;

    public void checkAllEnterpriseProducts() {
        List<EnterpriseProduct> products = enterpriseProductRepository.findByActiveTrue();
        for (EnterpriseProduct p : products) {
            try {
                checkProduct(p);
            } catch (Exception e) {
                log.severe("Erro ao verificar produto Enterprise id=" + p.getId() +
                        " ean=" + p.getEan() + ": " + e.getMessage());
            }
        }
    }

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

            BigDecimal diff = mapPrice.subtract(listedPrice);
            if (diff.compareTo(BigDecimal.ZERO) <= 0) continue;

            BigDecimal percentBelow = diff
                    .divide(mapPrice, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(2, RoundingMode.HALF_UP);

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

            log.info("Violação MAP: EAN=" + product.getEan() +
                    " vendedor=" + listing.sellerName() +
                    " preço=" + listedPrice +
                    " MAP=" + mapPrice +
                    " abaixo=" + percentBelow + "%");
        }

        product.setLastCheckAt(LocalDateTime.now());
        enterpriseProductRepository.save(product);

        return violations;
    }
}
