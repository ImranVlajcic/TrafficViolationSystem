package com.academy.trafficviolationsystem.core.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Registers a Caffeine-backed CacheManager for the entire application.
 *
 * Caches registered:
 *   "system-config" — SystemConfigService typed getters.
 *                     TTL 5 minutes: config changes propagate within 5 minutes
 *                     even without a @CacheEvict (e.g. direct DB edits during
 *                     maintenance). @CacheEvict in SystemConfigService.beforeUpdate()
 *                     flushes immediately on any HTTP update.
 *
 *   "fine-rules"    — FineRuleService.findActiveByType() cache.
 *                     TTL 30 minutes: fine rules change very rarely.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager();

        manager.registerCustomCache(
                "system-config",
                new CaffeineCache(
                        "system-config",
                        Caffeine.newBuilder()
                                .expireAfterWrite(5, TimeUnit.MINUTES)
                                .maximumSize(500)
                                .build()
                ).getNativeCache()
        );

        manager.registerCustomCache(
                "fine-rules",
                new CaffeineCache(
                        "fine-rules",
                        Caffeine.newBuilder()
                                .expireAfterWrite(30, TimeUnit.MINUTES)
                                .maximumSize(500)
                                .build()
                ).getNativeCache()
        );

        return manager;
    }
}
