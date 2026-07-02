package com.miniapi.router.saas.controller;

import com.miniapi.router.core.exception.RouterException;
import com.miniapi.router.core.util.TraceUtils;
import com.miniapi.router.saas.context.TenantContext;
import com.miniapi.router.saas.security.ApiKeyAuthService;
import com.miniapi.router.saas.service.ProxyService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.util.Map;

@RestController
public class OpenAIProxyController {

    private final ProxyService proxyService;
    private final ApiKeyAuthService apiKeyAuthService;

    public OpenAIProxyController(ProxyService proxyService, ApiKeyAuthService apiKeyAuthService) {
        this.proxyService = proxyService;
        this.apiKeyAuthService = apiKeyAuthService;
    }

    @PostMapping("/v1/chat/completions")
    public Object proxy(@RequestBody Map<String, Object> body, HttpServletRequest request, HttpServletResponse response) {
        authenticate(body, request);
        Object result = proxyService.proxy(new ProxyService.ProxyRequest("openai", body, apiKeyAuthService.extractApiKey(request), request));
        if (result instanceof StreamingResponseBody srb) {
            response.setContentType(MediaType.TEXT_EVENT_STREAM_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Cache-Control", "no-cache");
            response.setHeader("Connection", "keep-alive");
            return srb;
        }
        return ResponseEntity.ok(result);
    }

    private void authenticate(Map<String, Object> body, HttpServletRequest request) {
        String apiKey = apiKeyAuthService.extractApiKey(request);
        if (apiKey == null) {
            throw new RouterException("UNAUTHORIZED", "Missing API key", 401);
        }
        ApiKeyAuthService.AuthResult authResult = apiKeyAuthService.authenticate(apiKey);
        if (!authResult.success()) {
            throw new RouterException(authResult.errorCode(), authResult.errorMessage(),
                    authResult.errorCode().startsWith("TENANT") ? 403 : 401);
        }
        TenantContext.setTenantId(authResult.tenantId());
        TenantContext.setTraceId(TraceUtils.newTraceId());
    }
}
