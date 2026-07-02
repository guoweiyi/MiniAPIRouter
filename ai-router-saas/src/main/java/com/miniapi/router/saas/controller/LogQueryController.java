package com.miniapi.router.saas.controller;

import com.miniapi.router.saas.dto.response.ApiResponse;
import com.miniapi.router.saas.service.LogQueryService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/tenant/logs")
public class LogQueryController {

    private final LogQueryService logQueryService;

    public LogQueryController(LogQueryService logQueryService) {
        this.logQueryService = logQueryService;
    }

    @GetMapping
    public ApiResponse<Object> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int page_size,
            @RequestParam(required = false) String start_time,
            @RequestParam(required = false) String end_time,
            @RequestParam(required = false) String model,
            @RequestParam(required = false) String provider,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String trace_id) {
        return ApiResponse.success(logQueryService.search(page, page_size, start_time, end_time,
                model, provider, status, keyword, trace_id));
    }

    @GetMapping("/{id}")
    public ApiResponse<Object> detail(@PathVariable Long id) {
        return ApiResponse.success(logQueryService.getDetail(id));
    }
}
