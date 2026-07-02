package com.miniapi.router.saas.spiimpl;

import com.miniapi.router.core.spi.RateLimiter;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class RedisRateLimiter implements RateLimiter {

    private final StringRedisTemplate redis;

    public RedisRateLimiter(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public boolean tryAcquire(String key, int limit, int windowSeconds) {
        String redisKey = "rate_limit:" + key;
        Long count = redis.opsForValue().increment(redisKey);
        if (count != null && count == 1) {
            redis.expire(redisKey, windowSeconds, TimeUnit.SECONDS);
        }
        return count != null && count <= limit;
    }

    @Override
    public long getRemaining(String key, int limit, int windowSeconds) {
        String redisKey = "rate_limit:" + key;
        String val = redis.opsForValue().get(redisKey);
        long current = val != null ? Long.parseLong(val) : 0;
        return Math.max(0, limit - current);
    }
}
