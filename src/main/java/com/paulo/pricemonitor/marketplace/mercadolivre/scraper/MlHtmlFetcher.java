package com.paulo.pricemonitor.marketplace.mercadolivre.scraper;

import com.paulo.pricemonitor.marketplace.MarketplaceTemporarilyBlockedException;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
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

        return body;
    }

    /**
     * Resolve redirecionamentos de links curtos e afiliados (ex: meli.la/xxx)
     * seguindo os redirects HTTP até chegar na URL final do Mercado Livre.
     */
    static String resolveUrl(String url) {
        if (url == null || url.isBlank()) return url;
        // Só resolve se não for já uma URL do ML
        if (url.contains("mercadolivre.com.br") || url.contains("mercadolibre.com")) {
            return url;
        }
        try {
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection)
                    new java.net.URL(url).openConnection();
            conn.setInstanceFollowRedirects(false);
            conn.setRequestMethod("HEAD");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setRequestProperty("User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
            conn.connect();

            int status = conn.getResponseCode();
            String location = conn.getHeaderField("Location");
            conn.disconnect();

            if ((status == 301 || status == 302 || status == 303 || status == 307 || status == 308)
                    && location != null && !location.isBlank()) {
                // Segue mais um nível se necessário (links com double redirect)
                return resolveUrl(location);
            }
        } catch (Exception e) {
            // Se falhar na resolução, tenta com a URL original
        }
        return url;
    }

    /**
     * Normaliza URLs do Mercado Livre antes do request:
     *
     * 1. produto.mercadolivre.com.br → www.mercadolivre.com.br
     *    O subdomínio "produto." é usado em links de anúncios patrocinados
     *    e o ML aplica anti-bot muito mais agressivo nele.
     *
     * 2. Remove fragmento (#...) inteiramente — parâmetros como
     *    polycard_client, is_advertising, ad_domain identificam bots.
     *
     * 3. Remove sufixo _JM (e similares) do path — ex: /MLB-123-titulo_JM
     *    Esse sufixo é adicionado em links de anúncios e causa 404 quando
     *    acessado sem os parâmetros de rastreamento originais.
     *    Nesse caso reconstrói a URL limpa: /p/MLB123
     */
    static String normalizeUrl(String url) {
        if (url == null || url.isBlank()) return url;

        // 1. Troca subdomínio produto. por www.
        String normalized = url.replaceFirst(
                "(?i)https?://produto\\.mercadolivre\\.com\\.br",
                "https://www.mercadolivre.com.br"
        );

        // 2. Remove fragmento inteiro
        int hashIdx = normalized.indexOf('#');
        if (hashIdx != -1) {
            normalized = normalized.substring(0, hashIdx);
        }

        // 3. Remove query params de rastreamento de anúncio
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

        // 4. Reconstrói URL limpa se o path terminar com sufixo _XX (ex: _JM)
        //    que é adicionado em links de anúncios e causa 404 sem os params
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
