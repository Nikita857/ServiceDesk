package com.bm.wschat.feature.user.controller;

import com.bm.wschat.feature.user.dto.response.UserSearchResponse;
import com.bm.wschat.feature.user.model.User;
import com.bm.wschat.feature.user.repository.UserRepository;
import com.bm.wschat.shared.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "Поиск и управление пользователями")
public class UserController {

    private final UserRepository userRepository;

    @GetMapping("/search")
    @Operation(summary = "Поиск пользователей", description = "Поиск пользователей по ФИО или username. " +
            "Текущий пользователь исключается из результатов поиска.")
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
}
