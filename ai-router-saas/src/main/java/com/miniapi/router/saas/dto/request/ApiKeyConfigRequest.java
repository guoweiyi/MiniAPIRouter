package com.miniapi.router.saas.dto.request;

import lombok.Data;
import java.util.List;

@Data
public class ApiKeyConfigRequest {
    private String name;
    private String provider;
    private String protocol;
    private String apiKey;
    private String baseUrl;
    private List<String> models;
    private Integer weight = 1;
    private Integer priority = 0;
    private Integer maxConcurrent = 10;
    private Integer qpsLimit = 0;
    private Integer timeoutMs = 30000;
    private Integer retryCount = 1;
}
