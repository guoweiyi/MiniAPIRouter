package com.miniapi.router.core.protocol;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class UnifiedRequest {
    private String model;
    private List<Map<String, Object>> messages;
    private String systemPrompt;
    private Double temperature;
    private Integer maxTokens;
    private Double topP;
    private List<Map<String, Object>> tools;
    private Boolean stream;
    private Map<String, Object> extraParams;
    private String inboundProtocol;
    private String upstreamProtocol;
}
