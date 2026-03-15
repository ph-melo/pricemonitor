package com.paulo.pricemonitor.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@Profile("smtp")
@RequiredArgsConstructor
public class SmtpEmailService implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String from;

    @Override
    public void sendPriceDropEmail(String to, String productTitle, String productUrl,
                                   String oldPrice, String newPrice) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(from);
        msg.setTo(to);
        msg.setSubject("📉 Preço caiu: " + productTitle);
        msg.setText(
                "Boa notícia! O preço do produto que você monitora caiu.\n\n" +
                        "Produto: " + productTitle + "\n" +
                        "Preço anterior: R$ " + oldPrice + "\n" +
                        "Preço atual:    R$ " + newPrice + "\n\n" +
                        "Ver produto: " + productUrl + "\n\n" +
                        "— PriceMonitor"
        );
        mailSender.send(msg);
    }
}
