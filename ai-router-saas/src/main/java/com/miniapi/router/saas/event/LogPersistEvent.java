package com.miniapi.router.saas.event;

import java.time.LocalDateTime;
import java.util.Map;

public record LogPersistEvent(
        Long tenantId,
        Long userId,
        String traceId,
        String requestId,
        String clientIp,
        String protocol,
        String model,
        String mappedProvider,
        Long apiKeyId,
        Long routeRuleId,
        String intent,
        int promptTokens,
        int completionTokens,
        int totalTokens,
        int latencyMs,
        int ttftMs,
        int fallbackCount,
        String status,
        String promptStorageUrl,
        String responseStorageUrl,
        String errorCode,
        String errorMessage,
        String promptContent,
        String responseContent,
        LocalDateTime createdAt
) {}
