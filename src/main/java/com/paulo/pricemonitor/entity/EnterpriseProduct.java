package com.paulo.pricemonitor.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "enterprise_products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EnterpriseProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Código EAN/GTIN do produto
    @Column(nullable = false, length = 64)
    private String ean;

    // Nome do produto (preenchido pelo cliente)
    @Column(nullable = false, length = 500)
    private String productName;

    // Preço mínimo anunciado (MAP - Minimum Advertised Price)
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal mapPrice;

    // Percentual de tolerância — ex: 5.0 = alertar só se estiver 5% abaixo do MAP
    @Column(nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal tolerancePercent = BigDecimal.ZERO;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "last_check_at")
    private LocalDateTime lastCheckAt;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.active = true;
    }
}
