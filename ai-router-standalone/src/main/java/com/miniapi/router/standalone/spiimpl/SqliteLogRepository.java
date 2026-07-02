package com.miniapi.router.standalone.spiimpl;

import com.miniapi.router.core.domain.RequestLogMeta;
import com.miniapi.router.core.spi.LogRepository;
import com.miniapi.router.standalone.entity.RequestLogMetaDO;
import com.miniapi.router.standalone.mapper.RequestLogMetaMapper;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class SqliteLogRepository implements LogRepository {

    private final RequestLogMetaMapper mapper;

    public SqliteLogRepository(RequestLogMetaMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void save(RequestLogMeta meta) {
        RequestLogMetaDO dO = new RequestLogMetaDO();
        dO.setTenantId(meta.getTenantId());
        dO.setUserId(meta.getUserId());
        dO.setTraceId(meta.getTraceId());
        dO.setRequestId(meta.getRequestId());
        dO.setClientIp(meta.getClientIp());
        dO.setProtocol(meta.getProtocol());
        dO.setModel(meta.getModel());
        dO.setMappedProvider(meta.getMappedProvider());
        dO.setApiKeyId(meta.getApiKeyId());
        dO.setRouteRuleId(meta.getRouteRuleId());
        dO.setIntent(meta.getIntent());
        dO.setPromptTokens(meta.getPromptTokens());
        dO.setCompletionTokens(meta.getCompletionTokens());
        dO.setTotalTokens(meta.getTotalTokens());
        dO.setLatencyMs(meta.getLatencyMs());
        dO.setTtftMs(meta.getTtftMs());
        dO.setStatus(meta.getStatus());
        dO.setFallbackCount(meta.getFallbackCount());
        dO.setErrorCode(meta.getErrorCode());
        dO.setErrorMessage(meta.getErrorMessage());
        dO.setPromptStorageUrl(meta.getPromptStorageUrl());
        dO.setResponseStorageUrl(meta.getResponseStorageUrl());
        dO.setCreatedAt(meta.getCreatedAt() != null ? meta.getCreatedAt() : LocalDateTime.now());
        mapper.insert(dO);
    }

    @Override
    public RequestLogMeta findById(Long id) {
        RequestLogMetaDO dO = mapper.selectById(id);
        if (dO == null) return null;
        return toDomain(dO);
    }

    private RequestLogMeta toDomain(RequestLogMetaDO dO) {
        RequestLogMeta m = new RequestLogMeta();
        m.setId(dO.getId());
        m.setTenantId(dO.getTenantId());
        m.setUserId(dO.getUserId());
        m.setTraceId(dO.getTraceId());
        m.setRequestId(dO.getRequestId());
        m.setClientIp(dO.getClientIp());
        m.setProtocol(dO.getProtocol());
        m.setModel(dO.getModel());
        m.setMappedProvider(dO.getMappedProvider());
        m.setApiKeyId(dO.getApiKeyId());
        m.setRouteRuleId(dO.getRouteRuleId());
        m.setIntent(dO.getIntent());
        m.setPromptTokens(dO.getPromptTokens());
        m.setCompletionTokens(dO.getCompletionTokens());
        m.setTotalTokens(dO.getTotalTokens());
        m.setLatencyMs(dO.getLatencyMs());
        m.setTtftMs(dO.getTtftMs());
        m.setStatus(dO.getStatus());
        m.setFallbackCount(dO.getFallbackCount());
        m.setErrorCode(dO.getErrorCode());
        m.setErrorMessage(dO.getErrorMessage());
        m.setPromptStorageUrl(dO.getPromptStorageUrl());
        m.setResponseStorageUrl(dO.getResponseStorageUrl());
        m.setCreatedAt(dO.getCreatedAt());
        return m;
    }
}
