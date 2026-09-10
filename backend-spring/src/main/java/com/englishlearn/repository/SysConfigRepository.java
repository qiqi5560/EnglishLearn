package com.englishlearn.repository;

import com.englishlearn.entity.SysConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SysConfigRepository extends JpaRepository<SysConfig, Integer> {

    Optional<SysConfig> findByKey(String key);
}