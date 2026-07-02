package com.miniapi.router.saas.service;

import com.miniapi.router.core.spi.LogSearchRepository;
import com.miniapi.router.saas.context.TenantContext;
import com.miniapi.router.saas.entity.TenantDO;
import com.miniapi.router.saas.mapper.TenantMapper;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class DashboardService {

    private final LogSearchRepository searchRepository;
    private final TenantMapper tenantMapper;

    public DashboardService(LogSearchRepository searchRepository, TenantMapper tenantMapper) {
        this.searchRepository = searchRepository;
        this.tenantMapper = tenantMapper;
    }

    public Map<String, Object> summary(String startTime, String endTime, String interval) {
        Long tenantId = TenantContext.getTenantId();
        Map<String, Object> summary = searchRepository.dashboardSummary(tenantId, startTime, endTime, interval);
        TenantDO tenant = tenantMapper.selectById(tenantId);
        if (tenant != null) {
            summary.put("quota_used", tenant.getQuotaUsed());
            summary.put("quota_limit", tenant.getQuotaLimit());
        }
        return summary;
    }
}
