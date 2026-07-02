package com.miniapi.router.standalone.controller;

import com.miniapi.router.standalone.dto.ApiResponse;
import com.miniapi.router.standalone.service.LogQueryService;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/logs")
public class LogController {

    private final LogQueryService logQueryService;

    public LogController(LogQueryService logQueryService) {
        this.logQueryService = logQueryService;
    }

    @GetMapping
    public ApiResponse<Object> search(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int page_size,
            @RequestParam(required = false) String model,
            @RequestParam(required = false) String provider,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String trace_id,
            @RequestParam(required = false) String start_time,
            @RequestParam(required = false) String end_time) {
        Map<String, Object> query = new HashMap<>();
        if (model != null) query.put("model", model);
        if (provider != null) query.put("provider", provider);
        if (status != null) query.put("status", status);
        if (trace_id != null) query.put("traceId", trace_id);
        if (start_time != null) query.put("startTime", start_time);
        if (end_time != null) query.put("endTime", end_time);
        return ApiResponse.success(logQueryService.search(query, page, page_size));
    }

    @GetMapping("/{id}")
    public ApiResponse<Object> getDetail(@PathVariable Long id) {
        return ApiResponse.success(logQueryService.getDetail(id));
    }

    @GetMapping("/dashboard")
    public ApiResponse<Object> dashboard(
            @RequestParam(required = false) String start_time,
            @RequestParam(required = false) String end_time,
            @RequestParam(required = false) String interval) {
        return ApiResponse.success(logQueryService.dashboardSummary(start_time, end_time, interval));
    }
}
