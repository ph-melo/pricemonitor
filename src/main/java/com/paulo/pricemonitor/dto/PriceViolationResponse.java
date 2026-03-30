package com.paulo.pricemonitor.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PriceViolationResponse(
        Long id,
        Long enterpriseProductId,
        String productName,
        String ean,
        String mlItemId,
        String listingUrl,
        String sellerName,
        String listingTitle,
        BigDecimal listedPrice,
        BigDecimal mapPrice,
        BigDecimal percentBelow,
        boolean seen,
        LocalDateTime detectedAt
) {}
