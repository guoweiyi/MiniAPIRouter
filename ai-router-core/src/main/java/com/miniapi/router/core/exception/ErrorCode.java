package com.miniapi.router.core.exception;

public enum ErrorCode {
    SUCCESS("SUCCESS", 200, "成功"),
    INVALID_PARAMS("INVALID_PARAMS", 400, "请求参数无效"),
    INVALID_JSON("INVALID_JSON", 400, "JSON 格式错误"),
    MISSING_REQUIRED_FIELD("MISSING_REQUIRED_FIELD", 400, "缺少必填字段"),
    UNAUTHORIZED("UNAUTHORIZED", 401, "未认证"),
    INVALID_API_KEY("INVALID_API_KEY", 401, "API Key 无效"),
    INVALID_TOKEN("INVALID_TOKEN", 401, "JWT 无效"),
    FORBIDDEN("FORBIDDEN", 403, "权限不足"),
    TENANT_DISABLED("TENANT_DISABLED", 403, "租户已禁用"),
    QUOTA_EXCEEDED("QUOTA_EXCEEDED", 403, "配额已耗尽"),
    TENANT_EXPIRED("TENANT_EXPIRED", 403, "租户已过期"),
    RESOURCE_NOT_FOUND("RESOURCE_NOT_FOUND", 404, "资源不存在"),
    DUPLICATE_RESOURCE("DUPLICATE_RESOURCE", 409, "资源重复"),
    RATE_LIMITED("RATE_LIMITED", 429, "请求限流"),
    RATE_LIMITED_GLOBAL("RATE_LIMITED_GLOBAL", 429, "全局限流"),
    UPSTREAM_ERROR("UPSTREAM_ERROR", 502, "上游服务错误"),
    UPSTREAM_TIMEOUT("UPSTREAM_TIMEOUT", 504, "上游超时"),
    UPSTREAM_RATE_LIMITED("UPSTREAM_RATE_LIMITED", 429, "上游限流"),
    UPSTREAM_CONTENT_FILTERED("UPSTREAM_CONTENT_FILTERED", 400, "内容审核拦截"),
    ALL_UPSTREAM_FAILED("ALL_UPSTREAM_FAILED", 502, "所有上游不可用"),
    NO_ROUTE_MATCHED("NO_ROUTE_MATCHED", 404, "无匹配路由规则"),
    NO_AVAILABLE_UPSTREAM("NO_AVAILABLE_UPSTREAM", 503, "无可用上游"),
    STREAM_INTERRUPTED("STREAM_INTERRUPTED", 502, "流式连接中断"),
    INTERNAL_ERROR("INTERNAL_ERROR", 500, "内部错误"),
    DB_ERROR("DB_ERROR", 500, "数据库错误"),
    STORAGE_ERROR("STORAGE_ERROR", 500, "存储错误");

    public final String code;
    public final int httpStatus;
    public final String message;

    ErrorCode(String code, int httpStatus, String message) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.message = message;
    }
}
