package cn.nuaa.jensonxu.fairy.common.repository.caffeine;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;


/*
 *  caffeine 本地缓存配置
 **/
@EnableCaching
@Configuration
public class CaffeineConfig {

    @Bean
    public CacheManager cacheManager() {
        List<CaffeineCache> caches = new ArrayList<>();
        SimpleCacheManager cacheManager = new SimpleCacheManager();

        caches.add(buildCaffeineCache("sseChunkCache",
                Caffeine.newBuilder()
                        .maximumSize(10000)
                        .recordStats()));

        cacheManager.setCaches(caches);
        cacheManager.initializeCaches();
        return cacheManager;
    }

    private CaffeineCache buildCaffeineCache(String name, Caffeine<Object, Object> caffeine) {
        return new CaffeineCache(name, caffeine.build());
    }
}
