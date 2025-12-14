package com.bm.wschat.feature.friendship.controller;

import com.bm.wschat.feature.friendship.dto.response.FriendResponse;
import com.bm.wschat.feature.friendship.dto.response.FriendshipResponse;
import com.bm.wschat.feature.friendship.service.FriendshipService;
import com.bm.wschat.feature.user.model.User;
import com.bm.wschat.shared.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/friends")
@RequiredArgsConstructor
@Tag(name = "Friendship", description = "Управление друзьями и запросами в друзья")
public class FriendshipController {

    private final FriendshipService friendshipService;

    @PostMapping("/request/{userId}")
    @Operation(summary = "Отправить запрос в друзья", description = "Отправляет запрос дружбы указанному пользователю.")
    public ResponseEntity<ApiResponse<FriendshipResponse>> sendRequest(
            @PathVariable Long userId,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        "Запрос отправлен",
                        friendshipService.sendRequest(currentUser.getId(), userId)));
    }

    @PostMapping("/{id}/accept")
    @Operation(summary = "Принять запрос в друзья", description = "Принимает входящий запрос дружбы.")
    public ResponseEntity<ApiResponse<FriendshipResponse>> acceptRequest(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(ApiResponse.success(
                "Запрос принят",
                friendshipService.acceptRequest(id, currentUser.getId())));
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "Отклонить запрос в друзья", description = "Отклоняет входящий запрос дружбы.")
    public ResponseEntity<ApiResponse<FriendshipResponse>> rejectRequest(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(ApiResponse.success(
                "Запрос отклонён",
                friendshipService.rejectRequest(id, currentUser.getId())));
    }

    @DeleteMapping("/{friendId}")
    @Operation(summary = "Удалить из друзей", description = "Удаляет пользователя из списка друзей.")
    public ResponseEntity<ApiResponse<Void>> removeFriend(
            @PathVariable Long friendId,
            @AuthenticationPrincipal User currentUser) {
        friendshipService.removeFriend(currentUser.getId(), friendId);
        return ResponseEntity.ok(ApiResponse.success("Пользователь удалён из друзей"));
    }

    @PostMapping("/block/{userId}")
    @Operation(summary = "Заблокировать пользователя", description = "Блокирует пользователя, запрещая отправку сообщений и запросов.")
    public ResponseEntity<ApiResponse<FriendshipResponse>> blockUser(
            @PathVariable Long userId,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(ApiResponse.success(
                "Пользователь заблокирован",
                friendshipService.blockUser(currentUser.getId(), userId)));
    }

    @GetMapping
    @Operation(summary = "Получить список друзей", description = "Возвращает список всех друзей текущего пользователя.")
    public ResponseEntity<ApiResponse<List<FriendResponse>>> getFriends(
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(ApiResponse.success(
                friendshipService.getFriends(currentUser.getId())));
    }

    @GetMapping("/requests")
    @Operation(summary = "Получить входящие запросы", description = "Возвращает список входящих запросов в друзья.")
    public ResponseEntity<ApiResponse<List<FriendshipResponse>>> getPendingRequests(
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(ApiResponse.success(
                friendshipService.getPendingRequests(currentUser.getId())));
    }

    @GetMapping("/requests/outgoing")
    @Operation(summary = "Получить исходящие запросы", description = "Возвращает список исходящих запросов в друзья.")
    public ResponseEntity<ApiResponse<List<FriendshipResponse>>> getOutgoingRequests(
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(ApiResponse.success(
                friendshipService.getOutgoingRequests(currentUser.getId())));
    }

    @GetMapping("/requests/count")
    @Operation(summary = "Количество входящих запросов", description = "Возвращает количество непринятых входящих запросов.")
    public ResponseEntity<ApiResponse<Long>> getPendingRequestsCount(
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(ApiResponse.success(
                friendshipService.getPendingRequestsCount(currentUser.getId())));
    }

    @GetMapping("/check/{userId}")
    @Operation(summary = "Проверить дружбу", description = "Проверяет, являются ли текущий пользователь и указанный пользователь друзьями.")
    public ResponseEntity<ApiResponse<Boolean>> checkFriendship(
            @PathVariable Long userId,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(ApiResponse.success(
                friendshipService.areFriends(currentUser.getId(), userId)));
    }
}
