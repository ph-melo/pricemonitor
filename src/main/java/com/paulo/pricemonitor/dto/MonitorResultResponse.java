package com.paulo.pricemonitor.dto;

import lombok.Builder;

@Builder
public record MonitorResultResponse(
        String status,          // OK | CAPTCHA | ERROR
        String message,         // opcional
        ProductResponse product // produto atualizado
) {}