package com.paulo.pricemonitor.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "price_history",
        indexes = {
                @Index(name = "idx_price_history_product_id", columnList = "product_id"),
                @Index(name = "idx_price_history_collected_at", columnList = "collectedAt")
        }
)
public class PriceHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal price;

    @Column(nullable = false, length = 8)
    private String currency;

    @Column(nullable = false)
    private Instant collectedAt;

    @Column(length = 32)
    private String strategy;

    protected PriceHistory() {}

    public PriceHistory(Product product, BigDecimal price, String currency, Instant collectedAt, String strategy) {
        this.product = product;
        this.price = price;
        this.currency = currency;
        this.collectedAt = collectedAt;
        this.strategy = strategy;
    }

    public Long getId() { return id; }
    public Product getProduct() { return product; }
    public BigDecimal getPrice() { return price; }
    public String getCurrency() { return currency; }
    public Instant getCollectedAt() { return collectedAt; }
    public String getStrategy() { return strategy; }
}