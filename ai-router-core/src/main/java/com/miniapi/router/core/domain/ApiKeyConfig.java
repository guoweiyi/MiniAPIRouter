package com.miniapi.router.core.domain;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ApiKeyConfig {
    private Long id;
    private Long tenantId;
    private String name;
    private String provider;
    private String protocol;
    private String apiKey;
    private String apiKeyEnc;
    private String baseUrl;
    private List<String> models;
    private Integer weight;
    private Integer priority;
    private Integer maxConcurrent;
    private Integer qpsLimit;
    private Integer timeoutMs;
    private Integer retryCount;
    private Integer status;
    private String healthStatus;
    private LocalDateTime lastHealthCheckAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public boolean isEnabled() {
        return status != null && status == 1;
    }
}
