package com.bm.wschat.feature.user.controller;

import com.bm.wschat.feature.user.dto.request.ChangePasswordRequest;
import com.bm.wschat.feature.user.dto.request.UpdateProfileRequest;
import com.bm.wschat.feature.user.dto.request.UpdateTelegramRequest;
import com.bm.wschat.feature.user.dto.response.ProfileResponse;
import com.bm.wschat.feature.user.model.User;
import com.bm.wschat.feature.user.service.ProfileService;
import com.bm.wschat.shared.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.AccessDeniedException;

/**
 * Контроллер для работы с личным кабинетом пользователя.
 */
@RestController
@RequestMapping("/api/v1/profile")
@RequiredArgsConstructor
@Tag(name = "Profile", description = "Личный кабинет: профиль, аватар, пароль, Telegram")
public class ProfileController {

    private final ProfileService profileService;

    /**
     * Получить профиль текущего пользователя.
     */
    @GetMapping
    @Operation(summary = "Получить профиль", description = "Возвращает профиль текущего пользователя. Для специалистов включает среднюю оценку.")
    public ResponseEntity<ApiResponse<ProfileResponse>> getProfile(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success(
                profileService.getProfile(user.getId())));
    }

    /**
     * Обновить профиль (ФИО, email).
     */
    @PatchMapping
    @Operation(summary = "Обновить профиль", description = "Обновляет ФИО и/или email текущего пользователя.")
    public ResponseEntity<ApiResponse<ProfileResponse>> updateProfile(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Профиль обновлён",
                profileService.updateProfile(user.getId(), request.fio(), request.email())));
    }

    /**
     * Загрузить аватар.
     */
    @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Загрузить аватар", description = "Загружает аватар пользователя. Макс. размер — 5 МБ. Форматы: JPEG, PNG, GIF, WebP.")
    public ResponseEntity<ApiResponse<String>> uploadAvatar(
            @AuthenticationPrincipal User user,
            @RequestParam("file") MultipartFile file) {
        String avatarUrl = profileService.uploadAvatar(user.getId(), file);
        return ResponseEntity.ok(ApiResponse.success("Аватар загружен", avatarUrl));
    }

    /**
     * Удалить аватар.
     */
    @DeleteMapping("/avatar")
    @Operation(summary = "Удалить аватар", description = "Удаляет аватар текущего пользователя.")
    public ResponseEntity<ApiResponse<Void>> deleteAvatar(
            @AuthenticationPrincipal User user) {
        profileService.deleteAvatar(user.getId());
        return ResponseEntity.ok(ApiResponse.success("Аватар удалён", null));
    }

    /**
     * Сменить пароль.
     */
    @PutMapping("/password")
    @Operation(summary = "Сменить пароль", description = "Меняет пароль текущего пользователя. Требуется текущий пароль.")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody ChangePasswordRequest request) throws AccessDeniedException {
        profileService.changePassword(user, request.oldPassword(), request.newPassword());
        return ResponseEntity.ok(ApiResponse.success("Пароль изменён", null));
    }

    /**
     * Обновить Telegram ID.
     */
    @PutMapping("/telegram")
    @Operation(summary = "Привязать Telegram", description = "Привязывает или обновляет Telegram ID текущего пользователя.")
    public ResponseEntity<ApiResponse<Void>> updateTelegramId(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody UpdateTelegramRequest request) {
        profileService.updateTelegramId(user.getId(), request.telegramId());
        return ResponseEntity.ok(ApiResponse.success("Telegram ID обновлён", null));
    }
}
