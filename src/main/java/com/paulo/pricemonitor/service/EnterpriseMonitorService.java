package com.paulo.pricemonitor.service;

import com.paulo.pricemonitor.entity.EnterpriseProduct;
import com.paulo.pricemonitor.entity.MlListing;
import com.paulo.pricemonitor.repository.EnterpriseProductRepository;
import com.paulo.pricemonitor.repository.MlListingRepository;
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
    private final MlListingRepository mlListingRepository;
    private final MlEanSearchService mlEanSearchService;

    public void checkAllEnterpriseProducts() {
        for (EnterpriseProduct p : enterpriseProductRepository.findByActiveTrue()) {
            try {
                checkProduct(p);
            } catch (Exception e) {
                log.severe("Erro ao verificar produto Enterprise id=" + p.getId() +
                        " ean=" + p.getEan() + ": " + e.getMessage());
            }
        }
    }

    @Transactional
    public List<MlListing> checkProductById(Long userId, Long productId) {
        EnterpriseProduct product = enterpriseProductRepository
                .findByIdAndUserId(productId, userId)
                .orElseThrow(() -> new RuntimeException("Produto não encontrado"));
        return checkProduct(product);
    }

    @Transactional
    public List<MlListing> checkProduct(EnterpriseProduct product) {
        // Remove listings anteriores do produto antes de re-escanear
        mlListingRepository.deleteByEnterpriseProductId(product.getId());

        List<MlEanSearchService.MlListingSnapshot> snapshots =
                mlEanSearchService.searchByEan(product.getEan());

        List<MlListing> saved = new ArrayList<>();

        for (MlEanSearchService.MlListingSnapshot snap : snapshots) {
            BigDecimal mapPrice = product.getMapPrice();
            BigDecimal listedPrice = snap.price();

            BigDecimal diff = mapPrice.subtract(listedPrice);
            boolean isViolation = false;
            BigDecimal percentBelow = null;

            if (diff.compareTo(BigDecimal.ZERO) > 0) {
                percentBelow = diff
                        .divide(mapPrice, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .setScale(2, RoundingMode.HALF_UP);

                if (percentBelow.compareTo(product.getTolerancePercent()) > 0) {
                    isViolation = true;
                }
            }

            MlListing listing = MlListing.builder()
                    .enterpriseProduct(product)
                    .mlItemId(snap.itemId())
                    .listingUrl(snap.permalink())
                    .sellerName(snap.sellerName())
                    .sellerId(snap.sellerId())
                    .listingTitle(snap.title())
                    .listedPrice(listedPrice)
                    .mapPrice(mapPrice)
                    .percentBelow(percentBelow)
                    .violation(isViolation)
                    .seen(false)
                    .build();

            saved.add(mlListingRepository.save(listing));
        }

        product.setLastCheckAt(LocalDateTime.now());
        enterpriseProductRepository.save(product);

        long violations = saved.stream().filter(MlListing::isViolation).count();
        log.info("EAN=" + product.getEan() + " anuncios=" + saved.size() + " violacoes=" + violations);

        return saved;
    }
}
