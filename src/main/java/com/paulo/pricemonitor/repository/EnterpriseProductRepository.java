package com.paulo.pricemonitor.repository;

import com.paulo.pricemonitor.entity.EnterpriseProduct;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EnterpriseProductRepository extends JpaRepository<EnterpriseProduct, Long> {

    List<EnterpriseProduct> findByUserIdAndActiveTrue(Long userId);

    List<EnterpriseProduct> findByActiveTrue();

    Optional<EnterpriseProduct> findByIdAndUserId(Long id, Long userId);

    boolean existsByUserIdAndEan(Long userId, String ean);
}
