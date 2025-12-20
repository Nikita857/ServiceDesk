package com.bm.wschat.feature.user.controller;

import com.bm.wschat.feature.user.dto.request.UpdateStatusRequest;
import com.bm.wschat.feature.user.dto.response.UserSearchResponse;
import com.bm.wschat.feature.user.dto.response.UserStatusResponse;
import com.bm.wschat.feature.user.model.User;
import com.bm.wschat.feature.user.model.UserActivityStatus;
import com.bm.wschat.feature.user.model.UserActivityStatusEntity;
import com.bm.wschat.feature.user.repository.UserRepository;
import com.bm.wschat.feature.user.service.UserActivityStatusService;
import com.bm.wschat.shared.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Контроллер для управления пользователями и их статусами.
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "Поиск, управление пользователями и статусами активности")
public class UserController {

        private final UserRepository userRepository;
        private final UserActivityStatusService userActivityStatusService;

        /**
         * Поиск пользователей по ФИО или username.
         */
        @GetMapping("/search")
        @Operation(summary = "Поиск пользователей", description = "Поиск пользователей по ФИО или username. " +
                        "Текущий пользователь исключается из результатов.")
        public ResponseEntity<ApiResponse<Page<UserSearchResponse>>> searchUsers(
                        @RequestParam("q") String query,
                        @PageableDefault(size = 20, sort = "fio", direction = Sort.Direction.ASC) Pageable pageable,
                        @AuthenticationPrincipal User currentUser) {

                if (query == null || query.trim().length() < 2) {
                        return ResponseEntity.badRequest()
                                        .body(ApiResponse.error("Запрос должен содержать минимум 2 символа"));
                }

                Page<UserSearchResponse> results = userRepository
                                .searchByFioOrUsername(query.trim(), currentUser.getId(), pageable)
                                .map(user -> new UserSearchResponse(
                                                user.getId(),
                                                user.getUsername(),
                                                user.getFio()));

                return ResponseEntity.ok(ApiResponse.success(results));
        }

        /**
         * Получить текущий статус активности пользователя.
         */
        @GetMapping("/status")
        @Operation(summary = "Получить статус активности", description = "Возвращает текущий статус активности специалиста с временем последнего обновления.")
        public ResponseEntity<ApiResponse<UserStatusResponse>> getStatus(
                        @AuthenticationPrincipal User user) {

                UserActivityStatusEntity entity = userActivityStatusService.getStatusEntity(user.getId());

                UserStatusResponse response;
                if (entity != null) {
                        response = new UserStatusResponse(
                                        entity.getStatus(),
                                        entity.getStatus().isAvailableForAssignment(),
                                        entity.getUpdatedAt());
                } else {
                        // Если записи нет - пользователь OFFLINE
                        response = new UserStatusResponse(
                                        UserActivityStatus.OFFLINE,
                                        false,
                                        null);
                }

                return ResponseEntity.ok(ApiResponse.success(response));
        }

        /**
         * Получить статус активности другого пользователя по ID.
         * Доступно только специалистам и администраторам.
         */
        @GetMapping("/{userId}/status")
        @PreAuthorize("hasAnyRole('SYSADMIN', '1CSUPPORT', 'DEV1C', 'DEVELOPER', 'ADMIN')")
        @Operation(summary = "Получить статус пользователя по ID", description = "Возвращает статус активности указанного пользователя. "
                        +
                        "Доступно только специалистам для проверки перед назначением тикета.")
        public ResponseEntity<ApiResponse<UserStatusResponse>> getUserStatus(
                        @PathVariable Long userId) {

                UserActivityStatusEntity entity = userActivityStatusService.getStatusEntity(userId);

                UserStatusResponse response;
                if (entity != null) {
                        response = new UserStatusResponse(
                                        entity.getStatus(),
                                        entity.getStatus().isAvailableForAssignment(),
                                        entity.getUpdatedAt());
                } else {
                        response = new UserStatusResponse(
                                        UserActivityStatus.OFFLINE,
                                        false,
                                        null);
                }

                return ResponseEntity.ok(ApiResponse.success(response));
        }

        /**
         * Изменить статус активности специалиста.
         * Доступно только специалистам.
         */
        @PatchMapping("/status")
        @PreAuthorize("hasAnyRole('SYSADMIN', '1CSUPPORT', 'DEV1C', 'DEVELOPER', 'ADMIN')")
        @Operation(summary = "Изменить статус активности", description = "Обновляет статус активности специалиста. " +
                        "Статусы UNAVAILABLE, OFFLINE, TECHNICAL_ISSUE блокируют назначение тикетов. " +
                        "Статус BUSY позволяет назначать тикеты, но отображается предупреждение на UI.")
        public ResponseEntity<ApiResponse<UserStatusResponse>> changeStatus(
                        @AuthenticationPrincipal User user,
                        @Valid @RequestBody UpdateStatusRequest request) {

                UserActivityStatus newStatus = userActivityStatusService.setStatus(user, request.status());

                UserStatusResponse response = new UserStatusResponse(
                                newStatus,
                                newStatus.isAvailableForAssignment(),
                                java.time.Instant.now());

                return ResponseEntity.ok(ApiResponse.success("Статус обновлён на: " + newStatus, response));
        }
}
