package com.miniapi.router.core.protocol.converter.anthropic;

import com.miniapi.router.core.protocol.UnifiedStreamChunk;
import com.miniapi.router.core.protocol.converter.StreamConverter;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class AnthropicStreamConverter implements StreamConverter {

    private boolean started = false;

    @Override
    public String toSseChunk(UnifiedStreamChunk chunk, String inboundProtocol) {
        StringBuilder sb = new StringBuilder();
        if (chunk.getDeltaRole() != null && "assistant".equals(chunk.getDeltaRole())) {
            sb.append("event: message_start\n");
            Map<String, Object> msg = new LinkedHashMap<>();
            msg.put("type", "message_start");
            Map<String, Object> message = new LinkedHashMap<>();
            message.put("id", chunk.getId());
            message.put("type", "message");
            message.put("role", "assistant");
            message.put("model", chunk.getModel());
            message.put("content", List.of());
            message.put("stop_reason", null);
            Map<String, Object> usage = new LinkedHashMap<>();
            usage.put("input_tokens", 0);
            usage.put("output_tokens", 0);
            message.put("usage", usage);
            msg.put("message", message);
            sb.append("data: ").append(com.miniapi.router.core.util.JsonUtils.toJson(msg)).append("\n\n");

            sb.append("event: content_block_start\n");
            Map<String, Object> blockStart = new LinkedHashMap<>();
            blockStart.put("type", "content_block_start");
            blockStart.put("index", 0);
            Map<String, Object> contentBlock = new LinkedHashMap<>();
            contentBlock.put("type", "text");
            contentBlock.put("text", "");
            blockStart.put("content_block", contentBlock);
            sb.append("data: ").append(com.miniapi.router.core.util.JsonUtils.toJson(blockStart)).append("\n\n");
        }
        if (chunk.getDeltaContent() != null && !chunk.getDeltaContent().isEmpty()) {
            sb.append("event: content_block_delta\n");
            Map<String, Object> delta = new LinkedHashMap<>();
            delta.put("type", "content_block_delta");
            delta.put("index", chunk.getIndex());
            Map<String, Object> textDelta = new LinkedHashMap<>();
            textDelta.put("type", "text_delta");
            textDelta.put("text", chunk.getDeltaContent());
            delta.put("delta", textDelta);
            sb.append("data: ").append(com.miniapi.router.core.util.JsonUtils.toJson(delta)).append("\n\n");
        }
        if (chunk.getFinishReason() != null) {
            sb.append("event: content_block_stop\n");
            Map<String, Object> stop = new LinkedHashMap<>();
            stop.put("type", "content_block_stop");
            stop.put("index", 0);
            sb.append("data: ").append(com.miniapi.router.core.util.JsonUtils.toJson(stop)).append("\n\n");

            sb.append("event: message_delta\n");
            Map<String, Object> msgDelta = new LinkedHashMap<>();
            msgDelta.put("type", "message_delta");
            Map<String, Object> delta = new LinkedHashMap<>();
            delta.put("stop_reason", mapStop(chunk.getFinishReason()));
            msgDelta.put("delta", delta);
            sb.append("data: ").append(com.miniapi.router.core.util.JsonUtils.toJson(msgDelta)).append("\n\n");
        }
        return sb.toString();
    }

    @Override
    public String toDoneMark(String inboundProtocol) {
        return "event: message_stop\ndata: {\"type\":\"message_stop\"}\n\n";
    }

    private String mapStop(String reason) {
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
