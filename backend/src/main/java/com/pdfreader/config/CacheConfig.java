package com.pdfreader.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        // In-memory cache — no Redis or extra dependencies needed
        return new ConcurrentMapCacheManager("pdfText", "pdfList", "pdfMetadata");
    }
}
