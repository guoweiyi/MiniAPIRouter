package com.miniapi.router.saas.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.miniapi.router.core.exception.RouterException;
import com.miniapi.router.saas.context.TenantContext;
import com.miniapi.router.saas.dto.response.PageResult;
import com.miniapi.router.saas.entity.SysUserDO;
import com.miniapi.router.saas.mapper.SysUserMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final SysUserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public UserService(SysUserMapper userMapper, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    public PageResult<Map<String, Object>> list(int page, int pageSize, String keyword) {
        Long tenantId = TenantContext.getTenantId();
        String role = TenantContext.getRole();
        LambdaQueryWrapper<SysUserDO> wrapper = new LambdaQueryWrapper<>();
        if (!"super_admin".equals(role)) {
            wrapper.eq(SysUserDO::getTenantId, tenantId);
        }
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(SysUserDO::getUsername, keyword);
        }
        wrapper.orderByDesc(SysUserDO::getCreatedAt);

        Page<SysUserDO> p = new Page<>(page, pageSize);
        Page<SysUserDO> result = userMapper.selectPage(p, wrapper);
        List<Map<String, Object>> list = result.getRecords().stream().map(this::toResponse).collect(Collectors.toList());
        return new PageResult<>(list, result.getTotal(), page, pageSize);
    }

    public Map<String, Object> create(Map<String, Object> body) {
        Long tenantId = TenantContext.getTenantId();
        SysUserDO user = new SysUserDO();
        user.setTenantId(tenantId);
        user.setUsername((String) body.get("username"));
        user.setPassword(passwordEncoder.encode((String) body.get("password")));
        user.setNickname((String) body.get("nickname"));
        user.setEmail((String) body.get("email"));
        user.setPhone((String) body.get("phone"));
        user.setRole(body.get("role") != null ? (String) body.get("role") : "user");
        user.setStatus(1);
        userMapper.insert(user);
        return toResponse(user);
    }

    public Map<String, Object> update(Long id, Map<String, Object> body) {
        SysUserDO user = userMapper.selectById(id);
        if (user == null) throw new RouterException("RESOURCE_NOT_FOUND", "用户不存在", 404);
        if (body.get("nickname") != null) user.setNickname((String) body.get("nickname"));
        if (body.get("email") != null) user.setEmail((String) body.get("email"));
        if (body.get("phone") != null) user.setPhone((String) body.get("phone"));
        if (body.get("role") != null) user.setRole((String) body.get("role"));
        if (body.get("status") != null) user.setStatus(((Number) body.get("status")).intValue());
        if (body.get("password") != null) user.setPassword(passwordEncoder.encode((String) body.get("password")));
        userMapper.updateById(user);
        return toResponse(user);
    }

    public void delete(Long id) {
        userMapper.deleteById(id);
    }

    private Map<String, Object> toResponse(SysUserDO user) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", user.getId());
        m.put("tenant_id", user.getTenantId());
        m.put("username", user.getUsername());
        m.put("nickname", user.getNickname());
        m.put("email", user.getEmail());
        m.put("phone", user.getPhone());
        m.put("role", user.getRole());
        m.put("status", user.getStatus());
        m.put("last_login_at", user.getLastLoginAt());
        m.put("created_at", user.getCreatedAt());
        return m;
    }
}
