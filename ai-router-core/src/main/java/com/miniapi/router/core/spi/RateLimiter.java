package com.miniapi.router.core.spi;

public interface RateLimiter {
    boolean tryAcquire(String key, int limit, int windowSeconds);
    long getRemaining(String key, int limit, int windowSeconds);
}
