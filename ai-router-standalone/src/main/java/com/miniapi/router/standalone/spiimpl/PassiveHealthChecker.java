package com.miniapi.router.standalone.spiimpl;

import com.miniapi.router.core.domain.ApiKeyConfig;
import com.miniapi.router.core.spi.ApiKeyConfigRepository;
import com.miniapi.router.core.spi.HealthChecker;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class PassiveHealthChecker implements HealthChecker {

    private static final int FAILURE_THRESHOLD = 3;
    private final ApiKeyConfigRepository keyRepository;
    private final Map<Long, AtomicInteger> failureCounts = new ConcurrentHashMap<>();

    public PassiveHealthChecker(ApiKeyConfigRepository keyRepository) {
        this.keyRepository = keyRepository;
    }

    @Override
    public void check(ApiKeyConfig config) {
    }

    @Override
    public String getStatus(Long keyId) {
        AtomicInteger count = failureCounts.get(keyId);
        if (count == null || count.get() == 0) return "healthy";
        if (count.get() >= FAILURE_THRESHOLD) return "down";
        return "degraded";
    }

    @Override
    public void markDown(Long keyId, String reason) {
        AtomicInteger count = failureCounts.computeIfAbsent(keyId, k -> new AtomicInteger(0));
        int newCount = count.incrementAndGet();
        if (newCount >= FAILURE_THRESHOLD) {
            keyRepository.updateHealthStatus(keyId, "down");
        } else {
            keyRepository.updateHealthStatus(keyId, "degraded");
        }
    }

    @Override
    public void markHealthy(Long keyId) {
        AtomicInteger count = failureCounts.get(keyId);
        if (count != null) count.set(0);
        keyRepository.updateHealthStatus(keyId, "healthy");
    }
}
