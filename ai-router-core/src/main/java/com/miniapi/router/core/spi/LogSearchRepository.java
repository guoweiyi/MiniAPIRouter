package com.miniapi.router.core.spi;

import com.miniapi.router.core.domain.RequestLogMeta;
import java.util.List;
import java.util.Map;

public interface LogSearchRepository {
    Map<String, Object> search(Map<String, Object> query, int page, int pageSize);
    Map<String, Object> getDetail(Long id, Long tenantId);
    Map<String, Object> dashboardSummary(Long tenantId, String startTime, String endTime, String interval);
    void index(RequestLogMeta meta, String promptContent, String responseContent);
}
