package com.miniapi.router.standalone.controller;

import com.miniapi.router.core.domain.ApiKeyConfig;
import com.miniapi.router.core.domain.IntentConfig;
import com.miniapi.router.core.domain.RouteRule;
import com.miniapi.router.standalone.dto.ApiResponse;
import com.miniapi.router.standalone.service.ConfigService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/config")
public class ConfigController {

    private final ConfigService configService;

    public ConfigController(ConfigService configService) {
        this.configService = configService;
    }

    // ===== API Key Config =====

    @GetMapping("/api-keys")
    public ApiResponse<Object> listKeys(@RequestParam(defaultValue = "1") int page,
                                        @RequestParam(defaultValue = "20") int page_size) {
        return ApiResponse.success(configService.listKeys(page, page_size));
    }

    @GetMapping("/api-keys/{id}")
    public ApiResponse<Object> getKey(@PathVariable Long id) {
        return ApiResponse.success(configService.getKey(id));
    }

    @PostMapping("/api-keys")
    public ApiResponse<Object> createKey(@RequestBody ApiKeyConfig config) {
        return ApiResponse.success(configService.createKey(config));
    }

    @PutMapping("/api-keys/{id}")
    public ApiResponse<Object> updateKey(@PathVariable Long id, @RequestBody ApiKeyConfig config) {
        return ApiResponse.success(configService.updateKey(id, config));
    }

    @DeleteMapping("/api-keys/{id}")
    public ApiResponse<Object> deleteKey(@PathVariable Long id) {
        configService.deleteKey(id);
        return ApiResponse.success();
    }

    @PatchMapping("/api-keys/{id}/status")
    public ApiResponse<Object> updateKeyStatus(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        configService.updateKeyStatus(id, Integer.parseInt(body.get("status").toString()));
        return ApiResponse.success();
    }

    // ===== Route Rule =====

    @GetMapping("/route-rules")
    public ApiResponse<Object> listRules(@RequestParam(defaultValue = "1") int page,
                                         @RequestParam(defaultValue = "20") int page_size) {
        return ApiResponse.success(configService.listRules(page, page_size));
    }

    @GetMapping("/route-rules/{id}")
    public ApiResponse<Object> getRule(@PathVariable Long id) {
        return ApiResponse.success(configService.getRule(id));
    }

    @PostMapping("/route-rules")
    public ApiResponse<Object> createRule(@RequestBody RouteRule rule) {
        return ApiResponse.success(configService.createRule(rule));
    }

    @PutMapping("/route-rules/{id}")
    public ApiResponse<Object> updateRule(@PathVariable Long id, @RequestBody RouteRule rule) {
        return ApiResponse.success(configService.updateRule(id, rule));
    }

    @DeleteMapping("/route-rules/{id}")
    public ApiResponse<Object> deleteRule(@PathVariable Long id) {
        configService.deleteRule(id);
        return ApiResponse.success();
    }

    @PatchMapping("/route-rules/{id}/enabled")
    public ApiResponse<Object> updateRuleEnabled(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        configService.updateRuleEnabled(id, Boolean.TRUE.equals(body.get("enabled")));
        return ApiResponse.success();
    }

    // ===== Intent Config =====

    @GetMapping("/intents")
    public ApiResponse<Object> listIntents() {
        return ApiResponse.success(configService.listIntents());
    }

    @GetMapping("/intents/{id}")
    public ApiResponse<Object> getIntent(@PathVariable Long id) {
        return ApiResponse.success(configService.getIntent(id));
    }

    @PostMapping("/intents")
    public ApiResponse<Object> createIntent(@RequestBody IntentConfig config) {
        return ApiResponse.success(configService.createIntent(config));
    }

    @PutMapping("/intents/{id}")
    public ApiResponse<Object> updateIntent(@PathVariable Long id, @RequestBody IntentConfig config) {
        return ApiResponse.success(configService.updateIntent(id, config));
    }

    @DeleteMapping("/intents/{id}")
    public ApiResponse<Object> deleteIntent(@PathVariable Long id) {
        configService.deleteIntent(id);
        return ApiResponse.success();
    }
}
