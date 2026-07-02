package com.miniapi.router.core.protocol.converter.anthropic;

import com.miniapi.router.core.protocol.UnifiedRequest;
import com.miniapi.router.core.protocol.converter.RequestConverter;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class AnthropicRequestConverter implements RequestConverter {

    @Override
    @SuppressWarnings("unchecked")
    public UnifiedRequest convert(Map<String, Object> rawRequest, String apiKey) {
        UnifiedRequest req = new UnifiedRequest();
        req.setModel((String) rawRequest.get("model"));
        req.setMessages((List<Map<String, Object>>) rawRequest.get("messages"));
        Object system = rawRequest.get("system");
        if (system instanceof String s) {
            req.setSystemPrompt(s);
        }
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
        extra.remove("system");
        extra.remove("temperature");
        extra.remove("max_tokens");
        extra.remove("top_p");
        extra.remove("tools");
        extra.remove("stream");
        req.setExtraParams(extra);
        req.setInboundProtocol("anthropic");
        return req;
    }

    @Override
    public boolean supports(String protocol) {
        return "anthropic".equalsIgnoreCase(protocol);
    }

    @Override
    public Map<String, Object> buildUpstreamRequest(UnifiedRequest request, String upstreamProtocol) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", request.getModel());
        body.put("messages", request.getMessages());
        if (request.getSystemPrompt() != null) body.put("system", request.getSystemPrompt());
        if (request.getMaxTokens() != null) {
            body.put("max_tokens", request.getMaxTokens());
        } else {
            body.put("max_tokens", 4096);
        }
        if (request.getTemperature() != null) body.put("temperature", request.getTemperature());
        if (request.getTopP() != null) body.put("top_p", request.getTopP());
        if (request.getTools() != null) body.put("tools", request.getTools());
        body.put("stream", Boolean.TRUE.equals(request.getStream()));
        if (request.getExtraParams() != null) body.putAll(request.getExtraParams());
        return body;
    }
}
