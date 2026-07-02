package com.miniapi.router.saas.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.miniapi.router.core.domain.*;
import com.miniapi.router.core.exception.RouterException;
import com.miniapi.router.core.protocol.UnifiedRequest;
import com.miniapi.router.core.protocol.UnifiedResponse;
import com.miniapi.router.core.protocol.ProtocolRegistry;
import com.miniapi.router.core.protocol.converter.RequestConverter;
import com.miniapi.router.core.protocol.converter.ResponseConverter;
import com.miniapi.router.core.routing.RoutePipeline;
import com.miniapi.router.core.spi.*;
import com.miniapi.router.core.streaming.StreamProxy;
import com.miniapi.router.core.streaming.SseEventEmitter;
import com.miniapi.router.core.util.JsonUtils;
import com.miniapi.router.core.util.TraceUtils;
import com.miniapi.router.saas.context.TenantContext;
import com.miniapi.router.saas.entity.TenantDO;
import com.miniapi.router.saas.event.LogPersistEvent;
import com.miniapi.router.saas.mapper.TenantMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Executors;

@Service
public class ProxyService {

    private static final Logger log = LoggerFactory.getLogger(ProxyService.class);

    private final RoutePipeline routePipeline;
    private final StreamProxy streamProxy;
    private final ProtocolRegistry protocolRegistry;
    private final ApiKeyConfigRepository keyRepository;
    private final TenantMapper tenantMapper;
    private final RateLimiter rateLimiter;
    private final EventPublisher eventPublisher;

    public ProxyService(RoutePipeline routePipeline, StreamProxy streamProxy,
                        ProtocolRegistry protocolRegistry,
                        ApiKeyConfigRepository keyRepository,
                        TenantMapper tenantMapper, RateLimiter rateLimiter,
                        EventPublisher eventPublisher) {
        this.routePipeline = routePipeline;
        this.streamProxy = streamProxy;
        this.protocolRegistry = protocolRegistry;
        this.keyRepository = keyRepository;
        this.tenantMapper = tenantMapper;
        this.rateLimiter = rateLimiter;
        this.eventPublisher = eventPublisher;
    }

    public record ProxyRequest(
            String inboundProtocol,
            Map<String, Object> rawBody,
            String apiKey,
            HttpServletRequest httpRequest
    ) {}

    public Object proxy(ProxyRequest proxyRequest) {
        String traceId = TraceUtils.newTraceId();
        TenantContext.setTraceId(traceId);
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

        Long tenantId = TenantContext.getTenantId();
        String clientIp = getClientIp(proxyRequest.httpRequest());

        checkQuota(tenantId);

        RouteContext routeCtx = RouteContext.builder()
                .tenantId(tenantId)
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

        RouteResult routeResult = routePipeline.route(routeCtx);
        ApiKeyConfig selectedKey = routeResult.getSelectedKey();
        String upstreamProtocol = selectedKey.getProtocol() != null ? selectedKey.getProtocol() : "openai";
        unifiedReq.setUpstreamProtocol(upstreamProtocol);
        unifiedReq.setModel(routeResult.resolveUpstreamModel(model));

        String upstreamPath = getUpstreamPath(upstreamProtocol);
        Map<String, Object> upstreamBody = requestConverter.buildUpstreamRequest(unifiedReq, upstreamProtocol);

        if (stream) {
            return handleStream(proxyRequest, routeResult, inboundProtocol, upstreamPath, upstreamBody,
                    model, requestId, traceId, tenantId, clientIp, startTime);
        } else {
            return handleNonStream(proxyRequest, routeResult, inboundProtocol, upstreamPath, upstreamBody,
                    model, requestId, traceId, tenantId, clientIp, startTime);
        }
    }

    private Object handleNonStream(ProxyRequest proxyRequest, RouteResult routeResult, String inboundProtocol,
                                   String upstreamPath, Map<String, Object> upstreamBody, String model,
                                   String requestId, String traceId, Long tenantId, String clientIp, long startTime) {
        ResponseConverter responseConverter = protocolRegistry.getResponseConverter(inboundProtocol);
        try {
            StreamProxy.ProxyResult result = streamProxy.proxyNonStream(routeResult, inboundProtocol,
                    upstreamPath, upstreamBody, model, requestId);
            UnifiedResponse response = result.response();

            int latencyMs = (int) (System.currentTimeMillis() - startTime);
            int totalTokens = response.getTotalTokens();
            String promptContent = JsonUtils.toJson(upstreamBody.get("messages"));
            String responseContent = response.getContent();

            publishLog(tenantId, traceId, requestId, clientIp, inboundProtocol, model,
                    result.mappedProvider(), result.apiKeyId(), routeResult.getMatchedRule().getId(),
                    response.getPromptTokens(), response.getCompletionTokens(), totalTokens,
                    latencyMs, 0, 0, "success", promptContent, responseContent, null, null,
                    routeResult.getIntent());

            deductQuota(tenantId, totalTokens);

            return responseConverter.convert(response, inboundProtocol);
        } catch (RouterException e) {
            int latencyMs = (int) (System.currentTimeMillis() - startTime);
            publishLog(tenantId, traceId, requestId, clientIp, inboundProtocol, model,
                    routeResult.getSelectedKey().getProvider(), null,
                    routeResult.getMatchedRule() != null ? routeResult.getMatchedRule().getId() : null,
                    0, 0, 0, latencyMs, 0, 0, "failed", null, null, e.getErrorCode(), e.getMessage(),
                    routeResult.getIntent());
            return responseConverter.convertError(e.getErrorCode(), e.getMessage(), inboundProtocol);
        } catch (Exception e) {
            int latencyMs = (int) (System.currentTimeMillis() - startTime);
            publishLog(tenantId, traceId, requestId, clientIp, inboundProtocol, model,
                    routeResult.getSelectedKey().getProvider(), null, null,
                    0, 0, 0, latencyMs, 0, 0, "failed", null, null,
                    "INTERNAL_ERROR", e.getMessage(), routeResult.getIntent());
            return responseConverter.convertError("INTERNAL_ERROR", e.getMessage(), inboundProtocol);
        }
    }

    private StreamingResponseBody handleStream(ProxyRequest proxyRequest, RouteResult routeResult, String inboundProtocol,
                                                String upstreamPath, Map<String, Object> upstreamBody, String model,
                                                String requestId, String traceId, Long tenantId, String clientIp, long startTime) {
        StreamingResponseBody responseBody = outputStream -> {
            StreamProxy.StreamProxyContext ctx = new StreamProxy.StreamProxyContext(
                    routeResult, inboundProtocol, upstreamPath, upstreamBody, model, requestId, traceId, null);
            StreamProxy.StreamContext result = streamProxy.proxyStream(ctx, outputStream);

            int latencyMs = (int) (System.currentTimeMillis() - startTime);
            UsageStats stats = result.stats();
            String promptContent = JsonUtils.toJson(upstreamBody.get("messages"));
            String responseContent = result.content();

            String status = stats != null && stats.getFallbackCount() > 0 ? "fallback" : "success";
            publishLog(tenantId, traceId, requestId, clientIp, inboundProtocol, model,
                    result.mappedProvider(), result.apiKeyId(),
                    routeResult.getMatchedRule().getId(),
                    stats != null ? stats.getPromptTokens() : 0,
                    stats != null ? stats.getCompletionTokens() : 0,
                    stats != null ? stats.getTotalTokens() : 0,
                    latencyMs, stats != null ? stats.getTtftMs() : 0,
                    stats != null ? stats.getFallbackCount() : 0,
                    status, promptContent, responseContent, null, null,
                    routeResult.getIntent());

            if (stats != null && stats.getTotalTokens() > 0 && "success".equals(status)) {
                deductQuota(tenantId, stats.getTotalTokens());
            }
        };
        return responseBody;
    }

    private String getUpstreamPath(String protocol) {
        return "anthropic".equalsIgnoreCase(protocol) ? "/v1/messages" : "/v1/chat/completions";
    }

    private void publishLog(Long tenantId, String traceId, String requestId, String clientIp,
                            String protocol, String model, String mappedProvider, Long apiKeyId, Long routeRuleId,
                            int promptTokens, int completionTokens, int totalTokens, int latencyMs, int ttftMs,
                            int fallbackCount, String status, String promptContent, String responseContent,
                            String errorCode, String errorMessage, String intent) {
        try {
            LogPersistEvent event = new LogPersistEvent(
                    tenantId, TenantContext.getUserId(), traceId, requestId, clientIp,
                    protocol, model, mappedProvider, apiKeyId, routeRuleId, intent,
                    promptTokens, completionTokens, totalTokens, latencyMs, ttftMs,
                    fallbackCount, status, null, null, errorCode, errorMessage,
                    promptContent, responseContent, LocalDateTime.now()
            );
            eventPublisher.publishLogEvent(event);
        } catch (Exception e) {
            log.warn("[LogPublish] Failed to publish log event for trace={}: {}", traceId, e.getMessage());
        }
    }

    private void checkQuota(Long tenantId) {
        if (tenantId == null || tenantId == 0) return;
        TenantDO tenant = tenantMapper.selectById(tenantId);
        if (tenant == null) return;
        if (tenant.getStatus() != null && tenant.getStatus() == 0) {
            throw new RouterException("TENANT_DISABLED", "租户已禁用", 403);
        }
        if (tenant.getExpiresAt() != null && tenant.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RouterException("TENANT_EXPIRED", "租户已过期", 403);
        }
        if (tenant.getQuotaLimit() != null && tenant.getQuotaLimit() > 0
                && tenant.getQuotaUsed() != null && tenant.getQuotaUsed() >= tenant.getQuotaLimit()) {
            throw new RouterException("QUOTA_EXCEEDED",
                    "配额已耗尽，已使用 " + tenant.getQuotaUsed() + " / " + tenant.getQuotaLimit() + " Token", 403);
        }
        if (tenant.getMaxRps() != null && tenant.getMaxRps() > 0) {
            String rateKey = "tenant:" + tenantId;
            if (!rateLimiter.tryAcquire(rateKey, tenant.getMaxRps(), 1)) {
                throw new RouterException("RATE_LIMITED",
                        "请求频率超出限制 (max_rps=" + tenant.getMaxRps() + ")", 429);
            }
        }
    }

    private void deductQuota(Long tenantId, long totalTokens) {
        if (tenantId == null || tenantId == 0 || totalTokens <= 0) return;
        try {
            tenantMapper.addQuotaUsed(tenantId, totalTokens);
        } catch (Exception e) {
            log.warn("[Quota] Failed to deduct quota for tenant={}: {}", tenantId, e.getMessage());
        }
    }

    private String getClientIp(HttpServletRequest request) {
        if (request == null) return null;
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isEmpty()) return xff.split(",")[0].trim();
        return request.getRemoteAddr();
    }
}
