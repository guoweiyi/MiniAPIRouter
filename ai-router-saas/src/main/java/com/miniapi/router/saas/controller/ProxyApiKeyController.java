package com.miniapi.router.saas.controller;

import com.miniapi.router.saas.context.TenantContext;
import com.miniapi.router.saas.dto.response.ApiResponse;
import com.miniapi.router.saas.entity.TenantDO;
import com.miniapi.router.saas.mapper.TenantMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/v1/tenant/proxy-keys")
public class ProxyApiKeyController {

    private final TenantMapper tenantMapper;
    private final StringRedisTemplate redis;
    private final SecureRandom random = new SecureRandom();

    public ProxyApiKeyController(TenantMapper tenantMapper, StringRedisTemplate redis) {
        this.tenantMapper = tenantMapper;
        this.redis = redis;
    }

    @PostMapping
    public ApiResponse<Object> generate() {
        Long tenantId = TenantContext.getTenantId();
        TenantDO tenant = tenantMapper.selectById(tenantId);
        if (tenant == null) {
            return ApiResponse.error(404, "Tenant not found");
        }
        byte[] bytes = new byte[16];
        random.nextBytes(bytes);
        String randomPart = HexFormat.of().formatHex(bytes);
        String apiKey = "sk-miniapi-" + tenant.getTenantCode() + "-" + randomPart;
        redis.opsForValue().set("proxykey:" + tenant.getTenantCode() + ":" + randomPart,
                String.valueOf(tenantId), 365, TimeUnit.DAYS);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("api_key", apiKey);
        result.put("tenant_code", tenant.getTenantCode());
        result.put("created_at", java.time.LocalDateTime.now());
        return ApiResponse.success(result);
    }

    @GetMapping
    public ApiResponse<Object> list() {
        Long tenantId = TenantContext.getTenantId();
        TenantDO tenant = tenantMapper.selectById(tenantId);
        if (tenant == null) return ApiResponse.error(404, "Tenant not found");
        java.util.Set<String> keys = redis.keys("proxykey:" + tenant.getTenantCode() + ":*");
        java.util.List<Map<String, Object>> list = new java.util.ArrayList<>();
        if (keys != null) {
            for (String k : keys) {
                String randomPart = k.substring(k.lastIndexOf(":") + 1);
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("api_key", "sk-miniapi-" + tenant.getTenantCode() + "-" + randomPart);
                m.put("api_key_masked", "sk-miniapi-" + tenant.getTenantCode() + "-..." + randomPart.substring(randomPart.length() - 4));
                list.add(m);
            }
        }
        return ApiResponse.success(list);
    }
}
