package com.paulo.pricemonitor.service;

import com.paulo.pricemonitor.dto.CreateProductRequest;
import com.paulo.pricemonitor.dto.ProductResponse;
import com.paulo.pricemonitor.entity.PriceHistory;
import com.paulo.pricemonitor.entity.Product;
import com.paulo.pricemonitor.entity.User;
import com.paulo.pricemonitor.marketplace.MarketItemProvider;
import com.paulo.pricemonitor.monitor.MonitorStatus;
import com.paulo.pricemonitor.repository.PriceHistoryRepository;
import com.paulo.pricemonitor.repository.ProductRepository;
import com.paulo.pricemonitor.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private static final String MARKETPLACE_ML = "MERCADO_LIVRE";

    private final ProductRepository      productRepository;
    private final UserRepository         userRepository;
    private final PriceHistoryRepository priceHistoryRepository;
    private final MarketItemProvider     marketItemProvider;

    @Transactional
    public ProductResponse createProduct(CreateProductRequest request) {

        if (request == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request body é obrigatório");

        if (request.getUserId() == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "userId é obrigatório");

        if (request.getProductUrl() == null || request.getProductUrl().isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "productUrl é obrigatório");

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Usuário não encontrado: " + request.getUserId()));

        // ── Verifica limite do plano ──────────────────────────────────────
        int maxProducts = user.getPlan().getMaxProducts();
        long currentCount = productRepository.countByUserIdAndActiveTrue(user.getId());

        if (currentCount >= maxProducts) {
            throw new ResponseStatusException(
                    HttpStatus.PAYMENT_REQUIRED,
                    "Limite de monitoramento atingido. Seu plano " + user.getPlan().name() +
                            " permite até " + maxProducts + " produto(s). " +
                            "Remova um produto monitorado para adicionar outro."
            );
        }

        String url = request.getProductUrl().trim();

        // ── Valida que é uma URL do Mercado Livre ─────────────────────────
        if (!isMercadoLivreUrl(url)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "URL inválida. Insira um link do Mercado Livre (mercadolivre.com.br ou mercadolibre.com)");
        }

        // ── Evita duplicata pela URL exata ────────────────────────────────
        if (productRepository.existsByUserIdAndProductUrl(user.getId(), url)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Este produto já está sendo monitorado");
        }

        // ── Snapshot imediato via scraping ────────────────────────────────
        var snapshot = marketItemProvider.fetch(url, null, null);

        Product product = Product.builder()
                .productUrl(url)
                .marketplace(MARKETPLACE_ML)
                .externalId(url)
                .catalogId(null)
                .itemId(null)
                .title(safeTrim(snapshot.title()))
                .lastPrice(snapshot.price())
                .currency(snapshot.currency())
                .lastCheckAt(LocalDateTime.now())
                .lastStatus(MonitorStatus.INITIAL)
                .lastError(null)
                .user(user)
                .active(true)
                .build();

        Product saved = productRepository.save(product);

        priceHistoryRepository.save(new PriceHistory(
                saved,
                snapshot.price(),
                snapshot.currency(),
                java.time.Instant.now(),
                "CREATE"
        ));

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> listByUser(Long userId) {
        if (userId == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "userId é obrigatório");

        return productRepository.findByUserIdAndActiveTrue(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public void delete(Long userId, Long productId) {
        productRepository.findByUserIdAndId(userId, productId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Produto não encontrado"));

        productRepository.deleteHistoryByProductId(productId);
        productRepository.deleteById(productId);
    }

    public ProductResponse getProductResponse(Product p) {
        return toResponse(p);
    }

    // ─── Mapper ───────────────────────────────────────────────────────────────

    private ProductResponse toResponse(Product p) {
        return ProductResponse.builder()
                .id(p.getId())
                .externalId(p.getExternalId())
                .marketplace(p.getMarketplace())
                .title(p.getTitle())
                .productUrl(p.getProductUrl())
                .catalogId(p.getCatalogId())
                .itemId(p.getItemId())
                .lastPrice(p.getLastPrice())
                .currency(p.getCurrency())
                .lastCheckAt(p.getLastCheckAt())
                .trackingType("SCRAPER")
                .lastStatus(p.getLastStatus() != null ? p.getLastStatus().name() : null)
                .lastError(p.getLastError())
                .build();
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private boolean isMercadoLivreUrl(String url) {
        if (url == null) return false;
        String lower = url.toLowerCase();
        return lower.contains("mercadolivre.com.br")
                || lower.contains("mercadolibre.com")
                || lower.contains("mlstatic.com");
    }

    private String safeTrim(String v) {
        if (v == null) return null;
        String t = v.trim();
        return t.isEmpty() ? null : t;
    }
}
