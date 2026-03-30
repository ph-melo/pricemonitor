package com.paulo.pricemonitor.controller;

import com.paulo.pricemonitor.dto.CreateEnterpriseProductRequest;
import com.paulo.pricemonitor.dto.EnterpriseProductResponse;
import com.paulo.pricemonitor.dto.PriceViolationResponse;
import com.paulo.pricemonitor.entity.PriceViolation;
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
        List<PriceViolation> violations =
                enterpriseMonitorService.checkProductById(auth.userId(), productId);
        return Map.of(
                "violationsFound", violations.size(),
                "message", violations.isEmpty()
                        ? "Nenhuma violação de MAP detectada"
                        : violations.size() + " violação(ões) detectada(s)"
        );
    }

    // ─── Alertas / Violações ──────────────────────────────────────────────────

    @GetMapping("/violations")
    public List<PriceViolationResponse> getViolations(
            @AuthenticationPrincipal AuthenticatedUser auth) {
        return enterpriseProductService.getViolations(auth.userId());
    }

    @GetMapping("/violations/unseen")
    public List<PriceViolationResponse> getUnseenViolations(
            @AuthenticationPrincipal AuthenticatedUser auth) {
        return enterpriseProductService.getUnseenViolations(auth.userId());
    }

    @GetMapping("/violations/unseen/count")
    public Map<String, Long> countUnseen(
            @AuthenticationPrincipal AuthenticatedUser auth) {
        return Map.of("count",
                enterpriseProductService.getUnseenViolations(auth.userId()).size());
    }

    @PostMapping("/violations/mark-seen")
    public void markAllAsSeen(
            @AuthenticationPrincipal AuthenticatedUser auth) {
        enterpriseProductService.markAllViolationsAsSeen(auth.userId());
    }
}
