package com.miniapi.router.core.protocol.converter.openai;

import com.miniapi.router.core.protocol.ReasoningContentCache;
import com.miniapi.router.core.protocol.UnifiedRequest;
import com.miniapi.router.core.protocol.converter.RequestConverter;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class OpenAIRequestConverter implements RequestConverter {

    private final ReasoningContentCache reasoningCache;

    public OpenAIRequestConverter(ReasoningContentCache reasoningCache) {
        this.reasoningCache = reasoningCache;
    }

    @Override
    @SuppressWarnings("unchecked")
    public UnifiedRequest convert(Map<String, Object> rawRequest, String apiKey) {
        UnifiedRequest req = new UnifiedRequest();
        req.setModel((String) rawRequest.get("model"));
        req.setMessages((List<Map<String, Object>>) rawRequest.get("messages"));
        if (rawRequest.get("temperature") != null) {
            req.setTemperature(((Number) rawRequest.get("temperature")).doubleValue());
        }
        if (rawRequest.get("max_tokens") != null) {
            req.setMaxTokens(((Number) rawRequest.get("max_tokens")).intValue());
        }
        if (rawRequest.get("top_p") != null) {
            req.setTopP(((Number) rawRequest.get("top_p")).doubleValue());
        }
        req.setTools((List<Map<String, Object>>) rawRequest.get("tools"));
        req.setStream(Boolean.TRUE.equals(rawRequest.get("stream")));
        Map<String, Object> extra = new LinkedHashMap<>(rawRequest);
        extra.remove("model");
        extra.remove("messages");
        extra.remove("temperature");
        extra.remove("max_tokens");
        extra.remove("top_p");
        extra.remove("tools");
        extra.remove("stream");
        req.setExtraParams(extra);
        req.setInboundProtocol("openai");
        return req;
    }

    @Override
    public boolean supports(String protocol) {
        return "openai".equalsIgnoreCase(protocol);
    }

    @Override
    public Map<String, Object> buildUpstreamRequest(UnifiedRequest request, String upstreamProtocol) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", request.getModel());
        body.put("messages", injectReasoningContent(request.getMessages()));
        if (request.getTemperature() != null) body.put("temperature", request.getTemperature());
        if (request.getMaxTokens() != null) body.put("max_tokens", request.getMaxTokens());
        if (request.getTopP() != null) body.put("top_p", request.getTopP());
        if (request.getTools() != null) body.put("tools", request.getTools());
        body.put("stream", Boolean.TRUE.equals(request.getStream()));
        if (request.getExtraParams() != null) body.putAll(request.getExtraParams());
        return body;
    }

    private List<Map<String, Object>> injectReasoningContent(List<Map<String, Object>> messages) {
        if (messages == null) return null;
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> msg : messages) {
            if ("assistant".equals(msg.get("role")) && !msg.containsKey("reasoning_content")) {
                Object contentObj = msg.get("content");
                String contentKey = contentObj instanceof String s ? s : null;
                String reasoning = reasoningCache.lookup(contentKey);
                if (reasoning != null) {
                    Map<String, Object> msgCopy = new LinkedHashMap<>(msg);
                    msgCopy.put("reasoning_content", reasoning);
                    result.add(msgCopy);
                    continue;
                }
            }
            result.add(msg);
        }
        return result;
    }
}
