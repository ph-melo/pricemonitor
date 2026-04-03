package com.paulo.pricemonitor.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class MlEanSearchService {

    private static final Logger log = LoggerFactory.getLogger(MlEanSearchService.class);

    private final RestClient restClient;
    private final MlTokenService tokenService;

    public MlEanSearchService(
            @Value("${mercadolivre.api.base-url}") String baseUrl,
            MlTokenService tokenService) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("User-Agent", "PriceMonitor/1.0")
                .build();
        this.tokenService = tokenService;
    }

    @SuppressWarnings("unchecked")
    public List<MlListingSnapshot> searchByEan(String ean) {
        List<MlListingSnapshot> results = new ArrayList<>();
        String token = tokenService.getAccessToken();

        try {
            // Passo 1: busca o product_id pelo EAN no catálogo do ML
            var productSearch = restClient.get()
                    .uri("/products/search?site_id=MLB&status=active&q={ean}", ean)
                    .header("Authorization", "Bearer " + token)
                    .retrieve()
                    .toEntity(Map.class);

            if (productSearch.getBody() == null) {
                log.warn("[ML Search] Resposta vazia para EAN={}", ean);
                return results;
            }

            var products = (List<Map<String, Object>>) productSearch.getBody().get("results");
            if (products == null || products.isEmpty()) {
                log.info("[ML Search] Nenhum produto encontrado no catálogo para EAN={}", ean);
                return results;
            }

            log.info("[ML Search] EAN={} → {} produto(s) no catálogo", ean, products.size());

            for (Map<String, Object> product : products) {
                String productId = (String) product.get("id");
                if (productId == null) continue;
                fetchListingsByProductId(productId, token, results);
            }

        } catch (Exception e) {
            log.error("[ML Search] Erro ao buscar EAN={} no ML: {}", ean, e.getMessage());
        }

        return results;
    }

    @SuppressWarnings("unchecked")
    private void fetchListingsByProductId(String productId, String token, List<MlListingSnapshot> results) {
        try {
            var searchResponse = restClient.get()
                    .uri("/products/{productId}/items?status=active&limit=50", productId)
                    .header("Authorization", "Bearer " + token)
                    .retrieve()
                    .toEntity(Map.class);

            if (searchResponse.getBody() == null) return;

            // O endpoint retorna objetos completos diretamente em results
            var rawResults = (List<Map<String, Object>>) searchResponse.getBody().get("results");
            if (rawResults == null || rawResults.isEmpty()) {
                log.info("[ML Search] Nenhum anúncio para product_id={}", productId);
                return;
            }

            log.info("[ML Search] product_id={} → {} anúncio(s) encontrado(s)", productId, rawResults.size());

            for (Map<String, Object> item : rawResults) {
                try {
                    // O campo é item_id, não id
                    String itemId = (String) item.get("item_id");
                    if (itemId == null) continue;

                    Number priceRaw = (Number) item.get("price");
                    if (priceRaw == null) continue;
                    BigDecimal price = new BigDecimal(priceRaw.toString());

                    Long sellerId = null;
                    Number sellerIdRaw = (Number) item.get("seller_id");
                    if (sellerIdRaw != null) sellerId = sellerIdRaw.longValue();

                    // Monta permalink a partir do item_id
                    String permalink = "https://www.mercadolivre.com.br/p/" + itemId;

                    results.add(new MlListingSnapshot(itemId, null, permalink, price, null, sellerId));

                } catch (Exception e) {
                    log.warn("[ML Search] Erro ao parsear item: {}", e.getMessage());
                }
            }

        } catch (Exception e) {
            log.error("[ML Search] Erro ao buscar anúncios de product_id={}: {}", productId, e.getMessage());
        }
    }

    public record MlListingSnapshot(
            String itemId,
            String title,
            String permalink,
            BigDecimal price,
            String sellerName,
            Long sellerId
    ) {}
}