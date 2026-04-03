package com.paulo.pricemonitor.repository;

import com.paulo.pricemonitor.entity.AppConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AppConfigRepository extends JpaRepository<AppConfig, String> {
    Optional<AppConfig> findByKey(String key);
}