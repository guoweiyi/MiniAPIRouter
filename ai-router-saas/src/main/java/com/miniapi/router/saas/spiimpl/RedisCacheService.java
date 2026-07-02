package com.miniapi.router.saas.spiimpl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.miniapi.router.core.spi.CacheService;
import com.miniapi.router.core.util.JsonUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Set;
import java.util.function.Function;

@Component
public class RedisCacheService implements CacheService {

    private final StringRedisTemplate redis;

    public RedisCacheService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public <T> T get(String key, Class<T> type) {
        String val = redis.opsForValue().get(key);
        if (val == null) return null;
        return JsonUtils.fromJson(val, type);
    }

    @Override
    public <T> T getOrLoad(String key, Class<T> type, Function<String, T> loader, Duration ttl) {
        T cached = get(key, type);
        if (cached != null) return cached;
        T value = loader.apply(key);
        if (value != null) put(key, value, ttl);
        return value;
    }

    @Override
    public void put(String key, Object value, Duration ttl) {
        redis.opsForValue().set(key, JsonUtils.toJson(value), ttl);
    }

    @Override
    public void put(String key, Object value) {
        redis.opsForValue().set(key, JsonUtils.toJson(value));
    }

    @Override
    public void evict(String key) {
        redis.delete(key);
    }

    @Override
    public void evictPattern(String pattern) {
        Set<String> keys = redis.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            redis.delete(keys);
        }
    }
}
