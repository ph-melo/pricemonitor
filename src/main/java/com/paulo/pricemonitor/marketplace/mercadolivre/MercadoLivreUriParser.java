package com.paulo.pricemonitor.marketplace.mercadolivre;

import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Responsável apenas por interpretar URLs do Mercado Livre.
 * - Não acessa banco
 * - Não chama API externa
 *
 * Extrai:
 * - itemId (MLB...) via query params (wid, item_id) ou path
 * - catalogId via /p/{id} ou /up/{id}
 */
@Component
public class MercadoLivreUriParser {

    // Captura um ID do tipo MLB123... em qualquer lugar do texto
    private static final Pattern MLB_ID_PATTERN = Pattern.compile("\\bMLB\\d+\\b");

    public ResolvedIds parse(String url) {
        if (url == null || url.isBlank()) {
            return new ResolvedIds(null, null);
        }

        String trimmed = url.trim();

        String catalogId = extractCatalogId(trimmed);
        String itemId = extractItemId(trimmed);

        return new ResolvedIds(catalogId, itemId);
    }

    /**
     * catalogId pode aparecer assim:
     * - /p/MLBxxxx
     * - /up/MLBUxxxx (ou /up/MLBxxxx)
     */
    private String extractCatalogId(String url) {
        String id = extractAfterSegment(url, "/p/");
        if (id != null) return id;

        id = extractAfterSegment(url, "/up/");
        return id;
    }

    private String extractAfterSegment(String url, String segment) {
        int idx = url.indexOf(segment);
        if (idx == -1) return null;

        String after = url.substring(idx + segment.length());

        int endQ = after.indexOf("?");
        int endH = after.indexOf("#");

        int end;
        if (endQ == -1 && endH == -1) end = after.length();
        else if (endQ == -1) end = endH;
        else if (endH == -1) end = endQ;
        else end = Math.min(endQ, endH);

        String value = after.substring(0, end).trim();
        return value.isEmpty() ? null : value;
    }

    /**
     * itemId pode aparecer:
     * - wid=MLB...
     * - item_id=MLB...
     * - no path (ex: .../MLB1234567890 ...)
     */
    private String extractItemId(String url) {
        // 1) tenta pegar via query params
        String fromQuery = extractItemIdFromQuery(url);
        if (fromQuery != null) return fromQuery;

        // 2) tenta pegar via path
        String fromPath = extractItemIdFromPath(url);
        if (fromPath != null) return fromPath;

        return null;
    }

    private String extractItemIdFromQuery(String url) {
        try {
            URI uri = URI.create(url);
            String query = uri.getRawQuery();
            if (query == null || query.isBlank()) return null;

            Map<String, String> params = splitQuery(query);

            String wid = params.get("wid");
            if (wid != null && !wid.isBlank()) return wid.trim();

            String itemId = params.get("item_id");
            if (itemId != null && !itemId.isBlank()) return itemId.trim();

            return null;
        } catch (Exception e) {
            // URL não parseável => não acha itemId via query
            return null;
        }
    }

    private String extractItemIdFromPath(String url) {
        try {
            URI uri = URI.create(url);
            String path = uri.getPath();
            if (path == null || path.isBlank()) return null;

            Matcher m = MLB_ID_PATTERN.matcher(path);
            if (m.find()) return m.group();

            return null;
        } catch (Exception e) {
            // fallback: tenta no texto inteiro
            Matcher m = MLB_ID_PATTERN.matcher(url);
            if (m.find()) return m.group();
            return null;
        }
    }

    private Map<String, String> splitQuery(String query) {
        String[] pairs = query.split("&");
        Map<String, String> map = new HashMap<>();

        for (String pair : pairs) {
            int idx = pair.indexOf('=');
            if (idx <= 0) continue;

            String key = decode(pair.substring(0, idx));
            String val = decode(pair.substring(idx + 1));

            map.put(key, val);
        }

        return map;
    }

    private String decode(String s) {
        try {
            return URLDecoder.decode(s, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return s;
        }
    }
}