package com.miniapi.router.core.domain;

import lombok.Data;
import lombok.Builder;

@Data
@Builder
public class UsageStats {
    @Builder.Default
    private String type = "usage_stats";
    private int promptTokens;
    private int completionTokens;
    private int totalTokens;
    private boolean estimated;
    private int latencyMs;
    private int ttftMs;
    private String model;
    private String provider;
    private int fallbackCount;
    private long timestamp;
}
