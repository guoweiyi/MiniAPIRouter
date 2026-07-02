package com.miniapi.router.standalone.spiimpl;

import com.miniapi.router.core.spi.TenantContextProvider;
import org.springframework.stereotype.Component;

@Component
public class StandaloneTenantContextProvider implements TenantContextProvider {

    @Override
    public Long getTenantId() {
        return 1L;
    }

    @Override
    public Long getUserId() {
        return 0L;
    }
}
