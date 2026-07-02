package com.miniapi.router.saas.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.miniapi.router.saas.entity.SysUserDO;
import com.miniapi.router.saas.entity.TenantDO;
import com.miniapi.router.saas.mapper.SysUserMapper;
import com.miniapi.router.saas.mapper.TenantMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class DataInitializer implements CommandLineRunner {

    private final TenantMapper tenantMapper;
    private final SysUserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Value("${SAAS_ADMIN_DEFAULT_PASSWORD:admin123}")
    private String adminDefaultPassword;

    @Value("${SAAS_DEMO_ADMIN_DEFAULT_PASSWORD:demo123}")
    private String demoAdminDefaultPassword;

    public DataInitializer(TenantMapper tenantMapper, SysUserMapper userMapper, PasswordEncoder passwordEncoder) {
        this.tenantMapper = tenantMapper;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        LambdaQueryWrapper<SysUserDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUserDO::getUsername, "admin");
        if (userMapper.selectCount(wrapper) == 0) {
            SysUserDO admin = new SysUserDO();
            admin.setTenantId(0L);
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode(adminDefaultPassword));
            admin.setNickname("超级管理员");
            admin.setRole("super_admin");
            admin.setStatus(1);
            userMapper.insert(admin);
            log.info("Created super admin: admin / {}", adminDefaultPassword);
        }

        LambdaQueryWrapper<TenantDO> tenantWrapper = new LambdaQueryWrapper<>();
        tenantWrapper.eq(TenantDO::getTenantCode, "demo");
        if (tenantMapper.selectCount(tenantWrapper) == 0) {
            TenantDO tenant = new TenantDO();
            tenant.setTenantCode("demo");
            tenant.setTenantName("Demo Corporation");
            tenant.setPlan("pro");
            tenant.setQuotaLimit(50000000L);
            tenant.setQuotaUsed(0L);
            tenant.setQuotaResetDay(1);
            tenant.setMaxRps(100);
            tenant.setStatus(1);
            tenantMapper.insert(tenant);
            log.info("Created default tenant: demo (id={})", tenant.getId());

            SysUserDO tenantAdmin = new SysUserDO();
            tenantAdmin.setTenantId(tenant.getId());
            tenantAdmin.setUsername("demo_admin");
            tenantAdmin.setPassword(passwordEncoder.encode(demoAdminDefaultPassword));
            tenantAdmin.setNickname("Demo Admin");
            tenantAdmin.setRole("tenant_admin");
            tenantAdmin.setStatus(1);
            userMapper.insert(tenantAdmin);
            log.info("Created tenant admin: demo_admin / {}", demoAdminDefaultPassword);
        }
    }
}
