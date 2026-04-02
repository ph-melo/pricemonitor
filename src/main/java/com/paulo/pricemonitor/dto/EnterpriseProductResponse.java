package com.paulo.pricemonitor.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record EnterpriseProductResponse(
        Long id,
        String ean,
        String productName,
        String marca,
        BigDecimal mapPrice,
        BigDecimal tolerancePercent,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime lastCheckAt,
        long totalListings,
        long totalViolations
) {}
