package com.paulo.pricemonitor.service;

import com.paulo.pricemonitor.entity.PriceHistory;
import com.paulo.pricemonitor.entity.Product;
import com.paulo.pricemonitor.marketplace.MarketItemProvider;
import com.paulo.pricemonitor.monitor.MonitorStatus;
import com.paulo.pricemonitor.repository.PriceHistoryRepository;
import com.paulo.pricemonitor.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PriceMonitorService {

    private final ProductRepository productRepository;
    private final PriceHistoryRepository priceHistoryRepository;
    private final MarketItemProvider marketItemProvider;
    private final EmailService emailService;

    @Transactional
    public void checkAllActiveProducts() {
        for (Product p : productRepository.findByActiveTrue()) {
            checkOne(p, "JOB", false);
        }
    }

    @Transactional
    public Product checkOneByUser(Long userId, Long productId) {
        if (userId == null || productId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "userId and productId are required");
        }

        Product p = productRepository.findByUserIdAndId(userId, productId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Product not found for userId=" + userId + " productId=" + productId
                ));

        checkOne(p, "MANUAL", true);
        return p;
    }

    @Transactional
    public void checkOne(Product product) {
        checkOne(product, "JOB", false);
    }

    private void checkOne(Product product, String strategy, boolean throwOnError) {

        if (!product.isActive()) {
            product.setLastStatus(MonitorStatus.PAUSED);
            product.setLastError(null);
            product.setLastCheckAt(LocalDateTime.now());
            productRepository.save(product);
            return;
        }

        try {
            var snapshot = marketItemProvider.fetch(
                    product.getProductUrl(),
                    product.getItemId(),
                    product.getCatalogId()
            );

            priceHistoryRepository.save(new PriceHistory(
                    product,
                    snapshot.price(),
                    snapshot.currency(),
                    java.time.Instant.now(),
                    strategy
            ));

            BigDecimal previousPrice = product.getLastPrice();
            boolean hadPriceBefore = previousPrice != null;
            boolean changed = !hadPriceBefore || snapshot.price().compareTo(previousPrice) != 0;

            // ✅ e-mail SOMENTE quando o preço cai
            if (changed && hadPriceBefore && snapshot.price().compareTo(previousPrice) < 0) {
                emailService.sendPriceDropEmail(
                        product.getUser().getEmail(),
                        snapshot.title(),
                        product.getProductUrl(),
                        previousPrice.toString(),
                        snapshot.price().toString()
                );
            }

            if (!hadPriceBefore) {
                product.setLastStatus(MonitorStatus.INITIAL);
            } else if (changed) {
                product.setLastStatus(MonitorStatus.CHANGED);
            } else {
                product.setLastStatus(MonitorStatus.NO_CHANGE);
            }

            product.setLastError(null);
            product.setLastPrice(snapshot.price());
            product.setCurrency(snapshot.currency());
            product.setTitle(safeTrim(snapshot.title()));
            product.setLastCheckAt(LocalDateTime.now());

            productRepository.save(product);

        } catch (Exception e) {
            product.setLastStatus(MonitorStatus.ERROR);
            product.setLastError(trimTo1000(e.getMessage()));
            product.setLastCheckAt(LocalDateTime.now());
            productRepository.save(product);

            if (throwOnError) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "Failed to fetch product snapshot",
                        e
                );
            }
        }
    }

    private String safeTrim(String v) {
        if (v == null) return null;
        String t = v.trim();
        return t.isEmpty() ? null : t;
    }

    private String trimTo1000(String msg) {
        if (msg == null) return "Unknown error";
        msg = msg.trim();
        if (msg.length() <= 1000) return msg;
        return msg.substring(0, 1000);
    }
}
