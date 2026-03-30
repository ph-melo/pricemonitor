package com.paulo.pricemonitor.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

@Service
public class MlEanSearchService {

    private static final Logger log = Logger.getLogger(MlEanSearchService.class.getName());

    private final RestClient restClient;

    public MlEanSearchService(@Value("${mercadolivre.api.base-url}") String baseUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("User-Agent", "PriceMonitor/1.0")
                .build();
    }

    public List<MlListingSnapshot> searchByEan(String ean) {
        List<MlListingSnapshot> results = new ArrayList<>();

        try {
            var searchResponse = restClient.get()
                    .uri("/sites/MLB/search?q={ean}&limit=50", ean)
                    .retrieve()
                    .toEntity(Map.class);

            if (searchResponse.getBody() == null) return results;

            var body = searchResponse.getBody();
            var items = (List<Map<String, Object>>) body.get("results");
            if (items == null) return results;

            for (Map<String, Object> item : items) {
                try {
                    String itemId = (String) item.get("id");
                    String title = (String) item.get("title");
                    String permalink = (String) item.get("permalink");

                    Number priceRaw = (Number) item.get("price");
                    if (priceRaw == null) continue;
                    BigDecimal price = new BigDecimal(priceRaw.toString());

                    String sellerName = null;
                    Long sellerId = null;
                    var seller = (Map<String, Object>) item.get("seller");
                    if (seller != null) {
                        sellerName = (String) seller.get("nickname");
                        Number sellerIdRaw = (Number) seller.get("id");
                        if (sellerIdRaw != null) sellerId = sellerIdRaw.longValue();
                    }

                    results.add(new MlListingSnapshot(itemId, title, permalink, price, sellerName, sellerId));
                } catch (Exception e) {
                    log.warning("Erro ao parsear item EAN=" + ean + ": " + e.getMessage());
                }
            }
        } catch (Exception e) {
            log.severe("Erro ao buscar EAN=" + ean + " no ML: " + e.getMessage());
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
