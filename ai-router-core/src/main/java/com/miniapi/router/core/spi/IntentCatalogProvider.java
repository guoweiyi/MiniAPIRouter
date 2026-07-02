package com.miniapi.router.core.spi;

import com.miniapi.router.core.domain.IntentConfig;
import java.util.List;

public interface IntentCatalogProvider {
    List<IntentConfig> findAll(Long tenantId);
    IntentConfig findByLabel(Long tenantId, String label);
}
