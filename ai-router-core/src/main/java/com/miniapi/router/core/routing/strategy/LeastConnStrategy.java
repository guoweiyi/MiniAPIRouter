package com.miniapi.router.core.routing.strategy;

import com.miniapi.router.core.domain.ApiKeyConfig;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class LeastConnStrategy implements RouteStrategy {

    private final Map<Long, AtomicInteger> activeConnections = new ConcurrentHashMap<>();

    @Override
    public ApiKeyConfig select(List<ApiKeyConfig> candidates) {
        if (candidates == null || candidates.isEmpty()) return null;
        return candidates.stream()
                .min(Comparator.comparingInt(k -> getConnCount(k.getId())))
                .orElse(candidates.get(0));
    }

    public void acquire(Long keyId) {
        activeConnections.computeIfAbsent(keyId, k -> new AtomicInteger(0)).incrementAndGet();
    }

    public void release(Long keyId) {
        AtomicInteger count = activeConnections.get(keyId);
        if (count != null) count.decrementAndGet();
    }

    private int getConnCount(Long keyId) {
        AtomicInteger count = activeConnections.get(keyId);
        return count != null ? count.get() : 0;
    }

    @Override
    public String name() { return "least_conn"; }
}
