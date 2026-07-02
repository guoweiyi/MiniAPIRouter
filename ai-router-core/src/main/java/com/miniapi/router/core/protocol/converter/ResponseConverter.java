package com.miniapi.router.core.protocol.converter;

import com.miniapi.router.core.protocol.UnifiedResponse;
import java.util.Map;

public interface ResponseConverter {
    Map<String, Object> convert(UnifiedResponse response, String inboundProtocol);
    boolean supports(String protocol);
    Map<String, Object> convertError(String errorCode, String message, String inboundProtocol);
}
