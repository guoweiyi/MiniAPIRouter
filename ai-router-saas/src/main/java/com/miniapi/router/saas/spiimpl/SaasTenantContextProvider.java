package com.miniapi.router.saas.spiimpl;

import com.miniapi.router.core.spi.TenantContextProvider;
import com.miniapi.router.saas.context.TenantContext;
import org.springframework.stereotype.Component;

@Component
public class SaasTenantContextProvider implements TenantContextProvider {

    @Override
    public Long getTenantId() {
        Long id = TenantContext.getTenantId();
        return id != null ? id : 1L;
    }

    @Override
    public Long getUserId() {
        return TenantContext.getUserId();
    }
}
