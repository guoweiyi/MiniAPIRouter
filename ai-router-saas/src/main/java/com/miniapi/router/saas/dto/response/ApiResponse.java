package com.miniapi.router.saas.dto.response;

import lombok.Data;
import lombok.Builder;

@Data
@Builder
public class ApiResponse<T> {
    private int code;
    private String message;
    private T data;
    private String traceId;

    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder().code(0).message("success").data(data).build();
    }

    public static <T> ApiResponse<T> success() {
        return success(null);
    }

    public static <T> ApiResponse<T> error(int code, String message) {
        return ApiResponse.<T>builder().code(code).message(message).build();
    }
}
