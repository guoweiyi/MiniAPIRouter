package com.miniapi.router.saas.controller;

import com.miniapi.router.saas.dto.response.ApiResponse;
import com.miniapi.router.saas.service.DashboardService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/tenant/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/summary")
    public ApiResponse<Object> summary(
            @RequestParam(required = false) String start_time,
            @RequestParam(required = false) String end_time,
            @RequestParam(required = false) String interval) {
        return ApiResponse.success(dashboardService.summary(start_time, end_time, interval));
    }
}
