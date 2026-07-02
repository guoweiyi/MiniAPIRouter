package com.miniapi.router.standalone.service;

import com.miniapi.router.core.spi.LogSearchRepository;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class LogQueryService {

    private static final Long TENANT_ID = 1L;
    private final LogSearchRepository logSearchRepository;

    public LogQueryService(LogSearchRepository logSearchRepository) {
        this.logSearchRepository = logSearchRepository;
    }

    public Map<String, Object> search(Map<String, Object> query, int page, int pageSize) {
        query.put("tenantId", TENANT_ID);
        return logSearchRepository.search(query, page, pageSize);
    }

    public Map<String, Object> getDetail(Long id) {
        return logSearchRepository.getDetail(id, TENANT_ID);
    }

    public Map<String, Object> dashboardSummary(String startTime, String endTime, String interval) {
        return logSearchRepository.dashboardSummary(TENANT_ID, startTime, endTime, interval);
    }
}
