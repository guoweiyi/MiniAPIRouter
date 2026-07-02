package com.miniapi.router.standalone.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(int code, String message, T data, String traceId) {

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(0, "success", data, null);
    }

    public static <T> ApiResponse<T> success() {
        return new ApiResponse<>(0, "success", null, null);
    }

    public static <T> ApiResponse<T> error(int code, String message) {
        return new ApiResponse<>(code, message, null, null);
    }
}
