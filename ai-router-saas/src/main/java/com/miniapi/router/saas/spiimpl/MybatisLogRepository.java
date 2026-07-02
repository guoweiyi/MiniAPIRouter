package com.miniapi.router.saas.spiimpl;

import com.miniapi.router.core.domain.RequestLogMeta;
import com.miniapi.router.core.spi.LogRepository;
import com.miniapi.router.saas.entity.RequestLogMetaDO;
import com.miniapi.router.saas.mapper.RequestLogMetaMapper;
import org.springframework.stereotype.Component;

@Component
public class MybatisLogRepository implements LogRepository {

    private final RequestLogMetaMapper mapper;

    public MybatisLogRepository(RequestLogMetaMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void save(RequestLogMeta meta) {
        mapper.insert(toDO(meta));
        if (meta.getId() == null) {
            meta.setId(toDO(meta).getId());
        }
    }

    @Override
    public RequestLogMeta findById(Long id) {
        RequestLogMetaDO dO = mapper.selectById(id);
        return dO != null ? toDomain(dO) : null;
    }

    private RequestLogMetaDO toDO(RequestLogMeta m) {
        RequestLogMetaDO dO = new RequestLogMetaDO();
        dO.setId(m.getId());
        dO.setTenantId(m.getTenantId());
        dO.setUserId(m.getUserId());
        dO.setTraceId(m.getTraceId());
        dO.setRequestId(m.getRequestId());
        dO.setClientIp(m.getClientIp());
        dO.setProtocol(m.getProtocol());
        dO.setModel(m.getModel());
        dO.setMappedProvider(m.getMappedProvider());
        dO.setApiKeyId(m.getApiKeyId());
        dO.setRouteRuleId(m.getRouteRuleId());
        dO.setIntent(m.getIntent());
        dO.setPromptTokens(m.getPromptTokens());
        dO.setCompletionTokens(m.getCompletionTokens());
        dO.setTotalTokens(m.getTotalTokens());
        dO.setLatencyMs(m.getLatencyMs());
        dO.setTtftMs(m.getTtftMs());
        dO.setStatus(m.getStatus());
        dO.setFallbackCount(m.getFallbackCount());
        dO.setErrorCode(m.getErrorCode());
        dO.setErrorMessage(m.getErrorMessage());
        dO.setPromptStorageUrl(m.getPromptStorageUrl());
        dO.setResponseStorageUrl(m.getResponseStorageUrl());
        dO.setCreatedAt(m.getCreatedAt());
        return dO;
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
