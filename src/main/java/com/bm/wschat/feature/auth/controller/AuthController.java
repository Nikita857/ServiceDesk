package com.bm.wschat.feature.auth.controller;

import com.bm.wschat.feature.auth.dto.request.RefreshTokenRequest;
import com.bm.wschat.feature.auth.dto.request.AuthRequest;
import com.bm.wschat.feature.auth.dto.response.AuthResponse;
import com.bm.wschat.feature.auth.service.AuthService;
import com.bm.wschat.feature.auth.service.RefreshTokenService;
import com.bm.wschat.feature.user.model.User;
import com.bm.wschat.shared.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Аутентификация и управление токенами")
public class AuthController {

    private final AuthService authService;
    private final RefreshTokenService refreshTokenService;

    @PostMapping("/login")
    @Operation(summary = "Аутентификация пользователя", description = "Принимает логин и пароль, возвращает access и refresh токены.")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody AuthRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success("Login successful", authService.login(request)));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Обновление токена", description = "Принимает refresh токен и возвращает новую пару access и refresh токенов.")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success("Token refreshed successfully", authService.refreshToken(request)));
    }

    @PostMapping("/logout")
    @Operation(summary = "Выход из системы", description = "Осуществляет выход из системы, удаляя refresh токен пользователя.")
    public ResponseEntity<ApiResponse<Void>> logout(@AuthenticationPrincipal User user) {
        refreshTokenService.deleteByUserId(user.getId());
        return ResponseEntity.ok(
                ApiResponse.success("Successfully logged out"));
    }
}