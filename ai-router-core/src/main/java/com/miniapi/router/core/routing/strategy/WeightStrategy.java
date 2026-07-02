package com.miniapi.router.core.routing.strategy;

import com.miniapi.router.core.domain.ApiKeyConfig;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class WeightStrategy implements RouteStrategy {

    @Override
    public ApiKeyConfig select(List<ApiKeyConfig> candidates) {
        if (candidates == null || candidates.isEmpty()) return null;
        int totalWeight = candidates.stream().mapToInt(k -> k.getWeight() != null ? k.getWeight() : 1).sum();
        if (totalWeight <= 0) return candidates.get(0);
        int r = ThreadLocalRandom.current().nextInt(totalWeight);
        int cumulative = 0;
        for (ApiKeyConfig c : candidates) {
            cumulative += (c.getWeight() != null ? c.getWeight() : 1);
            if (r < cumulative) return c;
        }
        return candidates.get(candidates.size() - 1);
    }

    @Override
    public String name() { return "weight"; }
}
