package com.miniapi.router.saas.controller;

import com.miniapi.router.saas.dto.request.ApiKeyConfigRequest;
import com.miniapi.router.saas.dto.response.ApiResponse;
import com.miniapi.router.saas.dto.response.PageResult;
import com.miniapi.router.saas.service.ApiKeyConfigService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/tenant/api-keys")
public class ApiKeyConfigController {

    private final ApiKeyConfigService apiService;

    public ApiKeyConfigController(ApiKeyConfigService apiService) {
        this.apiService = apiService;
    }

    @PostMapping
    public ApiResponse<Object> create(@Valid @RequestBody ApiKeyConfigRequest req) {
        return ApiResponse.success(apiService.create(req));
    }

    @GetMapping
    public ApiResponse<PageResult<Map<String, Object>>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int page_size,
            @RequestParam(required = false) String provider,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String health_status) {
        return ApiResponse.success(apiService.list(page, page_size, provider, status, health_status));
    }

    @PutMapping("/{id}")
    public ApiResponse<Object> update(@PathVariable Long id, @RequestBody ApiKeyConfigRequest req) {
        return ApiResponse.success(apiService.update(id, req));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Object> delete(@PathVariable Long id) {
        apiService.delete(id);
        return ApiResponse.success();
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<Object> updateStatus(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        apiService.updateStatus(id, Boolean.TRUE.equals(body.get("enabled")));
        return ApiResponse.success();
    }

    @PostMapping("/{id}/health-check")
    public ApiResponse<Object> healthCheck(@PathVariable Long id) {
        return ApiResponse.success(apiService.healthCheck(id));
    }
}
