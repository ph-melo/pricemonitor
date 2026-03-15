package com.paulo.pricemonitor.marketplace.mercadolivre.scraper;

import com.paulo.pricemonitor.marketplace.mercadolivre.scraper.model.MlItemSnapshot;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class MlHtmlParser {

    public MlItemSnapshot parse(String html) {
        Document doc = Jsoup.parse(html);

        // 1) Dados embutidos no JS da página (mais confiável — preço real exibido)
        MlItemSnapshot embedded = tryEmbeddedJson(html);
        if (embedded.price() != null && embedded.title() != null) return embedded;

        // 2) JSON-LD
        MlItemSnapshot jsonLd = tryJsonLd(doc);
        if (jsonLd.price() != null && jsonLd.title() != null) return jsonLd;

        // 3) Meta tags
        MlItemSnapshot meta = tryMeta(doc);
        if (meta.price() != null && meta.title() != null) return meta;

        // 4) HTML fallback
        return tryHtmlFallback(doc);
    }

    // ─── Estratégia 1: JSON embutido no JS ──────────────────────────────────
    // O ML injeta os dados do produto num JSON dentro de tags <script> como:
    //   "actual_price":{"currency_id":"BRL","amount":64,"cents":99,...}
    //   "price":{"currency_id":"BRL","amount":64,"cents":99,...}
    // Esses campos refletem o preço REAL exibido na página.

    private MlItemSnapshot tryEmbeddedJson(String html) {

        // Tenta extrair amount + cents separados (formato mais confiável)
        BigDecimal price = extractAmountAndCents(html);

        // Se não achou, tenta actual_price como decimal direto: "actual_price":64.99
        if (price == null) {
            price = extractDecimalField(html, "actual_price");
        }

        // Título via og:title ou <title>
        String title = extractMetaOrTitle(html);

        if (price != null) {
            return new MlItemSnapshot(title, price, "BRL", "embedded-js");
        }
        return new MlItemSnapshot(null, null, null, "embedded-miss");
    }

    /**
     * Extrai preço do formato: "amount":64,"cents":99
     * ou: "amount":64,"cents":99,"currency_id":"BRL"
     * Esse é o formato interno do ML e representa exatamente o que aparece na tela.
     */
    private BigDecimal extractAmountAndCents(String html) {
        // Padrão: "amount": NUMBER , ... "cents": NUMBER
        // Tenta dentro de blocos que contenham "actual_price" ou "price"
        Pattern blockPattern = Pattern.compile(
                "(?:actual_price|\"price\")\\s*[\":].*?\"amount\"\\s*:\\s*(\\d+).*?\"cents\"\\s*:\\s*(\\d+)",
                Pattern.DOTALL
        );
        Matcher m = blockPattern.matcher(html);
        if (m.find()) {
            try {
                long amount = Long.parseLong(m.group(1));
                long cents  = Long.parseLong(m.group(2));
                return new BigDecimal(amount + "." + String.format("%02d", cents));
            } catch (Exception ignored) {}
        }

        // Fallback: qualquer "amount":N,"cents":N próximos (dentro de 200 chars)
        Pattern amountPattern = Pattern.compile("\"amount\"\\s*:\\s*(\\d+)");
        Pattern centsPattern  = Pattern.compile("\"cents\"\\s*:\\s*(\\d+)");
        Matcher am = amountPattern.matcher(html);
        while (am.find()) {
            int start = am.start();
            int end   = Math.min(start + 200, html.length());
            String snippet = html.substring(start, end);
            Matcher cm = centsPattern.matcher(snippet);
            if (cm.find()) {
                try {
                    long amount = Long.parseLong(am.group(1));
                    long cents  = Long.parseLong(cm.group(1));
                    // sanidade: amount deve ser razoável (R$1 a R$99.999)
                    if (amount >= 1 && amount <= 99_999) {
                        return new BigDecimal(amount + "." + String.format("%02d", cents));
                    }
                } catch (Exception ignored) {}
            }
        }
        return null;
    }

    /**
     * Extrai valor decimal de campos como:
     *   "actual_price":64.99  ou  "actual_price":"64.99"
     */
    private BigDecimal extractDecimalField(String html, String fieldName) {
        Pattern p = Pattern.compile(
                "\"" + fieldName + "\"\\s*:\\s*\"?([0-9]+[.,][0-9]{1,2})\"?"
        );
        Matcher m = p.matcher(html);
        if (m.find()) {
            return parsePrice(m.group(1));
        }
        return null;
    }

    private String extractMetaOrTitle(String html) {
        Pattern og = Pattern.compile("<meta[^>]*property=[\"']og:title[\"'][^>]*content=[\"']([^\"']+)[\"']");
        Matcher m = og.matcher(html);
        if (m.find()) return m.group(1);

        Pattern title = Pattern.compile("<title[^>]*>([^<]+)</title>");
        Matcher t = title.matcher(html);
        if (t.find()) return t.group(1).trim();

        return null;
    }

    // ─── Estratégia 2: JSON-LD ───────────────────────────────────────────────

    private MlItemSnapshot tryJsonLd(Document doc) {
        for (Element el : doc.select("script[type=application/ld+json]")) {
            String json = el.data();
            if (json == null || json.isBlank()) continue;

            String title    = findFirst(json, "\"name\"\\s*:\\s*\"(.*?)\"");
            String priceStr = findFirst(json, "\"price\"\\s*:\\s*\"?([\\d][\\d\\.,]*)\"?");
            String currency = findFirst(json, "\"priceCurrency\"\\s*:\\s*\"(.*?)\"");

            BigDecimal price = parsePrice(priceStr);

            if (title != null || price != null) {
                return new MlItemSnapshot(title, price, currency != null ? currency : "BRL", "jsonld");
            }
        }
        return new MlItemSnapshot(null, null, null, "jsonld-miss");
    }

    // ─── Estratégia 3: Meta tags ─────────────────────────────────────────────

    private MlItemSnapshot tryMeta(Document doc) {
        String title    = attr(doc, "meta[property=og:title]", "content");
        if (title == null) title = doc.title();

        String priceStr = attr(doc, "meta[itemprop=price]", "content");
        String currency = attr(doc, "meta[itemprop=priceCurrency]", "content");

        BigDecimal price = parsePrice(priceStr);
        return new MlItemSnapshot(title, price, currency != null ? currency : "BRL", "meta");
    }

    // ─── Estratégia 4: HTML fallback ─────────────────────────────────────────

    private MlItemSnapshot tryHtmlFallback(Document doc) {
        String title = attr(doc, "meta[property=og:title]", "content");
        if (title == null) title = doc.title();

        String bodyText = doc.text();
        String priceStr = findFirst(bodyText,
                "(R\\$\\s*)?(\\d{1,3}(\\.\\d{3})*(,\\d{2})|\\d+(,\\d{2}))");
        BigDecimal price = parsePrice(priceStr);

        return new MlItemSnapshot(title, price, "BRL", "html-fallback");
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private static String attr(Document doc, String css, String attr) {
        Element el = doc.selectFirst(css);
        return el != null ? el.attr(attr) : null;
    }

    private static String findFirst(String input, String regex) {
        if (input == null) return null;
        Matcher m = Pattern.compile(regex, Pattern.DOTALL).matcher(input);
        return m.find() ? m.group(m.groupCount()) : null;
    }

    /**
     * Converte string de preço normalizada para BigDecimal.
     * Usado nas estratégias 2, 3 e 4 — a estratégia 1 já monta
     * o valor corretamente via amount+cents.
     */
    static BigDecimal parsePrice(String raw) {
        if (raw == null || raw.isBlank()) return null;

        String s = raw.trim()
                .replace("R$", "")
                .trim()
                .replaceAll("[^0-9,\\.]", "");

        if (s.isEmpty()) return null;

        boolean looksLikeInteger = false;

        // Detecta inteiro disfarçado: "5924.00" ou "5924.0"
        if (!s.contains(",") && s.matches("\\d+\\.0+")) {
            s = s.replaceAll("\\.0+$", "");
            looksLikeInteger = true;
        } else if (!s.contains(",") && !s.contains(".")) {
            looksLikeInteger = true;
        }

        if (!looksLikeInteger) {
            if (s.contains(",") && s.contains(".")) {
                s = s.replace(".", "").replace(",", ".");
            } else if (s.contains(",")) {
                s = s.replace(",", ".");
            } else if (s.contains(".")) {
                String[] parts = s.split("\\.");
                if (parts.length == 2 && parts[1].length() == 3) {
                    s = s.replace(".", "");
                    looksLikeInteger = true;
                }
            }
        }

        BigDecimal value;
        try {
            value = new BigDecimal(s);
        } catch (Exception e) {
            return null;
        }

        // Heurística anti-centavos (só para inteiros sem decimal real)
        if (looksLikeInteger) {
            long intVal  = value.longValue();
            int  nDigits = String.valueOf(intVal).length();
            long cents   = intVal % 100;

            if (nDigits == 3 && intVal >= 100) {
                value = value.divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            } else if (nDigits == 4 && cents != 0) {
                value = value.divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            } else if (nDigits >= 5 && intVal <= 9_999_999) {
                value = value.divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            }
        }

        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
