package com.miniapi.router.saas.spiimpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.miniapi.router.core.domain.RequestLogMeta;
import com.miniapi.router.core.spi.LogSearchRepository;
import com.miniapi.router.saas.entity.RequestLogMetaDO;
import com.miniapi.router.saas.mapper.RequestLogMetaMapper;
import com.miniapi.router.saas.mapper.ApiKeyConfigMapper;
import com.miniapi.router.core.spi.BlobStorage;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class MariaDbLogSearchRepository implements LogSearchRepository {

    private final RequestLogMetaMapper mapper;
    private final BlobStorage blobStorage;

    public MariaDbLogSearchRepository(RequestLogMetaMapper mapper, BlobStorage blobStorage) {
        this.mapper = mapper;
        this.blobStorage = blobStorage;
    }

    @Override
    public Map<String, Object> search(Map<String, Object> query, int page, int pageSize) {
        Long tenantId = (Long) query.get("tenantId");
        LambdaQueryWrapper<RequestLogMetaDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RequestLogMetaDO::getTenantId, tenantId);

        if (query.get("startTime") != null) {
            wrapper.ge(RequestLogMetaDO::getCreatedAt, query.get("startTime"));
        }
        if (query.get("endTime") != null) {
            wrapper.le(RequestLogMetaDO::getCreatedAt, query.get("endTime"));
        }
        if (query.get("model") != null) {
            wrapper.eq(RequestLogMetaDO::getModel, query.get("model"));
        }
        if (query.get("provider") != null) {
            wrapper.eq(RequestLogMetaDO::getMappedProvider, query.get("provider"));
        }
        if (query.get("status") != null) {
            wrapper.eq(RequestLogMetaDO::getStatus, query.get("status"));
        }
        if (query.get("traceId") != null) {
            wrapper.eq(RequestLogMetaDO::getTraceId, query.get("traceId"));
        }
        if (query.get("keyword") != null) {
            wrapper.and(w -> w.like(RequestLogMetaDO::getErrorMessage, query.get("keyword"))
                    .or().like(RequestLogMetaDO::getModel, query.get("keyword")));
        }
        wrapper.orderByDesc(RequestLogMetaDO::getCreatedAt);

        Page<RequestLogMetaDO> p = new Page<>(page, pageSize);
        Page<RequestLogMetaDO> result = mapper.selectPage(p, wrapper);

        List<Map<String, Object>> list = result.getRecords().stream()
                .map(this::toMap)
                .collect(Collectors.toList());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("list", list);
        response.put("total", result.getTotal());
        response.put("page", page);
        response.put("page_size", pageSize);
        return response;
    }

    @Override
    public Map<String, Object> getDetail(Long id, Long tenantId) {
        LambdaQueryWrapper<RequestLogMetaDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RequestLogMetaDO::getId, id).eq(RequestLogMetaDO::getTenantId, tenantId);
        RequestLogMetaDO dO = mapper.selectOne(wrapper);
        if (dO == null) return null;
        Map<String, Object> detail = toMap(dO);
        if (dO.getPromptStorageUrl() != null) {
            String content = blobStorage.read(dO.getPromptStorageUrl());
            if (content != null) {
                try {
                    detail.put("messages", com.miniapi.router.core.util.JsonUtils.parse(content));
                } catch (Exception e) {
                    detail.put("messages", content);
                }
            }
        }
        if (dO.getResponseStorageUrl() != null) {
            String content = blobStorage.read(dO.getResponseStorageUrl());
            if (content != null) {
                detail.put("response_content", content);
            }
        }
        detail.put("fallback_events", List.of());
        detail.put("error", dO.getErrorMessage());
        return detail;
    }

    @Override
    public Map<String, Object> dashboardSummary(Long tenantId, String startTime, String endTime, String interval) {
        LocalDateTime start = startTime != null ? LocalDateTime.parse(startTime.replace("Z", "")) : LocalDateTime.now().minusDays(7);
        LocalDateTime end = endTime != null ? LocalDateTime.parse(endTime.replace("Z", "")) : LocalDateTime.now();

        LambdaQueryWrapper<RequestLogMetaDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RequestLogMetaDO::getTenantId, tenantId)
                .ge(RequestLogMetaDO::getCreatedAt, start)
                .le(RequestLogMetaDO::getCreatedAt, end);
        List<RequestLogMetaDO> all = mapper.selectList(wrapper);

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("total_requests", all.size());
        summary.put("total_tokens", all.stream().mapToLong(l -> l.getTotalTokens() != null ? l.getTotalTokens() : 0).sum());
        summary.put("avg_latency_ms", all.isEmpty() ? 0 : (int) all.stream().mapToInt(l -> l.getLatencyMs() != null ? l.getLatencyMs() : 0).average().orElse(0));
        summary.put("avg_ttft_ms", all.isEmpty() ? 0 : (int) all.stream().filter(l -> l.getTtftMs() != null).mapToInt(RequestLogMetaDO::getTtftMs).average().orElse(0));
        long successCount = all.stream().filter(l -> "success".equals(l.getStatus())).count();
        summary.put("success_rate", all.isEmpty() ? 0 : (double) successCount / all.size());
        long fallbackCount = all.stream().filter(l -> l.getFallbackCount() != null && l.getFallbackCount() > 0).count();
        summary.put("fallback_rate", all.isEmpty() ? 0 : (double) fallbackCount / all.size());

        List<Map<String, Object>> modelDist = mapper.modelDistribution(tenantId,
                start.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                end.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        summary.put("model_distribution", modelDist);

        List<Map<String, Object>> providerDist = mapper.providerDistribution(tenantId,
                start.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                end.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        long totalProvider = providerDist.stream().mapToLong(m -> ((Number) m.get("cnt")).longValue()).sum();
        providerDist.forEach(m -> m.put("percentage", totalProvider == 0 ? 0 : ((Number) m.get("cnt")).doubleValue() / totalProvider));
        summary.put("provider_distribution", providerDist);

        summary.put("tokens_trend", List.of());
        return summary;
    }

    @Override
    public void index(RequestLogMeta meta, String promptContent, String responseContent) {
    }

    private Map<String, Object> toMap(RequestLogMetaDO dO) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", dO.getId());
        m.put("trace_id", dO.getTraceId());
        m.put("request_id", dO.getRequestId());
        m.put("protocol", dO.getProtocol());
        m.put("model", dO.getModel());
        m.put("mapped_provider", dO.getMappedProvider());
        m.put("api_key_id", dO.getApiKeyId());
        m.put("route_rule_id", dO.getRouteRuleId());
        m.put("intent", dO.getIntent());
        m.put("prompt_tokens", dO.getPromptTokens());
        m.put("completion_tokens", dO.getCompletionTokens());
        m.put("total_tokens", dO.getTotalTokens());
        m.put("latency_ms", dO.getLatencyMs());
        m.put("ttft_ms", dO.getTtftMs());
        m.put("status", dO.getStatus());
        m.put("fallback_count", dO.getFallbackCount());
        m.put("prompt_storage_url", dO.getPromptStorageUrl());
        m.put("created_at", dO.getCreatedAt());
        return m;
    }
}
