package com.paulo.pricemonitor.service;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * Implementação dummy usada em desenvolvimento (profile != smtp).
 * Imprime no console sem enviar e-mail de verdade.
 */
@Service
@Profile("!smtp")
public class NoopEmailService implements EmailService {

    @Override
    public void sendPriceDropEmail(String to, String productTitle, String productUrl,
                                   String oldPrice, String newPrice) {
        System.out.printf("[NOOP EMAIL] to=%s | %s | R$%s -> R$%s | %s%n",
                to, productTitle, oldPrice, newPrice, productUrl);
    }
}
