package com.miniapi.router.core.protocol.converter;

import com.miniapi.router.core.domain.FallbackEvent;
import com.miniapi.router.core.domain.UsageStats;
import com.miniapi.router.core.protocol.UnifiedStreamChunk;
import com.miniapi.router.core.streaming.SseEventEmitter;

public interface StreamConverter {
    String toSseChunk(UnifiedStreamChunk chunk, String inboundProtocol);
    String toDoneMark(String inboundProtocol);
    boolean supports(String protocol);

    default String toUsageSseChunk(UsageStats stats) {
        return SseEventEmitter.usageStats(stats);
    }

    default String toFallbackSseChunk(FallbackEvent event) {
        return SseEventEmitter.fallbackSignal(event);
    }

    default String toErrorSseChunk(String errorCode, String message, String traceId) {
        return SseEventEmitter.streamError(errorCode, message, traceId);
    }
}
