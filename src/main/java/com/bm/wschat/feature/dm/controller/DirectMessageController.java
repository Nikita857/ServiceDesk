package com.bm.wschat.feature.dm.controller;

import com.bm.wschat.feature.dm.dto.request.SendDirectMessageRequest;
import com.bm.wschat.feature.dm.dto.response.ConversationResponse;
import com.bm.wschat.feature.dm.dto.response.DirectMessageResponse;
import com.bm.wschat.feature.dm.service.DirectMessageService;
import com.bm.wschat.feature.user.model.User;
import com.bm.wschat.shared.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Direct Messages", description = "Управление личными сообщениями")
public class DirectMessageController {

    private final DirectMessageService dmService;

    @PostMapping
    @Operation(summary = "Отправить личное сообщение", description = "Отправляет новое личное сообщение указанному получателю.")
    public ResponseEntity<ApiResponse<DirectMessageResponse>> sendMessage(
            @Valid @RequestBody SendDirectMessageRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Message sent",
                        dmService.sendMessage(request, user.getId())));
    }

    @GetMapping("/conversation/{partnerId}")
    @Operation(summary = "Получить переписку с другим пользователем", description = "Возвращает пагинированный список личных сообщений между текущим пользователем и указанным партнером.")
    public ResponseEntity<ApiResponse<Page<DirectMessageResponse>>> getConversation(
            @PathVariable Long partnerId,
            @PageableDefault(size = 50, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success(
                dmService.getConversation(user.getId(), partnerId, pageable)));
    }

    @GetMapping("/conversations")
    @Operation(summary = "Получить список всех диалогов текущего пользователя", description = "Возвращает список всех диалогов, в которых участвует текущий пользователь, с последними сообщениями.")
    public ResponseEntity<ApiResponse<List<ConversationResponse>>> getConversations(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success(
                dmService.getConversations(user.getId())));
    }

    @PostMapping("/conversation/{partnerId}/read")
    @Operation(summary = "Пометить сообщения от пользователя как прочитанные", description = "Помечает все непрочитанные личные сообщения от указанного партнера как прочитанные для текущего пользователя.")
    public ResponseEntity<ApiResponse<Integer>> markAsRead(
            @PathVariable Long partnerId,
            @AuthenticationPrincipal User user) {
        int count = dmService.markAsRead(partnerId, user.getId());
        return ResponseEntity.ok(ApiResponse.success("Marked " + count + " messages as read", count));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Получить количество непрочитанных личных сообщений", description = "Возвращает общее количество непрочитанных личных сообщений для текущего пользователя.")
    public ResponseEntity<ApiResponse<Long>> getUnreadCount(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success(dmService.getUnreadCount(user.getId())));
    }

    @DeleteMapping("/{messageId}")
    @Operation(summary = "Удалить личное сообщение", description = "Удаляет указанное личное сообщение (логическое удаление).")
    public ResponseEntity<ApiResponse<Void>> deleteMessage(
            @PathVariable Long messageId,
            @AuthenticationPrincipal User user) {
        dmService.deleteMessage(messageId, user.getId());
        return ResponseEntity.ok(ApiResponse.success("Message deleted"));
    }
}
