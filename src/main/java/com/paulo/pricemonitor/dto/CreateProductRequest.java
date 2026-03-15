package com.paulo.pricemonitor.dto;

import lombok.Data;

@Data
public class CreateProductRequest {
    private String productUrl;
    private Long userId;
}