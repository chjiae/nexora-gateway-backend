package com.nexora.platform.controller;

import com.nexora.platform.dto.*;
import com.nexora.platform.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@RequestBody LoginRequest request) {
        return ApiResponse.success(authService.login(request));
    }

    @GetMapping("/me")
    public ApiResponse<UserInfoResponse> currentUser(@RequestHeader("Authorization") String token) {
        return ApiResponse.success(authService.getCurrentUser(token));
    }

    @GetMapping("/menus")
    public ApiResponse<MenuResponse> menus(@RequestHeader("Authorization") String token) {
        return ApiResponse.success(authService.getMenus(token));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@RequestHeader("Authorization") String token) {
        authService.logout(token);
        return ApiResponse.success(null);
    }
}
