package com.miniapi.router.core.streaming;

import com.miniapi.router.core.domain.FallbackEvent;
import com.miniapi.router.core.domain.UsageStats;
import com.miniapi.router.core.util.JsonUtils;

public class SseEventEmitter {

    private SseEventEmitter() {}

    public static String data(String json) {
        return "data: " + json + "\n\n";
    }

    public static String event(String eventName, String json) {
        return "event: " + eventName + "\ndata: " + json + "\n\n";
    }

    public static String fallbackSignal(FallbackEvent event) {
        return event("fallback_signal", JsonUtils.toJson(event));
    }

    public static String usageStats(UsageStats stats) {
        return event("usage_stats", JsonUtils.toJson(stats));
    }

    public static String streamError(String errorCode, String message, String traceId) {
        return event("error", JsonUtils.toJson(java.util.Map.of(
                "type", "error",
                "error_code", errorCode,
                "message", message,
                "trace_id", traceId != null ? traceId : ""
        )));
    }
}
