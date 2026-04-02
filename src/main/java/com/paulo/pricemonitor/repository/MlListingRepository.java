package com.paulo.pricemonitor.repository;

import com.paulo.pricemonitor.entity.MlListing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface MlListingRepository extends JpaRepository<MlListing, Long> {

    List<MlListing> findByEnterpriseProductId(Long enterpriseProductId);

    @Query("SELECT l FROM MlListing l WHERE l.enterpriseProduct.user.id = :userId ORDER BY l.detectedAt DESC")
    List<MlListing> findByUserId(Long userId);

    @Query("SELECT l FROM MlListing l WHERE l.enterpriseProduct.user.id = :userId AND l.violation = true ORDER BY l.detectedAt DESC")
    List<MlListing> findViolationsByUserId(Long userId);

    @Query("SELECT l FROM MlListing l WHERE l.enterpriseProduct.id = :productId ORDER BY l.listedPrice ASC")
    List<MlListing> findByProductId(Long productId);

    @Query("SELECT COUNT(l) FROM MlListing l WHERE l.enterpriseProduct.id = :productId")
    long countByProductId(Long productId);

    @Query("SELECT COUNT(l) FROM MlListing l WHERE l.enterpriseProduct.id = :productId AND l.violation = true")
    long countViolationsByProductId(Long productId);

    @Query("SELECT COUNT(l) FROM MlListing l WHERE l.enterpriseProduct.user.id = :userId AND l.violation = true AND l.seen = false")
    long countUnseenViolationsByUserId(Long userId);

    @Modifying
    @Transactional
    @Query("UPDATE MlListing l SET l.seen = true WHERE l.enterpriseProduct.user.id = :userId")
    void markAllAsSeenByUserId(Long userId);

    @Modifying
    @Transactional
    @Query("DELETE FROM MlListing l WHERE l.enterpriseProduct.id = :productId")
    void deleteByEnterpriseProductId(Long productId);
}
