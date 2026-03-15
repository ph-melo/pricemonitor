package com.paulo.pricemonitor.controller;

import com.paulo.pricemonitor.marketplace.MarketItemProvider;
import com.paulo.pricemonitor.marketplace.mercadolivre.MercadoLivreUriParser;
import com.paulo.pricemonitor.marketplace.mercadolivre.ResolvedIds;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ScrapeController {

    private final MarketItemProvider marketItemProvider;
    private final MercadoLivreUriParser uriParser;

    public ScrapeController(MarketItemProvider marketItemProvider,
                            MercadoLivreUriParser uriParser) {
        this.marketItemProvider = marketItemProvider;
        this.uriParser = uriParser;
    }

    @GetMapping("/scrape/preview")
    public MarketItemProvider.ItemSnapshot preview(@RequestParam String url) {
        ResolvedIds ids = uriParser.parse(url);

        if (ids == null || ids.itemId() == null || ids.itemId().isBlank()) {
            throw new IllegalArgumentException("Não foi possível extrair itemId (MLB...) da URL informada.");
        }

        return marketItemProvider.fetch(url, ids.itemId(), ids.catalogId());
    }
}