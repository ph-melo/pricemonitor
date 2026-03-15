package com.paulo.pricemonitor.controller;

import com.paulo.pricemonitor.dto.CreateProductRequest;
import com.paulo.pricemonitor.dto.ProductResponse;
import com.paulo.pricemonitor.security.AuthenticatedUser;
import com.paulo.pricemonitor.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponse create(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestBody CreateProductRequest request
    ) {
        // userId sempre vem do token JWT — não do body
        request.setUserId(user.userId());
        return productService.createProduct(request);
    }

    @GetMapping
    public List<ProductResponse> list(@AuthenticationPrincipal AuthenticatedUser user) {
        return productService.listByUser(user.userId());
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long productId
    ) {
        productService.delete(user.userId(), productId);
        return ResponseEntity.noContent().build();
    }
}
