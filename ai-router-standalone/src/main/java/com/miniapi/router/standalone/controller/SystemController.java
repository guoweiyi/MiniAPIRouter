package com.miniapi.router.standalone.controller;

import com.miniapi.router.core.config.CoreProperties;
import com.miniapi.router.standalone.dto.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/system")
public class SystemController {

    private final CoreProperties properties;

    public SystemController(CoreProperties properties) {
        this.properties = properties;
    }

    @GetMapping("/health")
    public ApiResponse<Object> health() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("status", "UP");
        m.put("version", "1.0.0");
        m.put("mode", "standalone");
        return ApiResponse.success(m);
    }

    @GetMapping("/version")
    public ApiResponse<Object> version() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("version", "1.0.0");
        m.put("mode", "standalone");
        m.put("java_version", System.getProperty("java.version"));
        return ApiResponse.success(m);
    }

    @GetMapping("/info")
    public ApiResponse<Object> info() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("version", "1.0.0");
        m.put("mode", "standalone");
        m.put("auth_token", properties.getAuthToken());
        return ApiResponse.success(m);
    }
}
