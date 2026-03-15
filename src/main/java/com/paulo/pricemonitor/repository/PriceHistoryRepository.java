package com.paulo.pricemonitor.repository;

import com.paulo.pricemonitor.entity.PriceHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PriceHistoryRepository extends JpaRepository<PriceHistory, Long> {
    Optional<PriceHistory> findTopByProductIdOrderByCollectedAtDesc(Long productId);
}