package com.miniapi.router.saas.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.miniapi.router.core.domain.ApiKeyConfig;
import com.miniapi.router.core.exception.RouterException;
import com.miniapi.router.core.spi.ApiKeyConfigRepository;
import com.miniapi.router.core.util.CryptoUtils;
import com.miniapi.router.core.util.TraceUtils;
import com.miniapi.router.saas.context.TenantContext;
import com.miniapi.router.saas.dto.request.ApiKeyConfigRequest;
import com.miniapi.router.saas.dto.response.PageResult;
import com.miniapi.router.saas.entity.ApiKeyConfigDO;
import com.miniapi.router.saas.mapper.ApiKeyConfigMapper;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ApiKeyConfigService {

    private final ApiKeyConfigRepository keyRepository;
    private final ApiKeyConfigMapper mapper;
    private final CryptoUtils cryptoUtils;

    public ApiKeyConfigService(ApiKeyConfigRepository keyRepository, ApiKeyConfigMapper mapper, CryptoUtils cryptoUtils) {
        this.keyRepository = keyRepository;
        this.mapper = mapper;
        this.cryptoUtils = cryptoUtils;
    }

    public Map<String, Object> create(ApiKeyConfigRequest req) {
        Long tenantId = TenantContext.getTenantId();
        ApiKeyConfig config = new ApiKeyConfig();
        config.setTenantId(tenantId);
        config.setName(req.getName());
        config.setProvider(req.getProvider());
        config.setProtocol(req.getProtocol() != null ? req.getProtocol() : inferProtocol(req.getProvider()));
        config.setApiKey(req.getApiKey());
        config.setBaseUrl(req.getBaseUrl());
        config.setModels(req.getModels());
        config.setWeight(req.getWeight());
        config.setPriority(req.getPriority());
        config.setMaxConcurrent(req.getMaxConcurrent());
        config.setQpsLimit(req.getQpsLimit());
        config.setTimeoutMs(req.getTimeoutMs());
        config.setRetryCount(req.getRetryCount());
        config.setStatus(1);
        config.setHealthStatus("unknown");
        keyRepository.save(config);
        return toResponse(config);
    }

    public PageResult<Map<String, Object>> list(int page, int pageSize, String provider, Integer status, String healthStatus) {
        Long tenantId = TenantContext.getTenantId();
        LambdaQueryWrapper<ApiKeyConfigDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ApiKeyConfigDO::getTenantId, tenantId);
        if (provider != null) wrapper.eq(ApiKeyConfigDO::getProvider, provider);
        if (status != null) wrapper.eq(ApiKeyConfigDO::getStatus, status);
        if (healthStatus != null) wrapper.eq(ApiKeyConfigDO::getHealthStatus, healthStatus);
        wrapper.orderByDesc(ApiKeyConfigDO::getCreatedAt);

        Page<ApiKeyConfigDO> p = new Page<>(page, pageSize);
        Page<ApiKeyConfigDO> result = mapper.selectPage(p, wrapper);
        List<Map<String, Object>> list = result.getRecords().stream().map(dO -> {
            ApiKeyConfig c = keyRepository.findById(dO.getId());
            return c != null ? toResponse(c) : toResponseFromDO(dO);
        }).collect(Collectors.toList());
        return new PageResult<>(list, result.getTotal(), page, pageSize);
    }

    public Map<String, Object> update(Long id, ApiKeyConfigRequest req) {
        Long tenantId = TenantContext.getTenantId();
        ApiKeyConfig config = keyRepository.findById(id);
        if (config == null || !config.getTenantId().equals(tenantId)) {
            throw new RouterException("RESOURCE_NOT_FOUND", "API Key 配置不存在", 404);
        }
        if (req.getName() != null) config.setName(req.getName());
        if (req.getProvider() != null) config.setProvider(req.getProvider());
        if (req.getProtocol() != null) config.setProtocol(req.getProtocol());
        if (req.getApiKey() != null) config.setApiKey(req.getApiKey());
        if (req.getBaseUrl() != null) config.setBaseUrl(req.getBaseUrl());
        if (req.getModels() != null) config.setModels(req.getModels());
        if (req.getWeight() != null) config.setWeight(req.getWeight());
        if (req.getPriority() != null) config.setPriority(req.getPriority());
        if (req.getMaxConcurrent() != null) config.setMaxConcurrent(req.getMaxConcurrent());
        if (req.getQpsLimit() != null) config.setQpsLimit(req.getQpsLimit());
        if (req.getTimeoutMs() != null) config.setTimeoutMs(req.getTimeoutMs());
        if (req.getRetryCount() != null) config.setRetryCount(req.getRetryCount());
        keyRepository.update(config);
        return toResponse(keyRepository.findById(id));
    }

    public void delete(Long id) {
        Long tenantId = TenantContext.getTenantId();
        keyRepository.delete(id, tenantId);
    }

    public void updateStatus(Long id, boolean enabled) {
        Long tenantId = TenantContext.getTenantId();
        keyRepository.updateStatus(id, tenantId, enabled ? 1 : 0);
    }

    public Map<String, Object> healthCheck(Long id) {
        ApiKeyConfig config = keyRepository.findById(id);
        if (config == null) throw new RouterException("RESOURCE_NOT_FOUND", "配置不存在", 404);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", id);
        result.put("health_status", config.getHealthStatus() != null ? config.getHealthStatus() : "unknown");
        result.put("last_check_at", java.time.LocalDateTime.now());
        return result;
    }

    private String inferProtocol(String provider) {
        return "anthropic".equalsIgnoreCase(provider) ? "anthropic" : "openai";
    }

    private Map<String, Object> toResponse(ApiKeyConfig c) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", c.getId());
        m.put("name", c.getName());
        m.put("provider", c.getProvider());
        m.put("protocol", c.getProtocol());
        m.put("base_url", c.getBaseUrl());
        m.put("models", c.getModels());
        m.put("weight", c.getWeight());
        m.put("priority", c.getPriority());
        m.put("max_concurrent", c.getMaxConcurrent());
        m.put("qps_limit", c.getQpsLimit());
        m.put("timeout_ms", c.getTimeoutMs());
        m.put("retry_count", c.getRetryCount());
        m.put("status", c.getStatus());
        m.put("health_status", c.getHealthStatus());
        m.put("api_key_masked", cryptoUtils.mask(c.getApiKey()));
        m.put("created_at", c.getCreatedAt());
        return m;
    }

    private Map<String, Object> toResponseFromDO(ApiKeyConfigDO dO) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", dO.getId());
        m.put("name", dO.getName());
        m.put("provider", dO.getProvider());
        m.put("protocol", dO.getProtocol());
        m.put("base_url", dO.getBaseUrl());
        m.put("models", dO.getModels());
        m.put("weight", dO.getWeight());
        m.put("priority", dO.getPriority());
        m.put("status", dO.getStatus());
        m.put("health_status", dO.getHealthStatus());
        m.put("created_at", dO.getCreatedAt());
        return m;
    }
}
