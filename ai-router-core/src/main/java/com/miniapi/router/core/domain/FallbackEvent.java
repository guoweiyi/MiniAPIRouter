package com.miniapi.router.core.domain;

import lombok.Data;
import lombok.Builder;

@Data
@Builder
public class FallbackEvent {
    @Builder.Default
    private String type = "fallback_signal";
    private String reason;
    private String failedProvider;
    private Long failedKeyId;
    private String fallbackProvider;
    private Long fallbackKeyId;
    private Integer fallbackIndex;
    private Integer maxFallback;
    private Integer partialContentLength;
    private long timestamp;
}
