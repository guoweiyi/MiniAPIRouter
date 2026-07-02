package com.miniapi.router.core.streaming;

import com.fasterxml.jackson.databind.JsonNode;
import com.miniapi.router.core.protocol.UnifiedStreamChunk;
import com.miniapi.router.core.util.JsonUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DeltaJsonParser {

    public static final Map<String, Object> DONE = new HashMap<>();
    static { DONE.put("__done__", true); }

    @SuppressWarnings("unchecked")
    public static Object parseSseLine(String line, String upstreamProtocol, String defaultId, String defaultModel) {
        if (line == null || line.isEmpty()) return null;
        if (line.startsWith("event:")) return null;
        if (!line.startsWith("data:")) return null;

        String data = line.substring(5).trim();
        if ("[DONE]".equals(data)) return DONE;

        try {
            JsonNode node = JsonUtils.parse(data);
            if ("openai".equalsIgnoreCase(upstreamProtocol)) {
                return parseOpenAIChunk(node, defaultId, defaultModel);
            } else {
                return parseAnthropicChunk(node, defaultId, defaultModel);
            }
        } catch (Exception e) {
            return null;
        }
    }

    private static UnifiedStreamChunk parseOpenAIChunk(JsonNode node, String defaultId, String defaultModel) {
        String id = node.path("id").asText(defaultId);
        String model = node.path("model").asText(defaultModel);
        JsonNode choices = node.path("choices");
        if (!choices.isArray() || choices.isEmpty()) return null;

        JsonNode choice = choices.get(0);
        JsonNode delta = choice.path("delta");
        String content = delta.path("content").asText("");
        String role = delta.has("role") ? delta.get("role").asText() : null;
        String reasoningContent = delta.path("reasoning_content").asText("");
        String finishReason = choice.has("finish_reason") && !choice.path("finish_reason").isNull()
                ? choice.get("finish_reason").asText() : null;

        List<Map<String, Object>> toolCalls = null;
        JsonNode tcNode = delta.path("tool_calls");
        if (tcNode.isArray() && tcNode.size() > 0) {
            toolCalls = new java.util.ArrayList<>();
            for (JsonNode tc : tcNode) {
                Map<String, Object> tcMap = new java.util.LinkedHashMap<>();
                tcMap.put("index", tc.path("index").asInt(0));
                if (tc.has("id")) tcMap.put("id", tc.get("id").asText());
                Map<String, Object> fn = new java.util.LinkedHashMap<>();
                JsonNode fnNode = tc.path("function");
                if (fnNode.has("name")) fn.put("name", fnNode.get("name").asText());
                if (fnNode.has("arguments")) fn.put("arguments", fnNode.get("arguments").asText());
                if (!fn.isEmpty()) tcMap.put("function", fn);
                toolCalls.add(tcMap);
            }
        }

        if (content.isEmpty() && role == null && reasoningContent.isEmpty()
                && finishReason == null && toolCalls == null) return null;

        JsonNode usageNode = node.path("usage");
        UnifiedStreamChunk chunk = UnifiedStreamChunk.builder()
                .id(id)
                .model(model)
                .deltaContent(content.isEmpty() ? null : content)
                .deltaRole(role)
                .reasoningContent(reasoningContent.isEmpty() ? null : reasoningContent)
                .toolCalls(toolCalls)
                .finishReason(finishReason)
                .index(0)
                .timestamp(System.currentTimeMillis())
                .build();
        return chunk;
    }

    private static Object parseAnthropicChunk(JsonNode node, String defaultId, String defaultModel) {
        String type = node.path("type").asText("");
        switch (type) {
            case "message_start": {
                JsonNode message = node.path("message");
                String id = message.path("id").asText(defaultId);
                String model = message.path("model").asText(defaultModel);
                return UnifiedStreamChunk.builder()
                        .id(id).model(model).deltaRole("assistant").index(0)
                        .timestamp(System.currentTimeMillis()).build();
            }
            case "content_block_delta": {
                JsonNode delta = node.path("delta");
                String text = delta.path("text").asText("");
                if (text.isEmpty()) return null;
                int index = node.path("index").asInt(0);
                return UnifiedStreamChunk.builder()
                        .id(defaultId).model(defaultModel).deltaContent(text).index(index)
                        .timestamp(System.currentTimeMillis()).build();
            }
            case "message_delta": {
                JsonNode delta = node.path("delta");
                String stopReason = delta.path("stop_reason").asText(null);
                if (stopReason != null) {
                    String mapped = switch (stopReason) {
                        case "end_turn" -> "stop";
                        case "max_tokens" -> "length";
                        case "tool_use" -> "tool_calls";
                        default -> stopReason;
                    };
                    return UnifiedStreamChunk.builder()
                            .id(defaultId).model(defaultModel).finishReason(mapped).index(0)
                            .timestamp(System.currentTimeMillis()).build();
                }
                return null;
            }
            case "message_stop":
                return DONE;
            default:
                return null;
        }
    }
}
