package com.bm.wschat.feature.dm.controller;

import com.bm.wschat.feature.dm.dto.request.SendDirectMessageRequest;
import com.bm.wschat.feature.dm.dto.response.ConversationResponse;
import com.bm.wschat.feature.dm.dto.response.DirectMessageResponse;
import com.bm.wschat.feature.dm.service.DirectMessageService;
import com.bm.wschat.feature.user.model.User;
import com.bm.wschat.shared.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/dm")
@RequiredArgsConstructor
public class DirectMessageController {

    private final DirectMessageService dmService;

    /**
     * Отправить личное сообщение
     */
    @PostMapping
    public ResponseEntity<ApiResponse<DirectMessageResponse>> sendMessage(
            @Valid @RequestBody SendDirectMessageRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Message sent",
                        dmService.sendMessage(request, user.getId())));
    }

    /**
     * Получить переписку с пользователем
     */
    @GetMapping("/conversation/{partnerId}")
    public ResponseEntity<ApiResponse<Page<DirectMessageResponse>>> getConversation(
            @PathVariable Long partnerId,
            @PageableDefault(size = 50, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success(
                dmService.getConversation(user.getId(), partnerId, pageable)));
    }

    /**
     * Список всех диалогов
     */
    @GetMapping("/conversations")
    public ResponseEntity<ApiResponse<List<ConversationResponse>>> getConversations(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success(
                dmService.getConversations(user.getId())));
    }

    /**
     * Пометить сообщения от пользователя как прочитанные
     */
    @PostMapping("/conversation/{partnerId}/read")
    public ResponseEntity<ApiResponse<Integer>> markAsRead(
            @PathVariable Long partnerId,
            @AuthenticationPrincipal User user) {
        int count = dmService.markAsRead(partnerId, user.getId());
        return ResponseEntity.ok(ApiResponse.success("Marked " + count + " messages as read", count));
    }

    /**
     * Количество непрочитанных сообщений
     */
    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Long>> getUnreadCount(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success(dmService.getUnreadCount(user.getId())));
    }

    /**
     * Удалить сообщение
     */
    @DeleteMapping("/{messageId}")
    public ResponseEntity<ApiResponse<Void>> deleteMessage(
            @PathVariable Long messageId,
            @AuthenticationPrincipal User user) {
        dmService.deleteMessage(messageId, user.getId());
        return ResponseEntity.ok(ApiResponse.success("Message deleted"));
    }
}
