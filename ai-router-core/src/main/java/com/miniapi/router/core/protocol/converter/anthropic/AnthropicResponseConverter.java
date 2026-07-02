package com.miniapi.router.core.protocol.converter.anthropic;

import com.miniapi.router.core.protocol.UnifiedResponse;
import com.miniapi.router.core.protocol.converter.ResponseConverter;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class AnthropicResponseConverter implements ResponseConverter {

    @Override
    public Map<String, Object> convert(UnifiedResponse response, String inboundProtocol) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", response.getId() != null ? response.getId() : "msg_" + UUID.randomUUID().toString().replace("-", "").substring(0, 24));
        result.put("type", "message");
        result.put("role", "assistant");
        result.put("model", response.getModel());

        String text = response.getContent() != null ? response.getContent() : "";
        List<Map<String, Object>> contentBlocks;
        if (response.getContentBlocks() != null && !response.getContentBlocks().isEmpty()) {
            contentBlocks = response.getContentBlocks();
        } else {
            Map<String, Object> block = new LinkedHashMap<>();
            block.put("type", "text");
            block.put("text", text);
            contentBlocks = List.of(block);
        }
        result.put("content", contentBlocks);
        result.put("stop_reason", mapFinishReason(response.getFinishReason()));

        Map<String, Object> usage = new LinkedHashMap<>();
        usage.put("input_tokens", response.getPromptTokens());
        usage.put("output_tokens", response.getCompletionTokens());
        result.put("usage", usage);
        return result;
    }

    @Override
    public Map<String, Object> convertError(String errorCode, String message, String inboundProtocol) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("type", "error");
        Map<String, Object> error = new LinkedHashMap<>();
        error.put("type", errorCode.toLowerCase());
        error.put("message", message);
        result.put("error", error);
        return result;
    }

    private String mapFinishReason(String reason) {
        if (reason == null) return "end_turn";
        return switch (reason) {
            case "stop" -> "end_turn";
            case "length" -> "max_tokens";
            case "tool_calls" -> "tool_use";
            default -> reason;
        };
    }

    @Override
    public boolean supports(String protocol) {
        return "anthropic".equalsIgnoreCase(protocol);
    }
}
