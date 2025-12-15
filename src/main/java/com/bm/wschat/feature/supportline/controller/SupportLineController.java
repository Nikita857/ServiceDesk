package com.bm.wschat.feature.supportline.controller;

import com.bm.wschat.feature.supportline.dto.request.CreateSupportLineRequest;
import com.bm.wschat.feature.supportline.dto.request.UpdateSupportLineRequest;
import com.bm.wschat.feature.supportline.dto.response.SpecialistResponse;
import com.bm.wschat.feature.supportline.dto.response.SupportLineListResponse;
import com.bm.wschat.feature.supportline.dto.response.SupportLineResponse;
import com.bm.wschat.feature.supportline.service.SupportLineService;
import com.bm.wschat.feature.user.model.User;
import com.bm.wschat.shared.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/support-lines")
@RequiredArgsConstructor
@Tag(name = "Support Lines", description = "Управление линиями поддержки")
public class SupportLineController {

    private final SupportLineService supportLineService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Создать новую линию поддержки", description = "Создает новую линию поддержки с указанным именем и описанием.")
    public ResponseEntity<ApiResponse<SupportLineResponse>> createLine(
            @Valid @RequestBody CreateSupportLineRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Линия поддержки создана",
                        supportLineService.createLine(request)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получить линию поддержки по ID", description = "Возвращает информацию о линии поддержки по ее уникальному идентификатору.")
    public ResponseEntity<ApiResponse<SupportLineResponse>> getLine(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(supportLineService.getLineById(id)));
    }

    @GetMapping
    @Operation(summary = "Получить все линии поддержки", description = "Возвращает список всех доступных линий поддержки.")
    public ResponseEntity<ApiResponse<List<SupportLineListResponse>>> getAllLines() {
        return ResponseEntity.ok(ApiResponse.success(supportLineService.getAllLines()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Обновить линию поддержки", description = "Обновляет информацию о существующей линии поддержки.")
    public ResponseEntity<ApiResponse<SupportLineResponse>> updateLine(
            @PathVariable Long id,
            @Valid @RequestBody UpdateSupportLineRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Линия поддержки обновлена",
                supportLineService.updateLine(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Удалить линию поддержки", description = "Удаляет линию поддержки по ее уникальному идентификатору.")
    public ResponseEntity<ApiResponse<Void>> deleteLine(@PathVariable Long id) {
        supportLineService.deleteLine(id);
        return ResponseEntity.ok(ApiResponse.success("Линия поддержки удалена"));
    }

    @PostMapping("/{lineId}/specialists/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Добавить специалиста к линии поддержки", description = "Добавляет указанного специалиста к указанной линии поддержки.")
    public ResponseEntity<ApiResponse<SupportLineResponse>> addSpecialist(
            @PathVariable Long lineId,
            @PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.success("Специалист успешно добавлен",
                supportLineService.addSpecialist(lineId, userId)));
    }

    @DeleteMapping("/{lineId}/specialists/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Удалить специалиста из линии поддержки", description = "Удаляет указанного специалиста из указанной линии поддержки.")
    public ResponseEntity<ApiResponse<SupportLineResponse>> removeSpecialist(
            @PathVariable Long lineId,
            @PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.success("Специалист удален из линии",
                supportLineService.removeSpecialist(lineId, userId)));
    }

    @GetMapping("/{lineId}/specialists")
    @Operation(summary = "Получить список специалистов для линии поддержки", description = "Возвращает список всех специалистов, привязанных к указанной линии поддержки.")
    public ResponseEntity<ApiResponse<List<SpecialistResponse>>> getSpecialists(@PathVariable Long lineId) {
        return ResponseEntity.ok(ApiResponse.success(supportLineService.getLineSpecialists(lineId)));
    }

    @GetMapping("/my-lines")
    @PreAuthorize("hasAnyRole('SYSADMIN','SPECIALIST','DEVELOPER','ADMIN')")
    @Operation(summary = "Получить линии поддержки текущего специалиста", description = "Возвращает список линий поддержки, к которым привязан текущий аутентифицированный специалист.")
    public ResponseEntity<ApiResponse<List<SupportLineListResponse>>> getMyLines(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success(supportLineService.getLinesBySpecialist(user.getId())));
    }
}
