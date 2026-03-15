package com.paulo.pricemonitor.marketplace.mercadolivre.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Resposta mínima do GET /items/{itemId}.
 * A API tem muitos campos; a gente mapeia só o que precisamos.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record MercadoLivreItemResponse(
        String id,
        String title,
        @JsonProperty("catalog_product_id") String catalogProductId
) {
}