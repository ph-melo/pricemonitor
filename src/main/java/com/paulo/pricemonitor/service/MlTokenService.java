package com.paulo.pricemonitor.service;

import com.paulo.pricemonitor.entity.AppConfig;
import com.paulo.pricemonitor.repository.AppConfigRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.Map;

@Service
public class MlTokenService {

    private static final Logger log = LoggerFactory.getLogger(MlTokenService.class);
    private static final String REFRESH_TOKEN_KEY = "ml.refresh_token";

    @Value("${ml.client-id}")
    private String clientId;

    @Value("${ml.client-secret}")
    private String clientSecret;

    // Usado apenas na primeira inicialização se o banco ainda não tiver o token
    @Value("${ml.refresh-token}")
    private String refreshTokenFallback;

    private String accessToken;
    private Instant expiresAt = Instant.MIN;

    private final RestClient restClient = RestClient.create();
    private final AppConfigRepository configRepository;

    public MlTokenService(AppConfigRepository configRepository) {
        this.configRepository = configRepository;
    }

    // Ao subir, garante que o refresh_token existe no banco
    @PostConstruct
    public void init() {
        boolean existsInDb = configRepository.findByKey(REFRESH_TOKEN_KEY).isPresent();
        if (!existsInDb) {
            log.info("[ML Token] refresh_token não encontrado no banco. Salvando valor inicial da env var.");
            configRepository.save(new AppConfig(REFRESH_TOKEN_KEY, refreshTokenFallback));
        } else {
            log.info("[ML Token] refresh_token carregado do banco com sucesso.");
        }
    }

    public synchronized String getAccessToken() {
        if (Instant.now().isAfter(expiresAt.minusSeconds(300))) {
            log.info("[ML Token] Token expirado ou próximo de expirar, renovando...");
            refresh();
        }
        return accessToken;
    }

    @SuppressWarnings("unchecked")
    private void refresh() {
        try {
            // Lê o refresh_token mais recente do banco
            String currentRefreshToken = configRepository.findByKey(REFRESH_TOKEN_KEY)
                    .map(AppConfig::getValue)
                    .orElseThrow(() -> new IllegalStateException("refresh_token não encontrado no banco"));

            String body = "grant_type=refresh_token"
                    + "&client_id=" + clientId
                    + "&client_secret=" + clientSecret
                    + "&refresh_token=" + currentRefreshToken;

            var response = restClient.post()
                    .uri("https://api.mercadolibre.com/oauth/token")
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .header("Accept", "application/json")
                    .body(body)
                    .retrieve()
                    .toEntity(Map.class);

            var responseBody = response.getBody();
            if (responseBody == null) throw new IllegalStateException("Resposta vazia ao renovar token ML");

            this.accessToken = (String) responseBody.get("access_token");

            // Persiste o novo refresh_token no banco imediatamente
            String newRefreshToken = (String) responseBody.get("refresh_token");
            if (newRefreshToken != null && !newRefreshToken.isBlank()) {
                AppConfig config = configRepository.findByKey(REFRESH_TOKEN_KEY)
                        .orElse(new AppConfig(REFRESH_TOKEN_KEY, newRefreshToken));
                config.setValue(newRefreshToken);
                configRepository.save(config);
                log.info("[ML Token] Novo refresh_token persistido no banco.");
            }

            int expiresIn = (int) responseBody.get("expires_in");
            this.expiresAt = Instant.now().plusSeconds(expiresIn);
            log.info("[ML Token] Token renovado com sucesso. Expira em {}s.", expiresIn);

        } catch (Exception e) {
            log.error("[ML Token] Falha ao renovar token: {}", e.getMessage());
            throw new RuntimeException("Não foi possível renovar o token do Mercado Livre", e);
        }
    }
}