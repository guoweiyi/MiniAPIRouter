package com.miniapi.router.core.protocol.converter.openai;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.miniapi.router.core.domain.FallbackEvent;
import com.miniapi.router.core.domain.UsageStats;
import com.miniapi.router.core.protocol.UnifiedStreamChunk;
import com.miniapi.router.core.protocol.converter.StreamConverter;
import com.miniapi.router.core.util.JsonUtils;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class OpenAIStreamConverter implements StreamConverter {

    private static final ObjectMapper SSE_MAPPER = new ObjectMapper()
            .setSerializationInclusion(JsonInclude.Include.ALWAYS);

    @Override
    public String toSseChunk(UnifiedStreamChunk chunk, String inboundProtocol) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", chunk.getId());
        data.put("object", "chat.completion.chunk");
        data.put("created", chunk.getTimestamp() / 1000);
        data.put("model", chunk.getModel());
        data.put("system_fingerprint", "_miniapi_");

        Map<String, Object> delta = new LinkedHashMap<>();
        if (chunk.getDeltaRole() != null) {
            delta.put("role", chunk.getDeltaRole());
        }
        delta.put("content", chunk.getDeltaContent());
        if (chunk.getReasoningContent() != null) {
            delta.put("reasoning_content", chunk.getReasoningContent());
        }
        if (chunk.getToolCalls() != null && !chunk.getToolCalls().isEmpty()) {
            delta.put("tool_calls", chunk.getToolCalls());
        }

        Map<String, Object> choice = new LinkedHashMap<>();
        choice.put("index", chunk.getIndex());
        choice.put("delta", delta);
        choice.put("logprobs", null);
        choice.put("finish_reason", chunk.getFinishReason());
        data.put("choices", List.of(choice));

        return "data: " + sseJson(data) + "\n\n";
    }

    @Override
    public String toDoneMark(String inboundProtocol) {
        return "data: [DONE]\n\n";
    }

    @Override
    public String toUsageSseChunk(UsageStats stats) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", "chatcmpl-" + Long.toHexString(stats.getTimestamp()));
        data.put("object", "chat.completion.chunk");
        data.put("created", stats.getTimestamp() / 1000);
        data.put("model", stats.getModel());
        data.put("system_fingerprint", "_miniapi_");
        data.put("choices", List.of());
        Map<String, Object> usage = new LinkedHashMap<>();
        usage.put("prompt_tokens", stats.getPromptTokens());
        usage.put("completion_tokens", stats.getCompletionTokens());
        usage.put("total_tokens", stats.getTotalTokens());
        data.put("usage", usage);
        return "data: " + sseJson(data) + "\n\n";
    }

    @Override
    public String toFallbackSseChunk(FallbackEvent event) {
        return "";
    }

    @Override
    public String toErrorSseChunk(String errorCode, String message, String traceId) {
        Map<String, Object> data = new LinkedHashMap<>();
        Map<String, Object> error = new LinkedHashMap<>();
        error.put("message", message);
        error.put("type", errorCode.toLowerCase());
        error.put("code", errorCode);
        data.put("error", error);
        return "data: " + sseJson(data) + "\n\n";
    }

    @Override
    public boolean supports(String protocol) {
        return "openai".equalsIgnoreCase(protocol);
    }

    private static String sseJson(Object obj) {
        try {
            return SSE_MAPPER.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return JsonUtils.toJson(obj);
        }
    }
}
