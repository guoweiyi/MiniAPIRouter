package com.miniapi.router.core.spi;

public interface TenantContextProvider {
    Long getTenantId();
    Long getUserId();
}
