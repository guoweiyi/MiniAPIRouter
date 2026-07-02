package com.miniapi.router.core.routing.strategy;

import com.miniapi.router.core.domain.ApiKeyConfig;
import java.util.List;

public interface RouteStrategy {
    ApiKeyConfig select(List<ApiKeyConfig> candidates);
    String name();
}
