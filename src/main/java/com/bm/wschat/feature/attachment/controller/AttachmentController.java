package com.bm.wschat.feature.attachment.controller;

import com.bm.wschat.feature.attachment.dto.request.ConfirmUploadRequest;
import com.bm.wschat.feature.attachment.dto.request.UploadUrlRequest;
import com.bm.wschat.feature.attachment.dto.response.AttachmentResponse;
import com.bm.wschat.feature.attachment.dto.response.UploadUrlResponse;
import com.bm.wschat.feature.attachment.service.AttachmentService;
import com.bm.wschat.feature.user.model.User;
import com.bm.wschat.shared.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
import java.util.Map;

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
        @Operation(summary = "Скачать файл по имени (legacy)", description = "Устаревший эндпоинт. Используйте /attachments/{id}/download")
        public ResponseEntity<Resource> downloadFile(@PathVariable String filename) {
                Resource resource = attachmentService.download(filename);

                String contentType = "application/octet-stream";

                return ResponseEntity.ok()
                                .contentType(MediaType.parseMediaType(contentType))
                                .header(HttpHeaders.CONTENT_DISPOSITION,
                                                "attachment; filename=\"" + filename + "\"")
                                .body(resource);
        }

        @GetMapping("/attachments/{attachmentId}/download")
        @Operation(summary = "Скачать файл вложения", description = "Скачивает файл с оригинальным именем")
        public ResponseEntity<Resource> downloadById(@PathVariable Long attachmentId) {
                AttachmentService.DownloadResult result = attachmentService.downloadById(attachmentId);

                String mimeType = result.mimeType() != null ? result.mimeType() : "application/octet-stream";
                String filename = result.originalFilename() != null ? result.originalFilename() : "file";

                // RFC 5987 для поддержки UTF-8 имён файлов
                String encodedFilename = java.net.URLEncoder.encode(filename, java.nio.charset.StandardCharsets.UTF_8)
                                .replace("+", "%20");

                return ResponseEntity.ok()
                                .contentType(MediaType.parseMediaType(mimeType))
                                .header(HttpHeaders.CONTENT_DISPOSITION,
                                                "attachment; filename=\"" + filename + "\"; filename*=UTF-8''"
                                                                + encodedFilename)
                                .body(result.resource());
        }

        @DeleteMapping("/attachments/{attachmentId}")
        @Operation(summary = "Удалить вложение")
        public ResponseEntity<ApiResponse<Void>> deleteAttachment(
                        @PathVariable Long attachmentId,
                        @AuthenticationPrincipal User user) {
                attachmentService.delete(attachmentId, user.getId());
                return ResponseEntity.ok(ApiResponse.success("Вложение успешно удалено"));
        }

        // === Wiki Article Attachments ===

        @PostMapping("/wiki/{articleId}/attachments")
        @Operation(summary = "Загрузить вложение к статье Wiki")
        public ResponseEntity<ApiResponse<AttachmentResponse>> uploadToWikiArticle(
                        @PathVariable Long articleId,
                        @RequestParam("file") MultipartFile file,
                        @AuthenticationPrincipal User user) {
                return ResponseEntity.status(HttpStatus.CREATED)
                                .body(ApiResponse.success("Файл загружен",
                                                attachmentService.uploadToWikiArticle(articleId, file, user.getId())));
        }

        @GetMapping("/wiki/{articleId}/attachments")
        @Operation(summary = "Получить список вложений статьи Wiki")
        public ResponseEntity<ApiResponse<List<AttachmentResponse>>> getWikiArticleAttachments(
                        @PathVariable Long articleId) {
                return ResponseEntity.ok(ApiResponse.success(
                                attachmentService.getByWikiArticleId(articleId)));
        }

        // === Presigned URL Endpoints (MinIO Direct Upload) ===

        @PostMapping("/attachments/upload-url")
        @Operation(summary = "Получить presigned URL для загрузки", description = "Генерирует presigned URL для прямой загрузки файла в MinIO")
        public ResponseEntity<ApiResponse<UploadUrlResponse>> getUploadUrl(
                        @Valid @RequestBody UploadUrlRequest request) {
                return ResponseEntity.ok(ApiResponse.success(
                                attachmentService.generateUploadUrl(request)));
        }

        @PostMapping("/attachments/confirm")
        @Operation(summary = "Подтвердить загрузку", description = "Подтверждает успешную загрузку файла и создаёт запись в БД")
        public ResponseEntity<ApiResponse<AttachmentResponse>> confirmUpload(
                        @Valid @RequestBody ConfirmUploadRequest request,
                        @AuthenticationPrincipal User user) {
                return ResponseEntity.status(HttpStatus.CREATED)
                                .body(ApiResponse.success("Файл успешно загружен",
                                                attachmentService.confirmUpload(request, user.getId())));
        }

        @GetMapping("/attachments/{attachmentId}/url")
        @Operation(summary = "Получить presigned URL для скачивания", description = "Возвращает временную ссылку для скачивания файла напрямую из MinIO")
        public ResponseEntity<ApiResponse<Map<String, String>>> getDownloadUrl(
                        @PathVariable Long attachmentId) {
                String url = attachmentService.generateDownloadUrl(attachmentId);
                return ResponseEntity.ok(ApiResponse.success(Map.of("downloadUrl", url)));
        }
}
