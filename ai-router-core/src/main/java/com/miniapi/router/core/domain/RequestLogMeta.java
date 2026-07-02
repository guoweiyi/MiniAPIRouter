package com.miniapi.router.core.domain;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class RequestLogMeta {
    private Long id;
    private Long tenantId;
    private Long userId;
    private String traceId;
    private String requestId;
    private String clientIp;
    private String protocol;
    private String model;
    private String mappedProvider;
    private Long apiKeyId;
    private Long routeRuleId;
    private String intent;
    private Integer promptTokens;
    private Integer completionTokens;
    private Integer totalTokens;
    private Integer latencyMs;
    private Integer ttftMs;
    private String status;
    private Integer fallbackCount;
    private String errorCode;
    private String errorMessage;
    private String promptStorageUrl;
    private String responseStorageUrl;
    private LocalDateTime createdAt;
}
