package com.miniapi.router.saas.handler;

import com.miniapi.router.core.exception.RouterException;
import com.miniapi.router.saas.context.TenantContext;
import com.miniapi.router.saas.dto.response.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(AsyncRequestTimeoutException.class)
    public void handleAsyncTimeout(AsyncRequestTimeoutException e) {
        log.warn("[AsyncTimeout] SSE stream request timed out");
    }

    @ExceptionHandler(RouterException.class)
    public ResponseEntity<Map<String, Object>> handleRouterException(RouterException e) {
        HttpStatus status = HttpStatus.resolve(e.getHttpStatus());
        if (status == null) status = HttpStatus.INTERNAL_SERVER_ERROR;
        return ResponseEntity.status(status).body(Map.of(
                "code", e.getHttpStatus(),
                "message", e.getMessage(),
                "error_code", e.getErrorCode(),
                "trace_id", getTraceId()
        ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("Invalid parameters");
        return ResponseEntity.badRequest().body(Map.of(
                "code", 400,
                "message", msg,
                "error_code", "INVALID_PARAMS",
                "trace_id", getTraceId()
        ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "code", 500,
                "message", e.getMessage() != null ? e.getMessage() : "Internal error",
                "error_code", "INTERNAL_ERROR",
                "trace_id", getTraceId()
        ));
    }

    private String getTraceId() {
        String tid = TenantContext.getTraceId();
        return tid != null ? tid : "";
    }
}
