package com.paulo.pricemonitor.marketplace.mercadolivre.scraper;

import com.paulo.pricemonitor.marketplace.MarketItemProvider;
import com.paulo.pricemonitor.marketplace.mercadolivre.scraper.model.MlItemSnapshot;
import org.springframework.stereotype.Service;

@Service
public class MercadoLivreScraperClient implements MarketItemProvider {

    private final MlHtmlFetcher fetcher;
    private final MlHtmlParser parser;

    public MercadoLivreScraperClient(MlHtmlFetcher fetcher, MlHtmlParser parser) {
        this.fetcher = fetcher;
        this.parser = parser;
    }

    @Override
    public ItemSnapshot fetch(String url, String itemId, String catalogId) {
        String html = fetcher.getHtml(url);
        MlItemSnapshot parsed = parser.parse(html);

        if (parsed.title() == null || parsed.price() == null) {
            throw new IllegalStateException("Falha ao extrair title/price. strategy=" + parsed.strategy());
        }

        return new ItemSnapshot(
                itemId,
                catalogId,
                url,
                parsed.title(),
                parsed.price(),
                parsed.currency() != null ? parsed.currency() : "BRL"
        );
    }
}