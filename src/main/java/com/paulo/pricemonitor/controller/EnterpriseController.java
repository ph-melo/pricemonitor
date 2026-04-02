package com.paulo.pricemonitor.controller;

import com.paulo.pricemonitor.dto.CreateEnterpriseProductRequest;
import com.paulo.pricemonitor.dto.EnterpriseProductResponse;
import com.paulo.pricemonitor.dto.MlListingResponse;
import com.paulo.pricemonitor.entity.MlListing;
import com.paulo.pricemonitor.security.AuthenticatedUser;
import com.paulo.pricemonitor.service.EnterpriseMonitorService;
import com.paulo.pricemonitor.service.EnterpriseProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/enterprise")
@RequiredArgsConstructor
public class EnterpriseController {

    private final EnterpriseProductService enterpriseProductService;
    private final EnterpriseMonitorService enterpriseMonitorService;

    // ─── Produtos ─────────────────────────────────────────────────────────────

    @PostMapping("/products")
    @ResponseStatus(HttpStatus.CREATED)
    public EnterpriseProductResponse create(
            @RequestBody CreateEnterpriseProductRequest request,
            @AuthenticationPrincipal AuthenticatedUser auth) {
        return enterpriseProductService.create(
                new CreateEnterpriseProductRequest(
                        auth.userId(),
                        request.ean(),
                        request.productName(),
                        request.marca(),
                        request.mapPrice(),
                        request.tolerancePercent()
                ));
    }

    @GetMapping("/products")
    public List<EnterpriseProductResponse> list(
            @AuthenticationPrincipal AuthenticatedUser auth) {
        return enterpriseProductService.listByUser(auth.userId());
    }

    @DeleteMapping("/products/{productId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable Long productId,
            @AuthenticationPrincipal AuthenticatedUser auth) {
        enterpriseProductService.delete(auth.userId(), productId);
    }

    // ─── Verificação manual ───────────────────────────────────────────────────

    @PostMapping("/products/{productId}/check")
    public Map<String, Object> checkNow(
            @PathVariable Long productId,
            @AuthenticationPrincipal AuthenticatedUser auth) {
        List<MlListing> listings =
                enterpriseMonitorService.checkProductById(auth.userId(), productId);
        long violations = listings.stream().filter(MlListing::isViolation).count();
        return Map.of(
                "totalListings", listings.size(),
                "violations", violations,
                "message", listings.isEmpty()
                        ? "Nenhum anúncio encontrado para este EAN"
                        : listings.size() + " anúncio(s) encontrado(s), " + violations + " violação(ões)"
        );
    }

    // ─── Anúncios ─────────────────────────────────────────────────────────────

    @GetMapping("/products/{productId}/listings")
    public List<MlListingResponse> getListingsByProduct(
            @PathVariable Long productId,
            @AuthenticationPrincipal AuthenticatedUser auth) {
        return enterpriseProductService.getListingsByProduct(auth.userId(), productId);
    }

    @GetMapping("/listings")
    public List<MlListingResponse> getAllListings(
            @AuthenticationPrincipal AuthenticatedUser auth) {
        return enterpriseProductService.getAllListings(auth.userId());
    }

    // ─── Violações ────────────────────────────────────────────────────────────

    @GetMapping("/violations")
    public List<MlListingResponse> getViolations(
            @AuthenticationPrincipal AuthenticatedUser auth) {
        return enterpriseProductService.getViolations(auth.userId());
    }

    @PostMapping("/violations/mark-seen")
    public void markAllAsSeen(
            @AuthenticationPrincipal AuthenticatedUser auth) {
        enterpriseProductService.markAllAsSeen(auth.userId());
    }
}
