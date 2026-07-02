package com.miniapi.router.core.spi;

import com.miniapi.router.core.domain.ApiKeyConfig;
import java.util.List;

public interface ApiKeyConfigRepository {
    ApiKeyConfig findById(Long id);
    ApiKeyConfig findByApiKey(String apiKey);
    List<ApiKeyConfig> findByTenantId(Long tenantId);
    List<ApiKeyConfig> findByIds(List<Long> ids);
    ApiKeyConfig save(ApiKeyConfig config);
    void update(ApiKeyConfig config);
    void delete(Long id, Long tenantId);
    void updateStatus(Long id, Long tenantId, int status);
    void updateHealthStatus(Long id, String healthStatus);
}
