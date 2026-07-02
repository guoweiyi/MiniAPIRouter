package com.miniapi.router.core.protocol;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class UnifiedResponse {
    private String id;
    private String model;
    private String role = "assistant";
    private String content;
    private String reasoningContent;
    private List<Map<String, Object>> contentBlocks;
    private String finishReason;
    private int promptTokens;
    private int completionTokens;
    private int totalTokens;
    private Map<String, Object> raw;
    private String upstreamProtocol;
}
