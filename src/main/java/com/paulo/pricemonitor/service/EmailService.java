package com.paulo.pricemonitor.service;

public interface EmailService {
    /**
     * Envia notificação de queda de preço.
     * Chamado apenas quando newPrice < oldPrice.
     */
    void sendPriceDropEmail(String to, String productTitle, String productUrl,
                            String oldPrice, String newPrice);
}
