package com.paulo.pricemonitor.marketplace;

import java.math.BigDecimal;

public interface MarketItemProvider {

    ItemSnapshot fetch(String url, String itemId, String catalogId);

    record ItemSnapshot(
            String itemId,
            String catalogId,
            String url,
            String title,
            BigDecimal price,
            String currency
    ) {}
}