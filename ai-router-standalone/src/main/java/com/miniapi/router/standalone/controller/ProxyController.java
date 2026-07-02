package com.miniapi.router.standalone.controller;

import com.miniapi.router.core.exception.RouterException;
import com.miniapi.router.standalone.service.StandaloneProxyService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.util.Map;

@RestController
public class ProxyController {

    private final StandaloneProxyService proxyService;

    public ProxyController(StandaloneProxyService proxyService) {
        this.proxyService = proxyService;
    }

    @PostMapping("/v1/chat/completions")
    public Object openaiProxy(@RequestBody Map<String, Object> body, HttpServletRequest request, HttpServletResponse response) {
        Object result = proxyService.proxy(new StandaloneProxyService.ProxyRequest("openai", body, extractApiKey(request), request));
        if (result instanceof StreamingResponseBody srb) {
            response.setContentType(MediaType.TEXT_EVENT_STREAM_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Cache-Control", "no-cache");
            response.setHeader("Connection", "keep-alive");
            return srb;
        }
        return ResponseEntity.ok(result);
    }

    @PostMapping("/v1/messages")
    public Object anthropicProxy(@RequestBody Map<String, Object> body, HttpServletRequest request, HttpServletResponse response) {
        Object result = proxyService.proxy(new StandaloneProxyService.ProxyRequest("anthropic", body, extractApiKey(request), request));
        if (result instanceof StreamingResponseBody srb) {
            response.setContentType(MediaType.TEXT_EVENT_STREAM_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Cache-Control", "no-cache");
            response.setHeader("Connection", "keep-alive");
            return srb;
        }
        return ResponseEntity.ok(result);
    }

    private String extractApiKey(HttpServletRequest request) {
        String auth = request.getHeader("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            return auth.substring(7);
        }
        return request.getHeader("x-api-key");
    }
}
