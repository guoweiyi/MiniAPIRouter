package com.miniapi.router.core.domain;

import lombok.Data;
import lombok.Builder;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class RouteContext {
    private Long tenantId;
    private String traceId;
    private String requestId;
    private String clientIp;
    private String inboundProtocol;
    private String model;
    private List<Map<String, Object>> messages;
    private String systemPrompt;
    private Map<String, Object> parameters;
    private boolean stream;
    private RouteRule matchedRule;
    private List<ApiKeyConfig> candidates;
    private String intent;
}
