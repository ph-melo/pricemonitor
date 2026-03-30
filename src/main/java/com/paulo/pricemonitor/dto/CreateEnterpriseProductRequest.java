package com.paulo.pricemonitor.dto;

import java.math.BigDecimal;

public record CreateEnterpriseProductRequest(
        Long userId,
        String ean,
        String productName,
        BigDecimal mapPrice,
        BigDecimal tolerancePercent
) {}
