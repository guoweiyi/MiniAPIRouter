package com.miniapi.router.core.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "miniapi.router")
public class CoreProperties {
    private String cryptoSecret = "";
    private String blobStoragePath = "./data/logs";
    private String proxyBaseUrl = "";
    private String authToken = "sk-miniapi-standalone";
}
