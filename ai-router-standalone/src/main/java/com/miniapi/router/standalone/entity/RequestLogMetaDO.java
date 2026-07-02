package com.miniapi.router.standalone.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("request_log_meta")
public class RequestLogMetaDO {
    @TableId(type = IdType.AUTO)
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
