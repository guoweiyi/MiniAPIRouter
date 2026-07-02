package com.miniapi.router.core.protocol;

import com.miniapi.router.core.protocol.converter.*;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class ProtocolRegistry {

    private final List<RequestConverter> requestConverters;
    private final List<ResponseConverter> responseConverters;
    private final List<StreamConverter> streamConverters;

    public ProtocolRegistry(List<RequestConverter> requestConverters,
                            List<ResponseConverter> responseConverters,
                            List<StreamConverter> streamConverters) {
        this.requestConverters = requestConverters;
        this.responseConverters = responseConverters;
        this.streamConverters = streamConverters;
    }

    public RequestConverter getRequestConverter(String protocol) {
        return requestConverters.stream()
                .filter(c -> c.supports(protocol))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported protocol: " + protocol));
    }

    public ResponseConverter getResponseConverter(String protocol) {
        return responseConverters.stream()
                .filter(c -> c.supports(protocol))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported protocol: " + protocol));
    }

    public StreamConverter getStreamConverter(String protocol) {
        return streamConverters.stream()
                .filter(c -> c.supports(protocol))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported protocol: " + protocol));
    }

    public static String inferProtocol(String provider) {
        return switch (provider.toLowerCase()) {
            case "anthropic" -> "anthropic";
            default -> "openai";
        };
    }

    public static Map<String, Object> buildOpenAIRequestFromMessages(String model, List<Map<String, Object>> messages, boolean stream) {
        return Map.of("model", model, "messages", messages, "stream", stream);
    }
}
