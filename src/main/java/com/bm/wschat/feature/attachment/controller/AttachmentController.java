package com.bm.wschat.feature.attachment.controller;

import com.bm.wschat.feature.attachment.dto.response.AttachmentResponse;
import com.bm.wschat.feature.attachment.service.AttachmentService;
import com.bm.wschat.feature.user.model.User;
import com.bm.wschat.shared.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Attachments", description = "Управление вложениями")
public class AttachmentController {

    private final AttachmentService attachmentService;

    @PostMapping("/tickets/{ticketId}/attachments")
    @Operation(summary = "Загрузить вложение к тикету")
    public ResponseEntity<ApiResponse<AttachmentResponse>> uploadToTicket(
            @PathVariable Long ticketId,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("File uploaded successfully",
                        attachmentService.uploadToTicket(ticketId, file, user.getId())));
    }

    @PostMapping("/messages/{messageId}/attachments")
    @Operation(summary = "Загрузить вложение к сообщению")
    public ResponseEntity<ApiResponse<AttachmentResponse>> uploadToMessage(
            @PathVariable Long messageId,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("File uploaded successfully",
                        attachmentService.uploadToMessage(messageId, file, user.getId())));
    }

    @GetMapping("/tickets/{ticketId}/attachments")
    @Operation(summary = "Получить список вложений тикета")
    public ResponseEntity<ApiResponse<List<AttachmentResponse>>> getTicketAttachments(
            @PathVariable Long ticketId) {
        return ResponseEntity.ok(ApiResponse.success(
                attachmentService.getByTicketId(ticketId)));
    }

    @GetMapping("/messages/{messageId}/attachments")
    @Operation(summary = "Получить список вложений сообщения")
    public ResponseEntity<ApiResponse<List<AttachmentResponse>>> getMessageAttachments(
            @PathVariable Long messageId) {
        return ResponseEntity.ok(ApiResponse.success(
                attachmentService.getByMessageId(messageId)));
    }

    @GetMapping("/attachments/{attachmentId}")
    @Operation(summary = "Получить информацию о вложении")
    public ResponseEntity<ApiResponse<AttachmentResponse>> getAttachment(
            @PathVariable Long attachmentId) {
        return ResponseEntity.ok(ApiResponse.success(
                attachmentService.getById(attachmentId)));
    }

    @GetMapping("/attachments/file/{filename}")
    @Operation(summary = "Скачать файл вложения")
    public ResponseEntity<Resource> downloadFile(@PathVariable String filename) {
        Resource resource = attachmentService.download(filename);

        String contentType = "application/octet-stream";

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filename + "\"")
                .body(resource);
    }

    @DeleteMapping("/attachments/{attachmentId}")
    @Operation(summary = "Удалить вложение")
    public ResponseEntity<ApiResponse<Void>> deleteAttachment(
            @PathVariable Long attachmentId,
            @AuthenticationPrincipal User user) {
        attachmentService.delete(attachmentId, user.getId());
        return ResponseEntity.ok(ApiResponse.success("Attachment deleted successfully"));
    }
}
