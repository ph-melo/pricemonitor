package com.paulo.pricemonitor.marketplace.mercadolivre.scraper;

import com.paulo.pricemonitor.marketplace.MarketplaceTemporarilyBlockedException;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class MlHtmlFetcher {

    private final RestClient restClient;

    public MlHtmlFetcher(RestClient.Builder builder) {
        this.restClient = builder
                .defaultHeader(HttpHeaders.USER_AGENT, userAgent())
                .defaultHeader(HttpHeaders.ACCEPT_LANGUAGE, "pt-BR,pt;q=0.9,en;q=0.8")
                .defaultHeader(HttpHeaders.ACCEPT, "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .defaultHeader(HttpHeaders.REFERER, "https://www.mercadolivre.com.br/")
                .defaultHeader(HttpHeaders.CONNECTION, "keep-alive")
                .defaultHeader(HttpHeaders.CACHE_CONTROL, "no-cache")
                .build();
    }

    @RateLimiter(name = "mlScrape")
    @Retry(name = "mlScrape")
    @Cacheable(cacheNames = "ml_html", key = "#url")
    public String getHtml(String url) {

        // Resolve redirecionamentos (links curtos/afiliados) e normaliza
        String normalizedUrl = normalizeUrl(resolveUrl(url));

        var resp = restClient.get()
                .uri(normalizedUrl)
                .accept(MediaType.TEXT_HTML)
                .retrieve()
                .toEntity(String.class);

        int status = resp.getStatusCode().value();
        String body = resp.getBody();

        if (status == 429) {
            throw new MarketplaceTemporarilyBlockedException("Muitas requisições (429). Tente novamente mais tarde.");
        }
        if (status == 403) {
            throw new MarketplaceTemporarilyBlockedException("Acesso negado (403). Temporariamente bloqueado.");
        }
        if (status >= 400) {
            throw new IllegalStateException("Falha ao baixar HTML (status=" + status + ")");
        }

        if (body == null || body.length() < 800) {
            throw new MarketplaceTemporarilyBlockedException("HTML curto/inesperado (possível página intermediária).");
        }

        String low = body.toLowerCase();
        boolean looksLikeChallenge =
                low.contains("captcha") ||
                        low.contains("recaptcha") ||
                        low.contains("are you a robot") ||
                        (low.contains("robot") && low.contains("verify"));

        if (looksLikeChallenge) {
            throw new MarketplaceTemporarilyBlockedException("Detectado challenge/anti-bot na resposta.");
        }

        // Se caiu em página social de afiliado (/social/), extrai o link do produto
        if (normalizedUrl.contains("/social/")) {
            String productUrl = extractProductUrlFromSocialPage(body);
            if (productUrl != null) {
                // Busca o HTML da página real do produto
                return getHtml(productUrl);
            }
            throw new IllegalArgumentException(
                    "Link de afiliado não suportado. Clique em 'Ir para produto' e copie a URL do produto diretamente.");
        }

        return body;
    }

    /**
     * Extrai a URL do produto a partir de uma página social de afiliado (/social/).
     * Procura pelo botão "Ir para produto" e retorna o href.
     */
    static String extractProductUrlFromSocialPage(String html) {
        if (html == null) return null;
        try {
            Document doc = Jsoup.parse(html);

            // Tenta achar o botão "Ir para produto" ou "Ver produto"
            for (Element a : doc.select("a[href]")) {
                String href = a.attr("abs:href");
                String text = a.text().toLowerCase();
                if ((text.contains("ir para produto") || text.contains("ver produto") || text.contains("comprar"))
                        && (href.contains("mercadolivre.com.br") || href.contains("mercadolibre.com"))) {
                    return href;
                }
            }

            // Fallback: qualquer link do ML que pareça produto (contém MLB no path)
            for (Element a : doc.select("a[href*='mercadolivre.com.br']")) {
                String href = a.attr("abs:href");
                if (href.matches(".*/(MLB|p/MLB).*")) {
                    return href;
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    /**
     * Resolve redirecionamentos de links curtos e afiliados (ex: meli.la/xxx)
     * seguindo todos os redirects em loop até chegar na URL final do ML.
     */
    static String resolveUrl(String url) {
        if (url == null || url.isBlank()) return url;
        if (url.contains("mercadolivre.com.br") || url.contains("mercadolibre.com")) {
            return url;
        }

        String current = url;
        int maxHops = 10;

        for (int i = 0; i < maxHops; i++) {
            try {
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection)
                        new java.net.URL(current).openConnection();
                conn.setInstanceFollowRedirects(false);
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                conn.setRequestProperty("User-Agent", userAgent());
                conn.setRequestProperty("Accept", "text/html,application/xhtml+xml");
                conn.connect();

                int status = conn.getResponseCode();
                String location = conn.getHeaderField("Location");
                conn.disconnect();

                boolean isRedirect = status == 301 || status == 302
                        || status == 303 || status == 307 || status == 308;

                if (isRedirect && location != null && !location.isBlank()) {
                    if (location.startsWith("/")) {
                        java.net.URL base = new java.net.URL(current);
                        location = base.getProtocol() + "://" + base.getHost() + location;
                    }
                    current = location;
                    if (current.contains("mercadolivre.com.br") || current.contains("mercadolibre.com")) {
                        return current;
                    }
                } else {
                    break;
                }
            } catch (Exception e) {
                break;
            }
        }

        return current;
    }

    /**
     * Normaliza URLs do Mercado Livre antes do request:
     *
     * 1. produto.mercadolivre.com.br -> www.mercadolivre.com.br
     * 2. Remove fragmento (#...)
     * 3. Remove query params de rastreamento de anuncio
     * 4. Reconstroi URL limpa se path terminar com sufixo _XX (ex: _JM)
     */
    static String normalizeUrl(String url) {
        if (url == null || url.isBlank()) return url;

        String normalized = url.replaceFirst(
                "(?i)https?://produto\\.mercadolivre\\.com\\.br",
                "https://www.mercadolivre.com.br"
        );

        int hashIdx = normalized.indexOf('#');
        if (hashIdx != -1) {
            normalized = normalized.substring(0, hashIdx);
        }

        if (normalized.contains("?")) {
            String[] parts = normalized.split("\\?", 2);
            String base = parts[0];
            String[] params = parts[1].split("&");
            java.util.List<String> adParams = java.util.List.of(
                    "is_advertising", "ad_domain", "ad_position", "ad_click_id",
                    "reco_backend", "reco_client", "polycard_client", "reco_id",
                    "reco_model", "reco_item_pos"
            );
            java.util.StringJoiner clean = new java.util.StringJoiner("&");
            for (String p : params) {
                boolean isAd = adParams.stream().anyMatch(p::startsWith);
                if (!isAd) clean.add(p);
            }
            normalized = clean.length() > 0 ? base + "?" + clean : base;
        }

        if (normalized.matches(".*_[A-Z]{2}$")) {
            java.util.regex.Matcher m = java.util.regex.Pattern
                    .compile("/(MLB-?(\\d+))")
                    .matcher(normalized);
            if (m.find()) {
                String mlbId = "MLB" + m.group(2);
                normalized = "https://www.mercadolivre.com.br/p/" + mlbId;
            }
        }

        return normalized;
    }

    private static String userAgent() {
        return "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0 Safari/537.36";
    }
}
