package com.paulo.pricemonitor.marketplace.mercadolivre.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class MercadoLivreClientConfig {

    @Bean
    public WebClient mercadoLivreWebClient(
            @Value("${mercadolivre.api.base-url}") String baseUrl
    ) {
        return WebClient.builder()
                .baseUrl(baseUrl)
                .build();
    }
}