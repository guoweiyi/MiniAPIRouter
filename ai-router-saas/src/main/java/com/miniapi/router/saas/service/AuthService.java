package com.miniapi.router.saas.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.miniapi.router.saas.entity.SysUserDO;
import com.miniapi.router.saas.entity.TenantDO;
import com.miniapi.router.saas.mapper.SysUserMapper;
import com.miniapi.router.saas.mapper.TenantMapper;
import com.miniapi.router.saas.security.JwtTokenProvider;
import com.miniapi.router.core.exception.RouterException;
import com.miniapi.router.core.util.TraceUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class AuthService {

    private final SysUserMapper userMapper;
    private final TenantMapper tenantMapper;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;

    public AuthService(SysUserMapper userMapper, TenantMapper tenantMapper,
                       JwtTokenProvider jwtTokenProvider, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.tenantMapper = tenantMapper;
        this.jwtTokenProvider = jwtTokenProvider;
        this.passwordEncoder = passwordEncoder;
    }

    public Map<String, Object> login(String username, String password, String tenantCode) {
        LambdaQueryWrapper<SysUserDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUserDO::getUsername, username);
        SysUserDO user = userMapper.selectOne(wrapper);

        if (user == null || !passwordEncoder.matches(password, user.getPassword())) {
            throw new RouterException("UNAUTHORIZED", "用户名或密码错误", 401);
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new RouterException("FORBIDDEN", "用户已被禁用", 403);
        }

        Long tenantId = user.getTenantId();
        String tenantName = "";
        if (tenantId != null && tenantId > 0) {
            TenantDO tenant = tenantMapper.selectById(tenantId);
            if (tenant != null) {
                tenantName = tenant.getTenantName();
                if (tenant.getStatus() != null && tenant.getStatus() == 0) {
                    throw new RouterException("TENANT_DISABLED", "租户已被禁用", 403);
                }
            }
        }

        user.setLastLoginAt(LocalDateTime.now());
        userMapper.updateById(user);

        String token = jwtTokenProvider.generateToken(user.getId(), user.getUsername(),
                user.getRole(), tenantId, tenantName);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("token", token);
        result.put("expires_in", jwtTokenProvider.getExpirationMs() / 1000);

        Map<String, Object> userInfo = new LinkedHashMap<>();
        userInfo.put("id", user.getId());
        userInfo.put("username", user.getUsername());
        userInfo.put("nickname", user.getNickname());
        userInfo.put("role", user.getRole());
        userInfo.put("tenant_id", tenantId);
        userInfo.put("tenant_name", tenantName);
        result.put("user", userInfo);

        return result;
    }
}
