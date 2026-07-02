package com.miniapi.router.core.exception;

public class AllUpstreamFailedException extends RouterException {
    public AllUpstreamFailedException(String message) {
        super("ALL_UPSTREAM_FAILED", message, 502);
    }
}
