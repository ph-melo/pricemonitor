package com.paulo.pricemonitor.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MlListingResponse(
        Long id,
        Long enterpriseProductId,
        String productName,
        String ean,
        String marca,
        String mlItemId,
        String listingUrl,
        String sellerName,
        Long sellerId,
        String listingTitle,
        BigDecimal listedPrice,
        BigDecimal mapPrice,
        BigDecimal percentBelow,
        boolean violation,
        boolean seen,
        LocalDateTime detectedAt
) {}
