package com.miniapi.router.saas.dto.request;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class RouteRuleRequest {
    private String ruleName;
    private String matchType = "model";
    private String matchPattern;
    private List<Long> targetKeyIds;
    private String strategy = "weight";
    private String intentModel;
    private Map<String, Map<String, Integer>> intentWeights;
    private Boolean fallbackEnabled = true;
    private Integer maxFallback = 2;
    private Integer priority = 0;
    private String description;
}
