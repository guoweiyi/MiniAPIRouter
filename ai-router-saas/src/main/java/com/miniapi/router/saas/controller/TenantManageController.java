package com.miniapi.router.saas.controller;

import com.miniapi.router.saas.dto.response.ApiResponse;
import com.miniapi.router.saas.dto.response.PageResult;
import com.miniapi.router.saas.dto.request.TenantCreateRequest;
import com.miniapi.router.saas.service.TenantService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/tenants")
public class TenantManageController {

    private final TenantService tenantService;

    public TenantManageController(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    @PostMapping
    public ApiResponse<Object> create(@RequestBody TenantCreateRequest req) {
        return ApiResponse.success(tenantService.create(req));
    }

    @GetMapping
    public ApiResponse<PageResult<Map<String, Object>>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int page_size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String plan) {
        return ApiResponse.success(tenantService.list(page, page_size, keyword, status, plan));
    }

    @PutMapping("/{id}")
    public ApiResponse<Object> update(@PathVariable Long id, @RequestBody TenantCreateRequest req) {
        return ApiResponse.success(tenantService.update(id, req));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Object> delete(@PathVariable Long id) {
        tenantService.delete(id);
        return ApiResponse.success();
    }
}
