package com.miniapi.router.saas.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.miniapi.router.core.exception.RouterException;
import com.miniapi.router.saas.context.TenantContext;
import com.miniapi.router.saas.dto.request.TenantCreateRequest;
import com.miniapi.router.saas.dto.response.PageResult;
import com.miniapi.router.saas.entity.TenantDO;
import com.miniapi.router.saas.mapper.TenantMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class TenantService {

    private final TenantMapper tenantMapper;

    public TenantService(TenantMapper tenantMapper) {
        this.tenantMapper = tenantMapper;
    }

    public Map<String, Object> create(TenantCreateRequest req) {
        LambdaQueryWrapper<TenantDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TenantDO::getTenantCode, req.getTenantCode());
        if (tenantMapper.selectCount(wrapper) > 0) {
            throw new RouterException("DUPLICATE_RESOURCE", "租户编码已存在", 409);
        }
        TenantDO tenant = new TenantDO();
        tenant.setTenantCode(req.getTenantCode());
        tenant.setTenantName(req.getTenantName());
        tenant.setPlan(req.getPlan());
        tenant.setQuotaLimit(req.getQuotaLimit());
        tenant.setQuotaUsed(0L);
        tenant.setQuotaResetDay(1);
        tenant.setMaxRps(req.getMaxRps());
        tenant.setStatus(1);
        if (req.getExpiresAt() != null) {
            tenant.setExpiresAt(LocalDateTime.parse(req.getExpiresAt().replace("Z", ""),
                    DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        }
        tenantMapper.insert(tenant);
        return toResponse(tenant);
    }

    public PageResult<Map<String, Object>> list(int page, int pageSize, String keyword, Integer status, String plan) {
        LambdaQueryWrapper<TenantDO> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(TenantDO::getTenantName, keyword)
                    .or().like(TenantDO::getTenantCode, keyword));
        }
        if (status != null) wrapper.eq(TenantDO::getStatus, status);
        if (plan != null) wrapper.eq(TenantDO::getPlan, plan);
        wrapper.orderByDesc(TenantDO::getCreatedAt);

        Page<TenantDO> p = new Page<>(page, pageSize);
        Page<TenantDO> result = tenantMapper.selectPage(p, wrapper);
        List<Map<String, Object>> list = result.getRecords().stream().map(this::toResponse).collect(Collectors.toList());
        return new PageResult<>(list, result.getTotal(), page, pageSize);
    }

    public Map<String, Object> update(Long id, TenantCreateRequest req) {
        TenantDO tenant = tenantMapper.selectById(id);
        if (tenant == null) {
            throw new RouterException("RESOURCE_NOT_FOUND", "租户不存在", 404);
        }
        if (req.getTenantName() != null) tenant.setTenantName(req.getTenantName());
        if (req.getPlan() != null) tenant.setPlan(req.getPlan());
        if (req.getQuotaLimit() != null) tenant.setQuotaLimit(req.getQuotaLimit());
        if (req.getMaxRps() != null) tenant.setMaxRps(req.getMaxRps());
        if (req.getExpiresAt() != null) {
            tenant.setExpiresAt(LocalDateTime.parse(req.getExpiresAt().replace("Z", ""),
                    DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        }
        tenantMapper.updateById(tenant);
        return toResponse(tenant);
    }

    public void delete(Long id) {
        tenantMapper.deleteById(id);
    }

    private Map<String, Object> toResponse(TenantDO tenant) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", tenant.getId());
        m.put("tenant_code", tenant.getTenantCode());
        m.put("tenant_name", tenant.getTenantName());
        m.put("plan", tenant.getPlan());
        m.put("quota_limit", tenant.getQuotaLimit());
        m.put("quota_used", tenant.getQuotaUsed());
        m.put("max_rps", tenant.getMaxRps());
        m.put("status", tenant.getStatus());
        m.put("expires_at", tenant.getExpiresAt());
        m.put("created_at", tenant.getCreatedAt());
        return m;
    }
}
