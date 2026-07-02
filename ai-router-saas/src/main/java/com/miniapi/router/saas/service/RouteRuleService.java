package com.miniapi.router.saas.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.miniapi.router.core.domain.ApiKeyConfig;
import com.miniapi.router.core.domain.RouteRule;
import com.miniapi.router.core.exception.RouterException;
import com.miniapi.router.core.spi.ApiKeyConfigRepository;
import com.miniapi.router.core.spi.RouteRuleRepository;
import com.miniapi.router.saas.context.TenantContext;
import com.miniapi.router.saas.dto.request.RouteRuleRequest;
import com.miniapi.router.saas.dto.response.PageResult;
import com.miniapi.router.saas.entity.RouteRuleDO;
import com.miniapi.router.saas.mapper.RouteRuleMapper;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class RouteRuleService {

    private final RouteRuleRepository ruleRepository;
    private final ApiKeyConfigRepository keyRepository;
    private final RouteRuleMapper mapper;

    public RouteRuleService(RouteRuleRepository ruleRepository, ApiKeyConfigRepository keyRepository, RouteRuleMapper mapper) {
        this.ruleRepository = ruleRepository;
        this.keyRepository = keyRepository;
        this.mapper = mapper;
    }

    public Map<String, Object> create(RouteRuleRequest req) {
        Long tenantId = TenantContext.getTenantId();
        RouteRule rule = new RouteRule();
        rule.setTenantId(tenantId);
        rule.setRuleName(req.getRuleName());
        rule.setMatchType(req.getMatchType());
        rule.setMatchPattern(req.getMatchPattern());
        rule.setTargetKeyIds(req.getTargetKeyIds());
        rule.setStrategy(req.getStrategy());
        rule.setIntentModel(req.getIntentModel());
        rule.setIntentWeights(req.getIntentWeights());
        rule.setFallbackEnabled(req.getFallbackEnabled());
        ruleRepository.save(rule);
        return toResponse(ruleRepository.findById(rule.getId()));
    }

    public PageResult<Map<String, Object>> list(int page, int pageSize) {
        Long tenantId = TenantContext.getTenantId();
        LambdaQueryWrapper<RouteRuleDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RouteRuleDO::getTenantId, tenantId).orderByDesc(RouteRuleDO::getCreatedAt);
        Page<RouteRuleDO> p = new Page<>(page, pageSize);
        Page<RouteRuleDO> result = mapper.selectPage(p, wrapper);
        List<Map<String, Object>> list = result.getRecords().stream()
                .map(dO -> toResponse(ruleRepository.findById(dO.getId())))
                .collect(Collectors.toList());
        return new PageResult<>(list, result.getTotal(), page, pageSize);
    }

    public Map<String, Object> findById(Long id) {
        RouteRule rule = ruleRepository.findById(id);
        if (rule == null) throw new RouterException("RESOURCE_NOT_FOUND", "规则不存在", 404);
        return toResponse(rule);
    }

    public Map<String, Object> update(Long id, RouteRuleRequest req) {
        Long tenantId = TenantContext.getTenantId();
        RouteRule rule = ruleRepository.findById(id);
        if (rule == null || !rule.getTenantId().equals(tenantId)) {
            throw new RouterException("RESOURCE_NOT_FOUND", "规则不存在", 404);
        }
        if (req.getRuleName() != null) rule.setRuleName(req.getRuleName());
        if (req.getMatchType() != null) rule.setMatchType(req.getMatchType());
        if (req.getMatchPattern() != null) rule.setMatchPattern(req.getMatchPattern());
        if (req.getTargetKeyIds() != null) rule.setTargetKeyIds(req.getTargetKeyIds());
        if (req.getStrategy() != null) rule.setStrategy(req.getStrategy());
        if (req.getIntentModel() != null) rule.setIntentModel(req.getIntentModel());
        if (req.getIntentWeights() != null) rule.setIntentWeights(req.getIntentWeights());
        if (req.getFallbackEnabled() != null) rule.setFallbackEnabled(req.getFallbackEnabled());
        if (req.getMaxFallback() != null) rule.setMaxFallback(req.getMaxFallback());
        if (req.getPriority() != null) rule.setPriority(req.getPriority());
        if (req.getDescription() != null) rule.setDescription(req.getDescription());
        ruleRepository.update(rule);
        return toResponse(ruleRepository.findById(id));
    }

    public void delete(Long id) {
        ruleRepository.delete(id, TenantContext.getTenantId());
    }

    public void updateEnabled(Long id, boolean enabled) {
        ruleRepository.updateEnabled(id, TenantContext.getTenantId(), enabled);
    }

    private Map<String, Object> toResponse(RouteRule rule) {
        if (rule == null) return Map.of();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", rule.getId());
        m.put("rule_name", rule.getRuleName());
        m.put("match_type", rule.getMatchType());
        m.put("match_pattern", rule.getMatchPattern());
        m.put("target_key_ids", rule.getTargetKeyIds());
        if (rule.getTargetKeyIds() != null && !rule.getTargetKeyIds().isEmpty()) {
            List<ApiKeyConfig> keys = keyRepository.findByIds(rule.getTargetKeyIds());
            List<Map<String, Object>> targetKeys = keys.stream().map(k -> {
                Map<String, Object> tk = new LinkedHashMap<>();
                tk.put("id", k.getId());
                tk.put("name", k.getName());
                tk.put("provider", k.getProvider());
                tk.put("weight", k.getWeight());
                return tk;
            }).collect(Collectors.toList());
            m.put("target_keys", targetKeys);
        }
        m.put("strategy", rule.getStrategy());
        m.put("intent_model", rule.getIntentModel());
        m.put("intent_weights", rule.getIntentWeights());
        m.put("fallback_enabled", rule.getFallbackEnabled());
        m.put("max_fallback", rule.getMaxFallback());
        m.put("priority", rule.getPriority());
        m.put("enabled", rule.getEnabled());
        m.put("description", rule.getDescription());
        m.put("created_at", rule.getCreatedAt());
        return m;
    }
}
