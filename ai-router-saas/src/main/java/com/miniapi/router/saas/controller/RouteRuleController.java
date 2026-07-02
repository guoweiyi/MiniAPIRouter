package com.miniapi.router.saas.controller;

import com.miniapi.router.saas.dto.request.RouteRuleRequest;
import com.miniapi.router.saas.dto.response.ApiResponse;
import com.miniapi.router.saas.dto.response.PageResult;
import com.miniapi.router.saas.service.RouteRuleService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/tenant/route-rules")
public class RouteRuleController {

    private final RouteRuleService routeRuleService;

    public RouteRuleController(RouteRuleService routeRuleService) {
        this.routeRuleService = routeRuleService;
    }

    @PostMapping
    public ApiResponse<Object> create(@Valid @RequestBody RouteRuleRequest req) {
        return ApiResponse.success(routeRuleService.create(req));
    }

    @GetMapping
    public ApiResponse<PageResult<Map<String, Object>>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int page_size) {
        return ApiResponse.success(routeRuleService.list(page, page_size));
    }

    @GetMapping("/{id}")
    public ApiResponse<Object> findById(@PathVariable Long id) {
        return ApiResponse.success(routeRuleService.findById(id));
    }

    @PutMapping("/{id}")
    public ApiResponse<Object> update(@PathVariable Long id, @RequestBody RouteRuleRequest req) {
        return ApiResponse.success(routeRuleService.update(id, req));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Object> delete(@PathVariable Long id) {
        routeRuleService.delete(id);
        return ApiResponse.success();
    }

    @PatchMapping("/{id}/enabled")
    public ApiResponse<Object> updateEnabled(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        routeRuleService.updateEnabled(id, Boolean.TRUE.equals(body.get("enabled")));
        return ApiResponse.success();
    }
}
