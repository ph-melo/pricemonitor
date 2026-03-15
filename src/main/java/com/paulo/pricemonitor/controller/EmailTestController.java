package com.paulo.pricemonitor.controller;

import com.paulo.pricemonitor.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/test")
@RequiredArgsConstructor
public class EmailTestController {

    private final EmailService emailService;

    @PostMapping("/email")
    public ResponseEntity<String> testEmail(@RequestParam String to) {
        emailService.sendPriceDropEmail(
                to,
                "Produto Teste",
                "https://www.mercadolivre.com.br/",
                "100.00",
                "90.00"
        );
        return ResponseEntity.ok("Email disparado (verifique inbox/spam).");
    }
}
