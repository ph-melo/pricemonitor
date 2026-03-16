package com.paulo.pricemonitor.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Compatibilidade: por enquanto vamos manter.
     * Regra:
     * - Se tiver itemId => externalId = itemId
     * - Senão => externalId = catalogId
     */
    @Column(name = "external_id", nullable = false, length = 1000)
    private String externalId;

    @Column(nullable = false)
    private String marketplace;

    @Column(length = 500)
    private String title;

    @Column(name = "product_url", length = 1000, nullable = false)
    private String productUrl;

    // /p/MLB...
    @Column(name = "catalog_id")
    private String catalogId;

    // wid=MLB... ou item_id=MLB...
    @Column(name = "item_id")
    private String itemId;

    @Column(name = "last_price", precision = 19, scale = 2)
    private java.math.BigDecimal lastPrice;

    @Column(name = "currency", length = 8)
    private String currency;

    @Column(name = "last_check_at")
    private LocalDateTime lastCheckAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "last_status", length = 32)
    private com.paulo.pricemonitor.monitor.MonitorStatus lastStatus;

    @Column(name = "last_error", length = 1000)
    private String lastError;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.active = true;
        if (this.lastStatus == null) {
            this.lastStatus = com.paulo.pricemonitor.monitor.MonitorStatus.NO_CHANGE;
        }
    }
}
