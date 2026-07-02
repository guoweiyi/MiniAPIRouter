package com.miniapi.router.core.exception;

public class UpstreamException extends RouterException {
    public UpstreamException(String message) {
        super("UPSTREAM_ERROR", message, 502);
    }
    public UpstreamException(String errorCode, String message, int httpStatus) {
        super(errorCode, message, httpStatus);
    }
}
