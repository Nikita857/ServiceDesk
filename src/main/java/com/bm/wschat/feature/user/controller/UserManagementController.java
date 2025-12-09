package com.bm.wschat.feature.user.controller;

import com.bm.wschat.feature.auth.mapper.AuthMapper;
import com.bm.wschat.feature.user.dto.response.UserAuthResponse;
import com.bm.wschat.feature.user.model.User;
import com.bm.wschat.feature.user.service.UserManagementService;
import com.bm.wschat.shared.common.ApiResponse;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class UserManagementController {

    private final UserManagementService userManagementService;
    private final AuthMapper authMapper;
    //Разобраться с ролями и сделать нормальную обработку исключений при ограниченных правах

    @PostMapping
    public ResponseEntity<ApiResponse<UserAuthResponse>> createUser(
            @RequestParam @NotNull String username,
            @RequestParam @NotNull String password,
            @RequestParam(required = false) String fio,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) Set<String> roles,
            @RequestParam(defaultValue = "true") Boolean active
    ) {
        User user = userManagementService.createUser(username, password, fio, email, roles, active);
        UserAuthResponse response = new UserAuthResponse(
                user.getId(), user.getFio(), user.getUsername(),
                user.getTelegramId(), user.isSpecialist(), user.getRoles(),
                user.isActive());
        return ResponseEntity.ok(ApiResponse.success("User created successfully", response));
    }

    @PutMapping("/{id}/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @PathVariable Long id,
            @RequestParam @NotNull String newPassword
    ) {
        userManagementService.changePassword(id, newPassword);
        return ResponseEntity.ok(ApiResponse.success("Password changed successfully"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Long id) {
        userManagementService.deleteUser(id);
        return ResponseEntity.ok(ApiResponse.success("User deleted successfully"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserAuthResponse>> getUser(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("User retrieved successfully",
                authMapper.toAuthResponse(userManagementService.findUserById(id))));
    }
}