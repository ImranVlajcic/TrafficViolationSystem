package com.academy.trafficviolationsystem.configuration;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SystemConfigRepository extends JpaRepository<SystemConfigEntity, Integer> {

    Optional<SystemConfigEntity> findByConfigKey(String configKey);

    List<SystemConfigEntity> findByCategoryOrderByConfigKeyAsc(String category);
}
