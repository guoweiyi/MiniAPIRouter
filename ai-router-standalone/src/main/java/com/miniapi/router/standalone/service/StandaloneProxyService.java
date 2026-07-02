package com.miniapi.router.standalone.service;

import com.miniapi.router.core.domain.*;
import com.miniapi.router.core.exception.RouterException;
import com.miniapi.router.core.protocol.ProtocolRegistry;
import com.miniapi.router.core.protocol.UnifiedRequest;
import com.miniapi.router.core.protocol.UnifiedResponse;
import com.miniapi.router.core.protocol.converter.RequestConverter;
import com.miniapi.router.core.protocol.converter.ResponseConverter;
import com.miniapi.router.core.routing.RoutePipeline;
import com.miniapi.router.core.spi.BlobStorage;
import com.miniapi.router.core.spi.LogRepository;
import com.miniapi.router.core.streaming.StreamProxy;
import com.miniapi.router.core.util.JsonUtils;
import com.miniapi.router.core.util.TraceUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;

@Service
public class StandaloneProxyService {

    private static final Logger log = LoggerFactory.getLogger(StandaloneProxyService.class);
    private static final Long TENANT_ID = 1L;

    private final RoutePipeline routePipeline;
    private final StreamProxy streamProxy;
    private final ProtocolRegistry protocolRegistry;
    private final LogRepository logRepository;
    private final BlobStorage blobStorage;

    public StandaloneProxyService(RoutePipeline routePipeline, StreamProxy streamProxy,
                                  ProtocolRegistry protocolRegistry, LogRepository logRepository,
                                  BlobStorage blobStorage) {
        this.routePipeline = routePipeline;
        this.streamProxy = streamProxy;
        this.protocolRegistry = protocolRegistry;
        this.logRepository = logRepository;
        this.blobStorage = blobStorage;
    }

    public record ProxyRequest(
            String inboundProtocol,
            Map<String, Object> rawBody,
            String apiKey,
            HttpServletRequest httpRequest
    ) {}

    public Object proxy(ProxyRequest proxyRequest) {
        String traceId = TraceUtils.newTraceId();
        String requestId = TraceUtils.newRequestId();
        long startTime = System.currentTimeMillis();

        Map<String, Object> body = proxyRequest.rawBody();
        String model = (String) body.get("model");
        if (model == null) {
            throw new RouterException("MISSING_REQUIRED_FIELD", "model is required", 400);
        }
        String inboundProtocol = proxyRequest.inboundProtocol();
        boolean stream = Boolean.TRUE.equals(body.get("stream"));

        RequestConverter requestConverter = protocolRegistry.getRequestConverter(inboundProtocol);
        UnifiedRequest unifiedReq = requestConverter.convert(body, proxyRequest.apiKey());
        unifiedReq.setInboundProtocol(inboundProtocol);

        String clientIp = getClientIp(proxyRequest.httpRequest());

        RouteContext routeCtx = RouteContext.builder()
                .tenantId(TENANT_ID)
                .traceId(traceId)
                .requestId(requestId)
                .clientIp(clientIp)
                .inboundProtocol(inboundProtocol)
                .model(model)
                .messages(unifiedReq.getMessages())
                .systemPrompt(unifiedReq.getSystemPrompt())
                .parameters(unifiedReq.getExtraParams())
                .stream(stream)
                .build();

        if (stream) {
            return handleStream(routeCtx, unifiedReq, inboundProtocol, model,
                    requestId, traceId, clientIp, startTime);
        } else {
            RouteResult routeResult = routePipeline.route(routeCtx);
            logRequestSummary(traceId, model, routeResult, inboundProtocol, false);
            ApiKeyConfig selectedKey = routeResult.getSelectedKey();
            String upstreamProtocol = selectedKey.getProtocol() != null ? selectedKey.getProtocol() : "openai";
            unifiedReq.setUpstreamProtocol(upstreamProtocol);
            unifiedReq.setModel(routeResult.resolveUpstreamModel(model));

            String upstreamPath = getUpstreamPath(upstreamProtocol);
            Map<String, Object> upstreamBody = requestConverter.buildUpstreamRequest(unifiedReq, upstreamProtocol);

            return handleNonStream(routeResult, inboundProtocol, upstreamPath, upstreamBody,
                    model, requestId, traceId, clientIp, startTime);
        }
    }

    private Object handleNonStream(RouteResult routeResult, String inboundProtocol,
                                   String upstreamPath, Map<String, Object> upstreamBody, String model,
                                   String requestId, String traceId, String clientIp, long startTime) {
        ResponseConverter responseConverter = protocolRegistry.getResponseConverter(inboundProtocol);
        try {
            StreamProxy.ProxyResult result = streamProxy.proxyNonStream(routeResult, inboundProtocol,
                    upstreamPath, upstreamBody, model, requestId);
            UnifiedResponse response = result.response();

            String promptStorageUrl = storeBlob(traceId, "prompt", JsonUtils.toJson(upstreamBody.get("messages")));
            String responseStorageUrl = storeBlob(traceId, "response", response.getContent());

            int latencyMs = (int) (System.currentTimeMillis() - startTime);
            saveLog(traceId, requestId, clientIp, inboundProtocol, model,
                    result.mappedProvider(), result.apiKeyId(), routeResult.getMatchedRule().getId(),
                    response.getPromptTokens(), response.getCompletionTokens(), response.getTotalTokens(),
                    latencyMs, 0, 0, "success", promptStorageUrl, responseStorageUrl, null, null,
                    routeResult.getIntent());

            return responseConverter.convert(response, inboundProtocol);
        } catch (RouterException e) {
            int latencyMs = (int) (System.currentTimeMillis() - startTime);
            saveLog(traceId, requestId, clientIp, inboundProtocol, model,
                    routeResult.getSelectedKey().getProvider(), null,
                    routeResult.getMatchedRule() != null ? routeResult.getMatchedRule().getId() : null,
                    0, 0, 0, latencyMs, 0, 0, "failed", null, null, e.getErrorCode(), e.getMessage(),
                    routeResult.getIntent());
            return responseConverter.convertError(e.getErrorCode(), e.getMessage(), inboundProtocol);
        } catch (Exception e) {
            int latencyMs = (int) (System.currentTimeMillis() - startTime);
            saveLog(traceId, requestId, clientIp, inboundProtocol, model,
                    routeResult.getSelectedKey().getProvider(), null, null,
                    0, 0, 0, latencyMs, 0, 0, "failed", null, null,
                    "INTERNAL_ERROR", e.getMessage(), routeResult.getIntent());
            return responseConverter.convertError("INTERNAL_ERROR", e.getMessage(), inboundProtocol);
        }
    }

    private StreamingResponseBody handleStream(RouteContext routeCtx, UnifiedRequest unifiedReq,
                                                String inboundProtocol, String model,
                                                String requestId, String traceId, String clientIp, long startTime) {
        StreamingResponseBody responseBody = outputStream -> {
            java.util.concurrent.atomic.AtomicReference<RouteResult> routeRef = new java.util.concurrent.atomic.AtomicReference<>();
            java.util.concurrent.atomic.AtomicReference<Throwable> errRef = new java.util.concurrent.atomic.AtomicReference<>();

            Thread routeThread = Thread.ofVirtual().name("route-eval-" + traceId).start(() -> {
                try {
                    routeRef.set(routePipeline.route(routeCtx));
                } catch (Throwable e) {
                    errRef.set(e);
                }
            });

            byte[] keepalive = ": \n\n".getBytes(StandardCharsets.UTF_8);
            while (routeThread.isAlive()) {
                try {
                    outputStream.write(keepalive);
                    outputStream.flush();
                } catch (IOException ignored) {
                    routeThread.interrupt();
                    return;
                }
                try {
                    routeThread.join(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }

            Throwable routeError = errRef.get();
            if (routeError != null || routeRef.get() == null) {
                String errMsg = routeError != null ? routeError.getMessage() : "routing failed";
                log.error("[Stream] Route failed: {}", errMsg);
                try {
                    outputStream.write(("data: {\"error\":{\"message\":\"" + errMsg + "\"}}\n\n").getBytes(StandardCharsets.UTF_8));
                    outputStream.write("data: [DONE]\n\n".getBytes(StandardCharsets.UTF_8));
                    outputStream.flush();
                } catch (IOException ignored) {}
                return;
            }

            RouteResult routeResult = routeRef.get();
            logRequestSummary(traceId, model, routeResult, inboundProtocol, true);
            ApiKeyConfig selectedKey = routeResult.getSelectedKey();
            String upstreamProtocol = selectedKey.getProtocol() != null ? selectedKey.getProtocol() : "openai";
            unifiedReq.setUpstreamProtocol(upstreamProtocol);
            unifiedReq.setModel(routeResult.resolveUpstreamModel(model));

            String upstreamPath = getUpstreamPath(upstreamProtocol);
            Map<String, Object> upstreamBody = protocolRegistry.getRequestConverter(inboundProtocol)
                    .buildUpstreamRequest(unifiedReq, upstreamProtocol);

            StreamProxy.StreamProxyContext ctx = new StreamProxy.StreamProxyContext(
                    routeResult, inboundProtocol, upstreamPath, upstreamBody, model, requestId, traceId, null);
            StreamProxy.StreamContext result = streamProxy.proxyStream(ctx, outputStream);

            int latencyMs = (int) (System.currentTimeMillis() - startTime);
            UsageStats stats = result.stats();
            String promptStorageUrl = storeBlob(traceId, "prompt", JsonUtils.toJson(upstreamBody.get("messages")));
            String responseStorageUrl = storeBlob(traceId, "response", result.content());

            String status = stats != null && stats.getFallbackCount() > 0 ? "fallback" : "success";
            saveLog(traceId, requestId, clientIp, inboundProtocol, model,
                    result.mappedProvider(), result.apiKeyId(),
                    routeResult.getMatchedRule().getId(),
                    stats != null ? stats.getPromptTokens() : 0,
                    stats != null ? stats.getCompletionTokens() : 0,
                    stats != null ? stats.getTotalTokens() : 0,
                    latencyMs, stats != null ? stats.getTtftMs() : 0,
                    stats != null ? stats.getFallbackCount() : 0,
                    status, promptStorageUrl, responseStorageUrl, null, null,
                    routeResult.getIntent());
        };
        return responseBody;
    }

    private void logRequestSummary(String traceId, String model, RouteResult routeResult,
                                    String protocol, boolean stream) {
        ApiKeyConfig selected = routeResult.getSelectedKey();
        log.info("[Request] trace={} model={} protocol={} stream={} intent={} → key_id={} name={} provider={} strategy={}",
                traceId, model, protocol, stream,
                routeResult.getIntent() != null ? routeResult.getIntent() : "-",
                selected.getId(), selected.getName(), selected.getProvider(), routeResult.getStrategy());
    }

    private String getUpstreamPath(String protocol) {
        return "anthropic".equalsIgnoreCase(protocol) ? "/v1/messages" : "/v1/chat/completions";
    }

    private String storeBlob(String traceId, String type, String content) {
        if (content == null || content.isEmpty()) return null;
        LocalDateTime now = LocalDateTime.now();
        String path = String.format("tenant_1/%04d/%02d/%02d/%s/%s.json",
                now.getYear(), now.getMonthValue(), now.getDayOfMonth(), type, traceId);
        return blobStorage.store(path, content);
    }

    private void saveLog(String traceId, String requestId, String clientIp,
                         String protocol, String model, String mappedProvider, Long apiKeyId, Long routeRuleId,
                         int promptTokens, int completionTokens, int totalTokens, int latencyMs, int ttftMs,
                         int fallbackCount, String status, String promptUrl, String responseUrl,
                         String errorCode, String errorMessage, String intent) {
        try {
            RequestLogMeta meta = new RequestLogMeta();
            meta.setTenantId(TENANT_ID);
            meta.setTraceId(traceId);
            meta.setRequestId(requestId);
            meta.setClientIp(clientIp);
            meta.setProtocol(protocol);
            meta.setModel(model);
            meta.setMappedProvider(mappedProvider != null ? mappedProvider : "unknown");
            meta.setApiKeyId(apiKeyId);
            meta.setRouteRuleId(routeRuleId);
            meta.setIntent(intent);
            meta.setPromptTokens(promptTokens);
            meta.setCompletionTokens(completionTokens);
            meta.setTotalTokens(totalTokens);
            meta.setLatencyMs(latencyMs);
            meta.setTtftMs(ttftMs);
            meta.setStatus(status);
            meta.setFallbackCount(fallbackCount);
            meta.setErrorCode(errorCode);
            meta.setErrorMessage(errorMessage);
            meta.setPromptStorageUrl(promptUrl);
            meta.setResponseStorageUrl(responseUrl);
            meta.setCreatedAt(LocalDateTime.now());
            logRepository.save(meta);
        } catch (Exception e) {
            log.error("[Log] Failed to save log for trace={}: {}", traceId, e.getMessage());
        }
    }

    private String getClientIp(HttpServletRequest request) {
        if (request == null) return null;
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isEmpty()) return xff.split(",")[0].trim();
        return request.getRemoteAddr();
    }
}
