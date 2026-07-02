package com.miniapi.router.standalone.spiimpl;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.miniapi.router.core.spi.RateLimiter;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class CaffeineRateLimiter implements RateLimiter {

    private final Cache<String, long[]> counters;

    public CaffeineRateLimiter() {
        this.counters = Caffeine.newBuilder()
                .expireAfterWrite(60, TimeUnit.SECONDS)
                .maximumSize(10000)
                .build();
    }

    @Override
    public synchronized boolean tryAcquire(String key, int limit, int windowSeconds) {
        String cacheKey = key + ":" + (System.currentTimeMillis() / 1000 / windowSeconds);
        long[] count = counters.getIfPresent(cacheKey);
        if (count == null) {
            count = new long[]{0};
            counters.put(cacheKey, count);
        }
        count[0]++;
        return count[0] <= limit;
    }

    @Override
    public long getRemaining(String key, int limit, int windowSeconds) {
        String cacheKey = key + ":" + (System.currentTimeMillis() / 1000 / windowSeconds);
        long[] count = counters.getIfPresent(cacheKey);
        long current = count != null ? count[0] : 0;
        return Math.max(0, limit - current);
    }
}
