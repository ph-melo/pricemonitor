package com.paulo.pricemonitor.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class MlEanSearchService {

    private final RestClient restClient;

    public MlEanSearchService(@Value("${mercadolivre.api.base-url}") String baseUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("User-Agent", "PriceMonitor/1.0")
                .build();
    }

    /**
     * Busca todos os anúncios ativos no ML para um dado EAN/GTIN.
     * Retorna lista de snapshots com itemId, título, preço, vendedor e URL.
     */
    public List<MlListingSnapshot> searchByEan(String ean) {
        List<MlListingSnapshot> results = new ArrayList<>();

        try {
            // 1. Busca itens pelo EAN no site MLB (Brasil)
            var searchResponse = restClient.get()
                    .uri("/sites/MLB/search?q={ean}&limit=50", ean)
                    .retrieve()
                    .toEntity(Map.class);

            if (searchResponse.getBody() == null) return results;

            var body = searchResponse.getBody();
            var results2 = (List<Map<String, Object>>) body.get("results");
            if (results2 == null) return results;

            for (Map<String, Object> item : results2) {
                try {
                    String itemId = (String) item.get("id");
                    String title = (String) item.get("title");
                    String permalink = (String) item.get("permalink");

                    // Preço pode vir como Integer ou Double
                    Number priceRaw = (Number) item.get("price");
                    if (priceRaw == null) continue;
                    BigDecimal price = new BigDecimal(priceRaw.toString());

                    // Vendedor
                    String sellerName = null;
                    Long sellerId = null;
                    var seller = (Map<String, Object>) item.get("seller");
                    if (seller != null) {
                        sellerName = (String) seller.get("nickname");
                        Number sellerIdRaw = (Number) seller.get("id");
                        if (sellerIdRaw != null) sellerId = sellerIdRaw.longValue();
                    }

                    results.add(new MlListingSnapshot(
                            itemId, title, permalink, price, sellerName, sellerId
                    ));
                } catch (Exception e) {
                    log.warn("Erro ao parsear item EAN={}: {}", ean, e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("Erro ao buscar EAN={} no ML: {}", ean, e.getMessage());
        }

        return results;
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
