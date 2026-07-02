package com.miniapi.router.saas.controller;

import com.miniapi.router.saas.dto.response.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/system")
public class SystemController {

    @GetMapping("/health")
    public ApiResponse<Object> health() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "UP");
        result.put("database", "UP");
        result.put("redis", "UP");
        result.put("timestamp", System.currentTimeMillis());
        return ApiResponse.success(result);
    }

    @GetMapping("/version")
    public ApiResponse<Object> version() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("name", "MiniAPIRouter SaaS");
        result.put("version", "1.0.0");
        result.put("java_version", System.getProperty("java.version"));
        return ApiResponse.success(result);
    }

    @GetMapping("/config")
    public ApiResponse<Object> config() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("mode", "saas");
        result.put("features", Map.of("multi_tenant", true, "elasticsearch", false, "minio", false));
        return ApiResponse.success(result);
    }
}
