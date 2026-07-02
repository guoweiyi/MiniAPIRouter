package com.miniapi.router.saas.event;

import com.miniapi.router.core.domain.RequestLogMeta;
import com.miniapi.router.core.spi.BlobStorage;
import com.miniapi.router.core.spi.LogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class LogPersistConsumer {

    private static final Logger log = LoggerFactory.getLogger(LogPersistConsumer.class);

    private final LogRepository logRepository;
    private final BlobStorage blobStorage;

    public LogPersistConsumer(LogRepository logRepository, BlobStorage blobStorage) {
        this.logRepository = logRepository;
        this.blobStorage = blobStorage;
    }

    @Async
    @EventListener
    public void onLogPersist(LogPersistEvent event) {
        try {
            String promptUrl = event.promptStorageUrl();
            String responseUrl = event.responseStorageUrl();

            if (promptUrl == null && event.promptContent() != null && !event.promptContent().isEmpty()) {
                promptUrl = storeBlob(event.tenantId(), event.traceId(), "prompt", event.promptContent());
            }
            if (responseUrl == null && event.responseContent() != null && !event.responseContent().isEmpty()) {
                responseUrl = storeBlob(event.tenantId(), event.traceId(), "response", event.responseContent());
            }

            RequestLogMeta meta = new RequestLogMeta();
            meta.setTenantId(event.tenantId());
            meta.setUserId(event.userId());
            meta.setTraceId(event.traceId());
            meta.setRequestId(event.requestId());
            meta.setClientIp(event.clientIp());
            meta.setProtocol(event.protocol());
            meta.setModel(event.model());
            meta.setMappedProvider(event.mappedProvider() != null ? event.mappedProvider() : "unknown");
            meta.setApiKeyId(event.apiKeyId());
            meta.setRouteRuleId(event.routeRuleId());
            meta.setIntent(event.intent());
            meta.setPromptTokens(event.promptTokens());
            meta.setCompletionTokens(event.completionTokens());
            meta.setTotalTokens(event.totalTokens());
            meta.setLatencyMs(event.latencyMs());
            meta.setTtftMs(event.ttftMs());
            meta.setStatus(event.status());
            meta.setFallbackCount(event.fallbackCount());
            meta.setErrorCode(event.errorCode());
            meta.setErrorMessage(event.errorMessage());
            meta.setPromptStorageUrl(promptUrl);
            meta.setResponseStorageUrl(responseUrl);
            meta.setCreatedAt(event.createdAt() != null ? event.createdAt() : LocalDateTime.now());
            logRepository.save(meta);
        } catch (Exception e) {
            log.error("[LogPersist] Failed to persist log for trace={}: {}", event.traceId(), e.getMessage());
        }
    }

    private String storeBlob(Long tenantId, String traceId, String type, String content) {
        LocalDateTime now = LocalDateTime.now();
        String path = String.format("tenant_%d/%04d/%02d/%02d/%s/%s.json",
                tenantId, now.getYear(), now.getMonthValue(), now.getDayOfMonth(), type, traceId);
        return blobStorage.store(path, content);
    }
}
