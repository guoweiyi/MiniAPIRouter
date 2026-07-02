package com.miniapi.router.standalone.spiimpl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.miniapi.router.core.spi.CacheService;
import com.miniapi.router.core.util.JsonUtils;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.function.Function;

@Component
public class CaffeineCacheService implements CacheService {

    private final Cache<String, String> cache;

    public CaffeineCacheService() {
        this.cache = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMinutes(10))
                .maximumSize(10000)
                .build();
    }

    @Override
    public <T> T get(String key, Class<T> type) {
        String json = cache.getIfPresent(key);
        if (json == null) return null;
        return JsonUtils.fromJson(json, type);
    }

    @Override
    public <T> T getOrLoad(String key, Class<T> type, Function<String, T> loader, Duration ttl) {
        T cached = get(key, type);
        if (cached != null) return cached;
        T value = loader.apply(key);
        if (value != null) put(key, value);
        return value;
    }

    @Override
    public void put(String key, Object value, Duration ttl) {
        if (value == null) return;
        cache.put(key, JsonUtils.toJson(value));
    }

    @Override
    public void put(String key, Object value) {
        put(key, value, Duration.ofMinutes(10));
    }

    @Override
    public void evict(String key) {
        cache.invalidate(key);
    }

    @Override
    public void evictPattern(String pattern) {
        String regex = pattern.replace("*", ".*");
        cache.asMap().keySet().removeIf(k -> k.matches(regex));
    }
}
