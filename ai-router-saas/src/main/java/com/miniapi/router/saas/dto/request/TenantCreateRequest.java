package com.miniapi.router.saas.dto.request;

import lombok.Data;

@Data
public class TenantCreateRequest {
    private String tenantCode;
    private String tenantName;
    private String plan = "free";
    private Long quotaLimit = 1000000L;
    private Integer maxRps = 10;
    private String expiresAt;
}
