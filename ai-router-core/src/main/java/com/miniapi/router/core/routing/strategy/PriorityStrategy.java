package com.miniapi.router.core.routing.strategy;

import com.miniapi.router.core.domain.ApiKeyConfig;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Component
public class PriorityStrategy implements RouteStrategy {

    @Override
    public ApiKeyConfig select(List<ApiKeyConfig> candidates) {
        if (candidates == null || candidates.isEmpty()) return null;
        return candidates.stream()
                .min(Comparator.comparingInt(k -> k.getPriority() != null ? k.getPriority() : 0))
                .orElse(candidates.get(0));
    }

    @Override
    public String name() { return "priority"; }
}
