package com.bm.wschat.feature.user.controller;

import com.bm.wschat.feature.auth.mapper.AuthMapper;
import com.bm.wschat.feature.user.dto.response.UserAuthResponse;
import com.bm.wschat.feature.user.model.User;
import com.bm.wschat.feature.user.service.UserManagementService;
import com.bm.wschat.shared.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "User Management", description = "Управление пользователями системы")
public class UserManagementController {

    private final UserManagementService userManagementService;
    private final AuthMapper authMapper;

    @GetMapping
    @Operation(summary = "Получить список всех пользователей", description = "Возвращает пагинированный список всех пользователей системы.")
    public ResponseEntity<ApiResponse<Page<UserAuthResponse>>> getAllUsers(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @RequestParam(required = false) String search) {
        Page<UserAuthResponse> users = userManagementService.findAllUsers(pageable, search)
                .map(authMapper::toAuthResponse);
        return ResponseEntity.ok(ApiResponse.success(users));
    }

    @PostMapping
    @Operation(summary = "Создать нового пользователя", description = "Создает нового пользователя с указанными данными и ролями.")
    public ResponseEntity<ApiResponse<UserAuthResponse>> createUser(
            @RequestParam @NotNull String username,
            @RequestParam @NotNull String password,
            @RequestParam(required = false) String fio,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) Set<String> roles,
            @RequestParam(defaultValue = "true") Boolean active) {
        User user = userManagementService.createUser(username, password, fio, email, roles, active);
        return ResponseEntity.ok(ApiResponse.success("Пользователь создан",
                authMapper.toAuthResponse(user)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получить пользователя по ID", description = "Возвращает информацию о пользователе по его уникальному идентификатору.")
    public ResponseEntity<ApiResponse<UserAuthResponse>> getUser(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Информация о пользователе",
                authMapper.toAuthResponse(userManagementService.findUserById(id))));
    }

    @PatchMapping("/{id}/fio")
    @Operation(summary = "Изменить ФИО пользователя", description = "Изменяет ФИО для указанного пользователя.")
    public ResponseEntity<ApiResponse<UserAuthResponse>> updateFio(
            @PathVariable Long id,
            @RequestParam @NotNull String fio) {
        User user = userManagementService.updateFio(id, fio);
        return ResponseEntity.ok(ApiResponse.success("ФИО обновлено",
                authMapper.toAuthResponse(user)));
    }

    @PatchMapping("/{id}/roles")
    @Operation(summary = "Изменить роли пользователя", description = "Изменяет набор ролей для указанного пользователя.")
    public ResponseEntity<ApiResponse<UserAuthResponse>> updateRoles(
            @PathVariable Long id,
            @RequestParam @NotNull Set<String> roles) {
        User user = userManagementService.updateRoles(id, roles);
        return ResponseEntity.ok(ApiResponse.success("Роли обновлены",
                authMapper.toAuthResponse(user)));
    }

    @PatchMapping("/{id}/active")
    @Operation(summary = "Включить/отключить пользователя", description = "Изменяет статус активности пользователя.")
    public ResponseEntity<ApiResponse<UserAuthResponse>> toggleActive(
            @PathVariable Long id,
            @RequestParam @NotNull Boolean active) {
        User user = userManagementService.setActive(id, active);
        return ResponseEntity.ok(ApiResponse.success(
                active ? "Пользователь активирован" : "Пользователь деактивирован",
                authMapper.toAuthResponse(user)));
    }

    @PutMapping("/{id}/password")
    @Operation(summary = "Изменить пароль пользователя", description = "Изменяет пароль для указанного пользователя.")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @PathVariable Long id,
            @RequestParam @NotNull String newPassword) {
        userManagementService.changePassword(id, newPassword);
        return ResponseEntity.ok(ApiResponse.success("Пароль изменен"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить пользователя", description = "Удаляет пользователя по его уникальному идентификатору.")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Long id) {
        userManagementService.deleteUser(id);
        return ResponseEntity.ok(ApiResponse.success("Пользователь удалён"));
    }
}
