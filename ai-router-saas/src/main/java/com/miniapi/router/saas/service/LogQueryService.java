package com.miniapi.router.saas.service;

import com.miniapi.router.core.spi.LogSearchRepository;
import com.miniapi.router.saas.context.TenantContext;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class LogQueryService {

    private final LogSearchRepository searchRepository;

    public LogQueryService(LogSearchRepository searchRepository) {
        this.searchRepository = searchRepository;
    }

    public Map<String, Object> search(int page, int pageSize, String startTime, String endTime,
                                      String model, String provider, String status, String keyword, String traceId) {
        Long tenantId = TenantContext.getTenantId();
        Map<String, Object> query = new LinkedHashMap<>();
        query.put("tenantId", tenantId);
        if (startTime != null) query.put("startTime", startTime);
        if (endTime != null) query.put("endTime", endTime);
        if (model != null) query.put("model", model);
        if (provider != null) query.put("provider", provider);
        if (status != null) query.put("status", status);
        if (keyword != null) query.put("keyword", keyword);
        if (traceId != null) query.put("traceId", traceId);
        return searchRepository.search(query, page, pageSize);
    }

    public Map<String, Object> getDetail(Long id) {
        Long tenantId = TenantContext.getTenantId();
        return searchRepository.getDetail(id, tenantId);
    }
}
