package com.paulo.pricemonitor.controller;

import com.paulo.pricemonitor.dto.MonitorResultResponse;
import com.paulo.pricemonitor.dto.ProductResponse;
import com.paulo.pricemonitor.entity.Product;
import com.paulo.pricemonitor.security.AuthenticatedUser;
import com.paulo.pricemonitor.service.PriceMonitorService;
import com.paulo.pricemonitor.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/monitor")
@RequiredArgsConstructor
public class MonitorController {

    private final PriceMonitorService monitorService;
    private final ProductService productService;

    @PostMapping("/run-once")
    public ResponseEntity<?> runOnce() {
        monitorService.checkAllActiveProducts();
        return ResponseEntity.ok("OK");
    }

    @PostMapping("/check/{productId}")
    public ResponseEntity<MonitorResultResponse> checkOneNow(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long productId
    ) {
        try {
            Product updated = monitorService.checkOneByUser(user.userId(), productId);
            ProductResponse pr = productService.getProductResponse(updated);

            return ResponseEntity.ok(MonitorResultResponse.builder()
                    .status("OK")
                    .product(pr)
                    .build());

        } catch (IllegalStateException e) {
            return ResponseEntity.ok(MonitorResultResponse.builder()
                    .status("CAPTCHA")
                    .message(e.getMessage())
                    .build());

        } catch (Exception e) {
            return ResponseEntity.ok(MonitorResultResponse.builder()
                    .status("ERROR")
                    .message(e.getMessage())
                    .build());
        }
    }
}
