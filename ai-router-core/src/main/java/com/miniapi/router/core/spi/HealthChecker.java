package com.miniapi.router.core.spi;

import com.miniapi.router.core.domain.ApiKeyConfig;

public interface HealthChecker {
    void check(ApiKeyConfig config);
    String getStatus(Long keyId);
    void markDown(Long keyId, String reason);
    void markHealthy(Long keyId);
}
