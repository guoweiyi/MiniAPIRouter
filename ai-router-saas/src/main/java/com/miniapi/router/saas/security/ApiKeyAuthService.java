package com.miniapi.router.saas.security;

import com.miniapi.router.saas.context.TenantContext;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.miniapi.router.saas.entity.ApiKeyConfigDO;
import com.miniapi.router.saas.entity.TenantDO;
import com.miniapi.router.saas.mapper.ApiKeyConfigMapper;
import com.miniapi.router.saas.mapper.TenantMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import com.miniapi.router.core.util.JsonUtils;
import com.miniapi.router.core.util.CryptoUtils;
import com.miniapi.router.core.util.TraceUtils;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
public class ApiKeyAuthService {

    private final TenantMapper tenantMapper;
    private final StringRedisTemplate redis;
    private final CryptoUtils cryptoUtils;

    public ApiKeyAuthService(TenantMapper tenantMapper, StringRedisTemplate redis, CryptoUtils cryptoUtils) {
        this.tenantMapper = tenantMapper;
        this.redis = redis;
        this.cryptoUtils = cryptoUtils;
    }

    public AuthResult authenticate(String apiKey) {
        if (apiKey == null || !apiKey.startsWith("sk-miniapi-")) {
            return AuthResult.fail("INVALID_API_KEY", "Invalid API key format");
        }
        String[] parts = apiKey.substring("sk-miniapi-".length()).split("-", 2);
        if (parts.length < 2) {
            return AuthResult.fail("INVALID_API_KEY", "Invalid API key format");
        }
        String tenantCode = parts[0];
        String randomPart = parts[1];

        String cacheKey = "apikey:tenant:" + tenantCode;
        String cached = redis.opsForValue().get(cacheKey);
        Long tenantId;
        if (cached != null) {
            tenantId = Long.parseLong(cached);
        } else {
            LambdaQueryWrapper<TenantDO> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(TenantDO::getTenantCode, tenantCode);
            TenantDO tenant = tenantMapper.selectOne(wrapper);
            if (tenant == null) {
                return AuthResult.fail("INVALID_API_KEY", "Tenant not found: " + tenantCode);
            }
            if (tenant.getStatus() != null && tenant.getStatus() == 0) {
                return AuthResult.fail("TENANT_DISABLED", "Tenant is disabled");
            }
            tenantId = tenant.getId();
            redis.opsForValue().set(cacheKey, String.valueOf(tenantId), 5, TimeUnit.MINUTES);
        }

        String proxyKey = "proxykey:" + tenantCode + ":" + randomPart;
        if (Boolean.FALSE.equals(redis.hasKey(proxyKey))) {
            return AuthResult.fail("INVALID_API_KEY", "API key not recognized");
        }
        return AuthResult.success(tenantId, apiKey);
    }

    public String extractApiKey(HttpServletRequest request) {
        String auth = request.getHeader("Authorization");
        if (auth != null && auth.startsWith("Bearer sk-miniapi")) {
            return auth.substring(7);
        }
        String xApiKey = request.getHeader("x-api-key");
        if (xApiKey != null && xApiKey.startsWith("sk-miniapi")) {
            return xApiKey;
        }
        return null;
    }

    public record AuthResult(boolean success, Long tenantId, String apiKey, String errorCode, String errorMessage) {
        public static AuthResult success(Long tenantId, String apiKey) {
            return new AuthResult(true, tenantId, apiKey, null, null);
        }
        public static AuthResult fail(String code, String msg) {
            return new AuthResult(false, null, null, code, msg);
        }
    }
}
