package com.miniapi.router.core.routing.strategy;

import com.miniapi.router.core.domain.ApiKeyConfig;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class RoundRobinStrategy implements RouteStrategy {

    private final AtomicInteger counter = new AtomicInteger(0);

    @Override
    public ApiKeyConfig select(List<ApiKeyConfig> candidates) {
        if (candidates == null || candidates.isEmpty()) return null;
        int idx = Math.abs(counter.getAndIncrement()) % candidates.size();
        return candidates.get(idx);
    }

    @Override
    public String name() { return "round_robin"; }
}
