package com.miniapi.router.core.domain;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
public class RouteRule {
    private Long id;
    private Long tenantId;
    private String ruleName;
    private String matchType;
    private String matchPattern;
    private List<Long> targetKeyIds;
    private String strategy;
    private String intentModel;
    private Map<String, Map<String, Integer>> intentWeights;
    private Boolean fallbackEnabled;
    private Integer maxFallback;
    private Integer priority;
    private Boolean enabled;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
