package com.bm.wschat.feature.message.controller;

import com.bm.wschat.feature.message.dto.request.EditMessageRequest;
import com.bm.wschat.feature.message.dto.request.SendMessageRequest;
import com.bm.wschat.feature.message.dto.response.MessageResponse;
import com.bm.wschat.feature.message.service.MessageService;
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

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Messages", description = "Управление сообщениями в тикетах")
public class MessageController {

    private final MessageService messageService;

    @PostMapping("/tickets/{ticketId}/messages")
    @Operation(summary = "Отправить сообщение в тикет", description = "Отправляет новое сообщение в указанный тикет.")
    public ResponseEntity<ApiResponse<MessageResponse>> sendMessage(
            @PathVariable Long ticketId,
            @Valid @RequestBody SendMessageRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Message sent successfully",
                        messageService.sendMessage(ticketId, request, user.getId())));
    }

    @GetMapping("/tickets/{ticketId}/messages")
    @Operation(summary = "Получить все сообщения тикета", description = "Возвращает пагинированный список сообщений для указанного тикета.")
    public ResponseEntity<ApiResponse<Page<MessageResponse>>> getTicketMessages(
            @PathVariable Long ticketId,
            @PageableDefault(size = 50, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success(
                messageService.getTicketMessages(ticketId, pageable, user)));
    }

    @PostMapping("/tickets/{ticketId}/messages/read")
    @Operation(summary = "Пометить все сообщения тикета как прочитанные", description = "Помечает все непрочитанные сообщения в указанном тикете как прочитанные для текущего пользователя.")
    public ResponseEntity<ApiResponse<Integer>> markAsRead(
            @PathVariable Long ticketId,
            @AuthenticationPrincipal User user) {
        int count = messageService.markAsRead(ticketId, user);
        return ResponseEntity.ok(ApiResponse.success("Marked " + count + " messages as read", count));
    }

    @GetMapping("/tickets/{ticketId}/messages/unread-count")
    @Operation(summary = "Получить количество непрочитанных сообщений в тикете", description = "Возвращает количество непрочитанных сообщений в указанном тикете для текущего пользователя.")
    public ResponseEntity<ApiResponse<Long>> getUnreadCount(
            @PathVariable Long ticketId,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success(messageService.getUnreadCount(ticketId, user)));
    }

    @PatchMapping("/messages/{messageId}")
    @Operation(summary = "Изменить сообщение в тикете", description = "Изменяет содержимое существующего сообщения в тикете.")
    public ResponseEntity<ApiResponse<MessageResponse>> editMessage(
            @PathVariable Long messageId,
            @Valid @RequestBody EditMessageRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success("Message updated successfully",
                messageService.editMessage(messageId, request, user.getId())));
    }

    @DeleteMapping("/messages/{messageId}")
    @Operation(summary = "Удалить сообщение из тикета", description = "Удаляет указанное сообщение из тикета (логическое удаление).")
    public ResponseEntity<ApiResponse<Void>> deleteMessage(
            @PathVariable Long messageId,
            @AuthenticationPrincipal User user) {
        messageService.deleteMessage(messageId, user.getId());
        return ResponseEntity.ok(ApiResponse.success("Message deleted successfully"));
    }
}
