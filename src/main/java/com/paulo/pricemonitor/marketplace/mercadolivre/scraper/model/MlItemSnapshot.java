package com.paulo.pricemonitor.marketplace.mercadolivre.scraper.model;

import java.math.BigDecimal;

public record MlItemSnapshot(
        String title,
        BigDecimal price,
        String currency,
        String strategy
) {}