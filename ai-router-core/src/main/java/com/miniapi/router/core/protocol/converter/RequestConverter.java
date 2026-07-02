package com.miniapi.router.core.protocol.converter;

import com.miniapi.router.core.protocol.UnifiedRequest;
import java.util.Map;

public interface RequestConverter {
    UnifiedRequest convert(Map<String, Object> rawRequest, String apiKey);
    boolean supports(String protocol);
    Map<String, Object> buildUpstreamRequest(UnifiedRequest request, String upstreamProtocol);
}
