package com.paulo.pricemonitor.marketplace.mercadolivre.client;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
@RequiredArgsConstructor
public class MercadoLivreClient {

    private final WebClient mercadoLivreWebClient;

    public MercadoLivreItemResponse getItem(String itemId) {
        return mercadoLivreWebClient.get()
                .uri("/items/{itemId}", itemId)
                .retrieve()
                .bodyToMono(MercadoLivreItemResponse.class)
                .block();
    }
}