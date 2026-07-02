package com.miniapi.router.standalone.handler;

import com.miniapi.router.core.exception.RouterException;
import com.miniapi.router.standalone.dto.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(RouterException.class)
    public ResponseEntity<ApiResponse<Object>> handleRouterException(RouterException e) {
        int httpStatus = e.getHttpStatus();
        return ResponseEntity.status(httpStatus)
                .body(ApiResponse.error(e.getErrorCode() != null ? mapErrorCode(e.getErrorCode()) : 500, e.getMessage()));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleNoResourceFound(NoResourceFoundException e) {
        return ResponseEntity.status(404).body(ApiResponse.error(404, "Not found"));
    }

    @ExceptionHandler(AsyncRequestTimeoutException.class)
    public void handleAsyncTimeout(AsyncRequestTimeoutException e) {
        log.warn("[AsyncTimeout] SSE stream request timed out");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleException(Exception e) {
        log.error("[Unhandled] {}", e.getMessage(), e);
        return ResponseEntity.status(500)
                .body(ApiResponse.error(500, e.getMessage() != null ? e.getMessage() : "Internal error"));
    }

    private int mapErrorCode(String errorCode) {
        return switch (errorCode) {
            case "UNAUTHORIZED", "INVALID_API_KEY", "INVALID_TOKEN" -> 401;
            case "FORBIDDEN", "TENANT_DISABLED", "TENANT_EXPIRED", "QUOTA_EXCEEDED" -> 403;
            case "RESOURCE_NOT_FOUND", "NO_ROUTE_MATCHED" -> 404;
            case "RATE_LIMITED" -> 429;
            case "MISSING_REQUIRED_FIELD", "INVALID_PARAMS", "INVALID_JSON" -> 400;
            default -> 500;
        };
    }
}
