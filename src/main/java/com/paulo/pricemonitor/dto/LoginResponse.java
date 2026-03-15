package com.paulo.pricemonitor.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoginResponse {
    private Long userId;
    private String name;
    private String email;
    private String token;
    private String plan;        // FREE | PRO
    private int maxProducts;    // 3 | 15
}