package com.paulo.pricemonitor.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class ProductResponse {

    private Long id;
    private String externalId;
    private String marketplace;
    private String title;
    private String productUrl;
    private String catalogId;
    private String itemId;
    private BigDecimal lastPrice;
    private String currency;
    private LocalDateTime lastCheckAt;
    private String trackingType;
    private String lastStatus;
    private String lastError;
}
