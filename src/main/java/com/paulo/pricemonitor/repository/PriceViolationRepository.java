package com.paulo.pricemonitor.repository;

import com.paulo.pricemonitor.entity.PriceViolation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface PriceViolationRepository extends JpaRepository<PriceViolation, Long> {

    // Todas as violações de um usuário (via produto Enterprise)
    @Query("SELECT v FROM PriceViolation v WHERE v.enterpriseProduct.user.id = :userId ORDER BY v.detectedAt DESC")
    List<PriceViolation> findByUserId(Long userId);

    // Violações não vistas de um usuário
    @Query("SELECT v FROM PriceViolation v WHERE v.enterpriseProduct.user.id = :userId AND v.seen = false ORDER BY v.detectedAt DESC")
    List<PriceViolation> findUnseenByUserId(Long userId);

    // Marca todas como vistas
    @Modifying
    @Transactional
    @Query("UPDATE PriceViolation v SET v.seen = true WHERE v.enterpriseProduct.user.id = :userId")
    void markAllAsSeenByUserId(Long userId);

    // Deleta violações de um produto
    @Modifying
    @Transactional
    @Query("DELETE FROM PriceViolation v WHERE v.enterpriseProduct.id = :productId")
    void deleteByEnterpriseProductId(Long productId);

    // Conta não vistas
    @Query("SELECT COUNT(v) FROM PriceViolation v WHERE v.enterpriseProduct.user.id = :userId AND v.seen = false")
    long countUnseenByUserId(Long userId);
}
