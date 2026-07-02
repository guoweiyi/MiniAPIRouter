package com.miniapi.router.saas.controller;

import com.miniapi.router.saas.dto.request.LoginRequest;
import com.miniapi.router.saas.dto.response.ApiResponse;
import com.miniapi.router.saas.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ApiResponse<Object> login(@Valid @RequestBody LoginRequest req) {
        return ApiResponse.success(authService.login(req.getUsername(), req.getPassword(), req.getTenantCode()));
    }
}
