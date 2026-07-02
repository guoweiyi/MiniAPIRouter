package com.miniapi.router.saas.spiimpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.miniapi.router.core.domain.RouteRule;
import com.miniapi.router.core.spi.RouteRuleRepository;
import com.miniapi.router.core.util.JsonUtils;
import com.miniapi.router.saas.entity.RouteRuleDO;
import com.miniapi.router.saas.mapper.RouteRuleMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Component
public class MybatisRouteRuleRepository implements RouteRuleRepository {

    private static final String CACHE_PREFIX = "rules:enabled:";
    private static final long CACHE_TTL_MINUTES = 5;

    private final RouteRuleMapper mapper;
    private final StringRedisTemplate redis;

    public MybatisRouteRuleRepository(RouteRuleMapper mapper, StringRedisTemplate redis) {
        this.mapper = mapper;
        this.redis = redis;
    }

    @Override
    public RouteRule findById(Long id) {
        RouteRuleDO dO = mapper.selectById(id);
        return dO != null ? toDomain(dO) : null;
    }

    @Override
    public List<RouteRule> findByTenantId(Long tenantId) {
        List<RouteRuleDO> list = mapper.selectList(
                new LambdaQueryWrapper<RouteRuleDO>().eq(RouteRuleDO::getTenantId, tenantId));
        return list.stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<RouteRule> findEnabledRules(Long tenantId) {
        String cacheKey = CACHE_PREFIX + tenantId;
        String cached = redis.opsForValue().get(cacheKey);
        if (cached != null) {
            try {
                return JsonUtils.fromJson(cached, new com.fasterxml.jackson.core.type.TypeReference<List<RouteRule>>() {});
            } catch (Exception ignored) {
            }
        }
        List<RouteRuleDO> list = mapper.selectList(
                new LambdaQueryWrapper<RouteRuleDO>()
                        .eq(RouteRuleDO::getTenantId, tenantId)
                        .eq(RouteRuleDO::getEnabled, 1)
                        .orderByAsc(RouteRuleDO::getPriority));
        List<RouteRule> rules = list.stream().map(this::toDomain).collect(Collectors.toList());
        redis.opsForValue().set(cacheKey, JsonUtils.toJson(rules), CACHE_TTL_MINUTES, TimeUnit.MINUTES);
        return rules;
    }

    @Override
    public RouteRule save(RouteRule rule) {
        RouteRuleDO dO = toDO(rule);
        mapper.insert(dO);
        rule.setId(dO.getId());
        evict(rule.getTenantId());
        return rule;
    }

    @Override
    public void update(RouteRule rule) {
        RouteRuleDO dO = toDO(rule);
        mapper.updateById(dO);
        evict(rule.getTenantId());
    }

    @Override
    public void delete(Long id, Long tenantId) {
        mapper.deleteById(id);
        evict(tenantId);
    }

    @Override
    public void updateEnabled(Long id, Long tenantId, boolean enabled) {
        RouteRuleDO dO = new RouteRuleDO();
        dO.setId(id);
        dO.setEnabled(enabled ? 1 : 0);
        mapper.updateById(dO);
        evict(tenantId);
    }

    private void evict(Long tenantId) {
        if (tenantId != null) {
            redis.delete(CACHE_PREFIX + tenantId);
        }
    }

    private RouteRule toDomain(RouteRuleDO dO) {
        RouteRule r = new RouteRule();
        r.setId(dO.getId());
        r.setTenantId(dO.getTenantId());
        r.setRuleName(dO.getRuleName());
        r.setMatchType(dO.getMatchType());
        r.setMatchPattern(dO.getMatchPattern());
        r.setTargetKeyIds(dO.getTargetKeyIds());
        r.setStrategy(dO.getStrategy());
        r.setIntentModel(dO.getIntentModel());
        r.setIntentWeights(dO.getIntentWeights());
        r.setFallbackEnabled(dO.getFallbackEnabled() != null && dO.getFallbackEnabled() == 1);
        r.setMaxFallback(dO.getMaxFallback());
        r.setPriority(dO.getPriority());
        r.setEnabled(dO.getEnabled() != null && dO.getEnabled() == 1);
        r.setDescription(dO.getDescription());
        r.setCreatedAt(dO.getCreatedAt());
        r.setUpdatedAt(dO.getUpdatedAt());
        return r;
    }

    private RouteRuleDO toDO(RouteRule r) {
        RouteRuleDO dO = new RouteRuleDO();
        dO.setId(r.getId());
        dO.setTenantId(r.getTenantId());
        dO.setRuleName(r.getRuleName());
        dO.setMatchType(r.getMatchType());
        dO.setMatchPattern(r.getMatchPattern());
        dO.setTargetKeyIds(r.getTargetKeyIds());
        dO.setStrategy(r.getStrategy());
        dO.setIntentModel(r.getIntentModel());
        dO.setIntentWeights(r.getIntentWeights());
        dO.setFallbackEnabled(Boolean.TRUE.equals(r.getFallbackEnabled()) ? 1 : 0);
        dO.setMaxFallback(r.getMaxFallback());
        dO.setPriority(r.getPriority());
        dO.setEnabled(Boolean.TRUE.equals(r.getEnabled()) ? 1 : 0);
        dO.setDescription(r.getDescription());
        return dO;
    }
}
