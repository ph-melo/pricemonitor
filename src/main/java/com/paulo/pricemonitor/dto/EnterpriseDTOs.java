package com.paulo.pricemonitor.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// ─── Request: cadastrar produto Enterprise ────────────────────────────────────
public record CreateEnterpriseProductRequest(
        Long userId,
        String ean,
        String productName,
        BigDecimal mapPrice,
        BigDecimal tolerancePercent
) {}

// ─── Response: produto Enterprise ────────────────────────────────────────────
public record EnterpriseProductResponse(
        Long id,
        String ean,
        String productName,
        BigDecimal mapPrice,
        BigDecimal tolerancePercent,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime lastCheckAt
) {}

// ─── Response: violação de MAP ────────────────────────────────────────────────
public record PriceViolationResponse(
        Long id,
        Long enterpriseProductId,
        String productName,
        String ean,
        String mlItemId,
        String listingUrl,
        String sellerName,
        String listingTitle,
        java.math.BigDecimal listedPrice,
        java.math.BigDecimal mapPrice,
        java.math.BigDecimal percentBelow,
        boolean seen,
        LocalDateTime detectedAt
) {}
