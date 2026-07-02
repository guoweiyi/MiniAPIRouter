package com.miniapi.router.standalone.spiimpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.miniapi.router.core.domain.ApiKeyConfig;
import com.miniapi.router.core.spi.ApiKeyConfigRepository;
import com.miniapi.router.core.util.CryptoUtils;
import com.miniapi.router.core.util.JsonUtils;
import com.miniapi.router.standalone.entity.ApiKeyConfigDO;
import com.miniapi.router.standalone.mapper.ApiKeyConfigMapper;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Component
public class SqliteApiKeyConfigRepository implements ApiKeyConfigRepository {

    private static final long CACHE_TTL_MINUTES = 5;

    private final ApiKeyConfigMapper mapper;
    private final CryptoUtils cryptoUtils;
    private final Cache<Long, ApiKeyConfig> cache;

    public SqliteApiKeyConfigRepository(ApiKeyConfigMapper mapper, CryptoUtils cryptoUtils) {
        this.mapper = mapper;
        this.cryptoUtils = cryptoUtils;
        this.cache = Caffeine.newBuilder()
                .expireAfterWrite(CACHE_TTL_MINUTES, TimeUnit.MINUTES)
                .maximumSize(200)
                .build();
    }

    @Override
    public ApiKeyConfig findById(Long id) {
        ApiKeyConfig cached = cache.getIfPresent(id);
        if (cached != null) return cached;
        ApiKeyConfigDO dO = mapper.selectById(id);
        if (dO == null) return null;
        ApiKeyConfig config = toDomain(dO);
        cache.put(id, config);
        return config;
    }

    @Override
    public ApiKeyConfig findByApiKey(String apiKey) {
        return null;
    }

    @Override
    public List<ApiKeyConfig> findByTenantId(Long tenantId) {
        List<ApiKeyConfigDO> list = mapper.selectList(
                new LambdaQueryWrapper<ApiKeyConfigDO>().eq(ApiKeyConfigDO::getTenantId, tenantId));
        return list.stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<ApiKeyConfig> findByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return List.of();
        List<ApiKeyConfig> result = new java.util.ArrayList<>();
        List<Long> missedIds = new java.util.ArrayList<>();
        for (Long id : ids) {
            ApiKeyConfig cached = cache.getIfPresent(id);
            if (cached != null) {
                result.add(cached);
            } else {
                missedIds.add(id);
            }
        }
        if (!missedIds.isEmpty()) {
            List<ApiKeyConfigDO> dbList = mapper.selectBatchIds(missedIds);
            for (ApiKeyConfigDO dO : dbList) {
                ApiKeyConfig config = toDomain(dO);
                cache.put(dO.getId(), config);
                result.add(config);
            }
        }
        return result;
    }

    @Override
    public ApiKeyConfig save(ApiKeyConfig config) {
        ApiKeyConfigDO dO = toDO(config);
        if (config.getApiKey() != null) {
            dO.setApiKeyEnc(cryptoUtils.encrypt(config.getApiKey()));
        }
        mapper.insert(dO);
        config.setId(dO.getId());
        return config;
    }

    @Override
    public void update(ApiKeyConfig config) {
        ApiKeyConfigDO dO = toDO(config);
        if (config.getApiKey() != null) {
            dO.setApiKeyEnc(cryptoUtils.encrypt(config.getApiKey()));
        }
        mapper.updateById(dO);
        cache.invalidate(config.getId());
    }

    @Override
    public void delete(Long id, Long tenantId) {
        mapper.deleteById(id);
        cache.invalidate(id);
    }

    @Override
    public void updateStatus(Long id, Long tenantId, int status) {
        ApiKeyConfigDO dO = new ApiKeyConfigDO();
        dO.setId(id);
        dO.setStatus(status);
        mapper.updateById(dO);
        cache.invalidate(id);
    }

    @Override
    public void updateHealthStatus(Long id, String healthStatus) {
        ApiKeyConfigDO dO = new ApiKeyConfigDO();
        dO.setId(id);
        dO.setHealthStatus(healthStatus);
        dO.setLastHealthCheckAt(java.time.LocalDateTime.now());
        mapper.updateById(dO);
        cache.invalidate(id);
    }

    private ApiKeyConfig toDomain(ApiKeyConfigDO dO) {
        ApiKeyConfig c = new ApiKeyConfig();
        c.setId(dO.getId());
        c.setTenantId(dO.getTenantId());
        c.setName(dO.getName());
        c.setProvider(dO.getProvider());
        c.setProtocol(dO.getProtocol());
        c.setApiKeyEnc(dO.getApiKeyEnc());
        c.setApiKey(cryptoUtils.decrypt(dO.getApiKeyEnc()));
        c.setBaseUrl(dO.getBaseUrl());
        c.setModels(dO.getModels());
        c.setWeight(dO.getWeight());
        c.setPriority(dO.getPriority());
        c.setMaxConcurrent(dO.getMaxConcurrent());
        c.setQpsLimit(dO.getQpsLimit());
        c.setTimeoutMs(dO.getTimeoutMs());
        c.setRetryCount(dO.getRetryCount());
        c.setStatus(dO.getStatus());
        c.setHealthStatus(dO.getHealthStatus());
        c.setLastHealthCheckAt(dO.getLastHealthCheckAt());
        c.setCreatedAt(dO.getCreatedAt());
        c.setUpdatedAt(dO.getUpdatedAt());
        return c;
    }

    private ApiKeyConfigDO toDO(ApiKeyConfig c) {
        ApiKeyConfigDO dO = new ApiKeyConfigDO();
        dO.setId(c.getId());
        dO.setTenantId(c.getTenantId());
        dO.setName(c.getName());
        dO.setProvider(c.getProvider());
        dO.setProtocol(c.getProtocol());
        dO.setApiKeyEnc(c.getApiKeyEnc());
        dO.setBaseUrl(c.getBaseUrl());
        dO.setModels(c.getModels());
        dO.setWeight(c.getWeight());
        dO.setPriority(c.getPriority());
        dO.setMaxConcurrent(c.getMaxConcurrent());
        dO.setQpsLimit(c.getQpsLimit());
        dO.setTimeoutMs(c.getTimeoutMs());
        dO.setRetryCount(c.getRetryCount());
        dO.setStatus(c.getStatus());
        dO.setHealthStatus(c.getHealthStatus());
        dO.setLastHealthCheckAt(c.getLastHealthCheckAt());
        return dO;
    }
}
