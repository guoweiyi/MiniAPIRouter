package com.miniapi.router.core.protocol;

import lombok.Data;
import lombok.Builder;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class UnifiedStreamChunk {
    private String id;
    private String model;
    private String deltaContent;
    private String deltaRole;
    private String reasoningContent;
    private List<Map<String, Object>> toolCalls;
    private String finishReason;
    private int index;
    private String upstreamProvider;
    private long timestamp;
}
