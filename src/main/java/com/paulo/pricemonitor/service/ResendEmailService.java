package com.paulo.pricemonitor.service;

import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ResendEmailService implements EmailService {

    private final Resend resend;
    private final String fromEmail;

    public ResendEmailService(
            @Value("${app.resend.api-key}") String apiKey,
            @Value("${app.mail.from}") String fromEmail) {
        this.resend = new Resend(apiKey);
        this.fromEmail = fromEmail;
    }

    @Override
    public void sendPriceDropEmail(String to, String productTitle,
                                   String productUrl, String oldPrice, String newPrice) {
        String html = """
                <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
                    <h2 style="color: #2d6a4f;">🎉 Queda de preço detectada!</h2>
                    <p>O produto que você está monitorando teve uma redução de preço:</p>
                    <div style="background: #f0f7f4; padding: 16px; border-radius: 8px; margin: 16px 0;">
                        <h3 style="margin: 0 0 8px 0;">%s</h3>
                        <p style="margin: 4px 0;">
                            <span style="text-decoration: line-through; color: #999;">De: R$ %s</span>
                        </p>
                        <p style="margin: 4px 0; font-size: 1.4em; color: #2d6a4f; font-weight: bold;">
                            Por: R$ %s
                        </p>
                    </div>
                    <a href="%s"
                       style="display: inline-block; background: #2d6a4f; color: white;
                              padding: 12px 24px; border-radius: 6px; text-decoration: none;
                              font-weight: bold; margin-top: 8px;">
                        Ver produto no Mercado Livre
                    </a>
                    <p style="color: #999; font-size: 0.85em; margin-top: 24px;">
                        Você recebeu este e-mail porque está monitorando este produto no PriceMonitor.
                    </p>
                </div>
                """.formatted(productTitle, oldPrice, newPrice, productUrl);

        CreateEmailOptions params = CreateEmailOptions.builder()
                .from(fromEmail)
                .to(to)
                .subject("💰 Preço caiu: " + productTitle)
                .html(html)
                .build();

        try {
            resend.emails().send(params);
        } catch (ResendException e) {
            throw new RuntimeException("Falha ao enviar e-mail via Resend: " + e.getMessage(), e);
        }
    }
}