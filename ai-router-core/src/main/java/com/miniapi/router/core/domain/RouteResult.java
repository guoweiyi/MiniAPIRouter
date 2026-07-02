package com.miniapi.router.core.domain;

import lombok.Data;
import lombok.Builder;
import java.util.List;

@Data
@Builder
public class RouteResult {
    private ApiKeyConfig selectedKey;
    private RouteRule matchedRule;
    private List<ApiKeyConfig> fallbackChain;
    private String strategy;
    private String intent;

    public boolean hasFallback() {
        return fallbackChain != null && !fallbackChain.isEmpty();
    }

    public String resolveUpstreamModel(String inboundModel) {
        if (selectedKey == null || selectedKey.getModels() == null || selectedKey.getModels().isEmpty()) {
            return inboundModel;
        }
        if (inboundModel != null && selectedKey.getModels().contains(inboundModel)) {
            return inboundModel;
        }
        return selectedKey.getModels().get(0);
    }
}
