package com.miniapi.router.core.spi;

import java.time.Duration;
import java.util.function.Function;

public interface CacheService {
    <T> T get(String key, Class<T> type);
    <T> T getOrLoad(String key, Class<T> type, Function<String, T> loader, Duration ttl);
    void put(String key, Object value, Duration ttl);
    void put(String key, Object value);
    void evict(String key);
    void evictPattern(String pattern);
}
