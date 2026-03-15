package com.paulo.pricemonitor.marketplace;

public class MarketplaceTemporarilyBlockedException extends RuntimeException {
    public MarketplaceTemporarilyBlockedException(String message) {
        super(message);
    }
}