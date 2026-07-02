package com.miniapi.router.core.spi;

import com.miniapi.router.core.domain.RouteRule;
import java.util.List;

public interface RouteRuleRepository {
    RouteRule findById(Long id);
    List<RouteRule> findByTenantId(Long tenantId);
    List<RouteRule> findEnabledRules(Long tenantId);
    RouteRule save(RouteRule rule);
    void update(RouteRule rule);
    void delete(Long id, Long tenantId);
    void updateEnabled(Long id, Long tenantId, boolean enabled);
}
