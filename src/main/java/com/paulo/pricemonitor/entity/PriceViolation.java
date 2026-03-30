package com.paulo.pricemonitor.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "price_violations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PriceViolation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "enterprise_product_id", nullable = false)
    private EnterpriseProduct enterpriseProduct;

    // ID do anúncio no ML (ex: MLB123456789)
    @Column(name = "ml_item_id", nullable = false, length = 64)
    private String mlItemId;

    // Link do anúncio violador
    @Column(name = "listing_url", length = 1000)
    private String listingUrl;

    // Nome do vendedor
    @Column(name = "seller_name", length = 255)
    private String sellerName;

    // ID do vendedor no ML
    @Column(name = "seller_id")
    private Long sellerId;

    // Título do anúncio
    @Column(name = "listing_title", length = 500)
    private String listingTitle;

    // Preço encontrado no anúncio
    @Column(name = "listed_price", nullable = false, precision = 19, scale = 2)
    private BigDecimal listedPrice;

    // Preço MAP configurado no momento da violação
    @Column(name = "map_price", nullable = false, precision = 19, scale = 2)
    private BigDecimal mapPrice;

    // Percentual abaixo do MAP — ex: 12.5 = 12,5% abaixo
    @Column(name = "percent_below", nullable = false, precision = 5, scale = 2)
    private BigDecimal percentBelow;

    // Se o alerta já foi visualizado pelo usuário
    @Column(nullable = false)
    @Builder.Default
    private boolean seen = false;

    @Column(name = "detected_at", nullable = false)
    private LocalDateTime detectedAt;

    @PrePersist
    public void prePersist() {
        this.detectedAt = LocalDateTime.now();
        this.seen = false;
    }
}
