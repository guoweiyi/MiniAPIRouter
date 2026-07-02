package com.miniapi.router.standalone.spiimpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.miniapi.router.core.domain.RouteRule;
import com.miniapi.router.core.spi.RouteRuleRepository;
import com.miniapi.router.standalone.entity.RouteRuleDO;
import com.miniapi.router.standalone.mapper.RouteRuleMapper;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Component
public class SqliteRouteRuleRepository implements RouteRuleRepository {

    private final RouteRuleMapper mapper;
    private final Cache<Long, List<RouteRule>> enabledRulesCache;

    public SqliteRouteRuleRepository(RouteRuleMapper mapper) {
        this.mapper = mapper;
        this.enabledRulesCache = Caffeine.newBuilder()
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .maximumSize(50)
                .build();
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
    public List<RouteRule> findEnabledRules(Long tenantId) {
        List<RouteRule> cached = enabledRulesCache.getIfPresent(tenantId);
        if (cached != null) return cached;
        List<RouteRuleDO> list = mapper.selectList(
                new LambdaQueryWrapper<RouteRuleDO>()
                        .eq(RouteRuleDO::getTenantId, tenantId)
                        .eq(RouteRuleDO::getEnabled, 1)
                        .orderByAsc(RouteRuleDO::getPriority));
        List<RouteRule> rules = list.stream().map(this::toDomain).collect(Collectors.toList());
        enabledRulesCache.put(tenantId, rules);
        return rules;
    }

    @Override
    public RouteRule save(RouteRule rule) {
        RouteRuleDO dO = toDO(rule);
        mapper.insert(dO);
        rule.setId(dO.getId());
        enabledRulesCache.invalidate(rule.getTenantId());
        return rule;
    }

    @Override
    public void update(RouteRule rule) {
        RouteRuleDO dO = toDO(rule);
        mapper.updateById(dO);
        enabledRulesCache.invalidate(rule.getTenantId());
    }

    @Override
    public void delete(Long id, Long tenantId) {
        mapper.deleteById(id);
        enabledRulesCache.invalidate(tenantId);
    }

    @Override
    public void updateEnabled(Long id, Long tenantId, boolean enabled) {
        RouteRuleDO dO = new RouteRuleDO();
        dO.setId(id);
        dO.setEnabled(enabled ? 1 : 0);
        mapper.updateById(dO);
        enabledRulesCache.invalidate(tenantId);
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
