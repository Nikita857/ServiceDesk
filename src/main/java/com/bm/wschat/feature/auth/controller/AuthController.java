package com.bm.wschat.feature.auth.controller;

import com.bm.wschat.feature.auth.dto.request.RefreshTokenRequest;
import com.bm.wschat.feature.auth.dto.request.AuthRequest;
import com.bm.wschat.feature.auth.dto.response.AuthResponse;
import com.bm.wschat.feature.auth.service.AuthService;
import com.bm.wschat.feature.auth.service.RefreshTokenService;
import com.bm.wschat.feature.user.model.User;
import com.bm.wschat.shared.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final RefreshTokenService refreshTokenService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody AuthRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success("Login successful", authService.login(request)));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success("Token refreshed successfully", authService.refreshToken(request)));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@AuthenticationPrincipal User user) {
        refreshTokenService.deleteByUserId(user.getId());
        return ResponseEntity.ok(
                ApiResponse.success("Successfully logged out"));
    }
}