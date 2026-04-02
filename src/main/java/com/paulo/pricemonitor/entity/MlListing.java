package com.paulo.pricemonitor.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "ml_listings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MlListing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "enterprise_product_id", nullable = false)
    private EnterpriseProduct enterpriseProduct;

    @Column(name = "ml_item_id", nullable = false, length = 64)
    private String mlItemId;

    @Column(name = "listing_url", length = 1000)
    private String listingUrl;

    @Column(name = "seller_name", length = 255)
    private String sellerName;

    @Column(name = "seller_id")
    private Long sellerId;

    @Column(name = "listing_title", length = 500)
    private String listingTitle;

    @Column(name = "listed_price", nullable = false, precision = 19, scale = 2)
    private BigDecimal listedPrice;

    @Column(name = "map_price", nullable = false, precision = 19, scale = 2)
    private BigDecimal mapPrice;

    // null = não viola, > 0 = percentual de violação
    @Column(name = "percent_below", precision = 5, scale = 2)
    private BigDecimal percentBelow;

    @Column(name = "is_violation", nullable = false)
    @Builder.Default
    private boolean violation = false;

    @Column(name = "seen", nullable = false)
    @Builder.Default
    private boolean seen = false;

    @Column(name = "detected_at", nullable = false)
    private LocalDateTime detectedAt;

    @PrePersist
    public void prePersist() {
        this.detectedAt = LocalDateTime.now();
    }
}
