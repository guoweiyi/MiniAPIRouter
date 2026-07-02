package com.miniapi.router.core.exception;

public class RateLimitExceededException extends RouterException {
    public RateLimitExceededException(String message) {
        super("RATE_LIMITED", message, 429);
    }
}
