package com.paulo.pricemonitor.repository;

import com.paulo.pricemonitor.entity.PriceHistory;
import com.paulo.pricemonitor.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByUserIdAndActiveTrue(Long userId);

    List<Product> findByUserId(Long userId);

    Optional<Product> findByUserIdAndId(Long userId, Long productId);

    List<Product> findByActiveTrue();

    boolean existsByUserIdAndProductUrl(Long userId, String productUrl);

    boolean existsByUserIdAndItemId(Long userId, String itemId);
    boolean existsByUserIdAndCatalogId(Long userId, String catalogId);

    // Conta quantos produtos ativos o usuário tem
    long countByUserIdAndActiveTrue(Long userId);

    @Modifying
    @Query("DELETE FROM PriceHistory ph WHERE ph.product.id = :productId")
    void deleteHistoryByProductId(@Param("productId") Long productId);
}
