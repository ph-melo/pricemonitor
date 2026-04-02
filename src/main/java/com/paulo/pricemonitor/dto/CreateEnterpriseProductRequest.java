package com.paulo.pricemonitor.dto;

import java.math.BigDecimal;

public record CreateEnterpriseProductRequest(
        Long userId,
        String ean,
        String productName,
        String marca,
        BigDecimal mapPrice,
        BigDecimal tolerancePercent
) {}
