package com.bm.wschat.feature.ticket.controller;

import com.bm.wschat.feature.ticket.dto.assignment.request.AssignmentCreateRequest;
import com.bm.wschat.feature.ticket.dto.assignment.request.AssignmentRejectRequest;
import com.bm.wschat.feature.ticket.dto.assignment.response.AssignmentResponse;
import com.bm.wschat.feature.ticket.service.AssignmentService;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Assignments", description = "Управление назначениями тикетов")
public class AssignmentController {

    private final AssignmentService assignmentService;

    @PostMapping("/assignments")
    @Operation(summary = "Создать новое назначение для тикета", description = "Назначает тикет специалисту или линии поддержки.")
    public ResponseEntity<ApiResponse<AssignmentResponse>> createAssignment(
            @Valid @RequestBody AssignmentCreateRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Assignment created successfully",
                        assignmentService.createAssignment(request, user.getId())));
    }

    @PostMapping("/assignments/{id}/accept")
    @PreAuthorize("hasAnyRole('SYSADMIN','DEV1C','DEVELOPER','ADMIN')")
    @Operation(summary = "Принять назначение тикета", description = "Текущий пользователь принимает назначенную ему задачу по тикету.")
    public ResponseEntity<ApiResponse<AssignmentResponse>> acceptAssignment(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success("Assignment accepted",
                assignmentService.acceptAssignment(id, user.getId())));
    }

    @PostMapping("/assignments/{id}/reject")
    @PreAuthorize("hasAnyRole('SYSADMIN','DEV1C','DEVELOPER','ADMIN')")
    @Operation(summary = "Отклонить назначение тикета", description = "Текущий пользователь отклоняет назначенную ему задачу по тикету с указанием причины.")
    public ResponseEntity<ApiResponse<AssignmentResponse>> rejectAssignment(
            @PathVariable Long id,
            @Valid @RequestBody AssignmentRejectRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success("Assignment rejected",
                assignmentService.rejectAssignment(id, request, user.getId())));
    }

    @GetMapping("/tickets/{ticketId}/assignments")
    @Operation(summary = "Получить историю назначений для тикета", description = "Возвращает список всех назначений для указанного тикета.")
    public ResponseEntity<ApiResponse<List<AssignmentResponse>>> getTicketAssignments(
            @PathVariable Long ticketId) {
        return ResponseEntity.ok(ApiResponse.success(
                assignmentService.getTicketAssignments(ticketId)));
    }

    @GetMapping("/assignments/{id}")
    @Operation(summary = "Получить назначение по ID", description = "Возвращает информацию о конкретном назначении по его ID.")
    public ResponseEntity<ApiResponse<AssignmentResponse>> getAssignment(
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(assignmentService.getById(id)));
    }

    @GetMapping("/assignments/pending")
    @PreAuthorize("hasAnyRole('SYSADMIN','DEV1C','DEVELOPER','ADMIN')")
    @Operation(summary = "Получить мои ожидающие назначения", description = "Возвращает пагинированный список назначений, ожидающих принятия текущим специалистом.")
    public ResponseEntity<ApiResponse<Page<AssignmentResponse>>> getMyPendingAssignments(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success(
                assignmentService.getMyPendingAssignments(user.getId(), pageable)));
    }

    @GetMapping("/assignments/pending-count")
    @PreAuthorize("hasAnyRole('SYSADMIN','DEV1C','DEVELOPER','ADMIN')")
    @Operation(summary = "Получить количество моих ожидающих назначений", description = "Возвращает количество назначений, ожидающих принятия текущим специалистом.")
    public ResponseEntity<ApiResponse<Long>> getPendingCount(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success(
                assignmentService.getPendingCount(user.getId())));
    }

    @GetMapping("/tickets/{ticketId}/current-assignment")
    @Operation(summary = "Получить текущее активное назначение для тикета", description = "Возвращает информацию о текущем активном назначении для указанного тикета, если оно есть.")
    public ResponseEntity<ApiResponse<AssignmentResponse>> getCurrentAssignment(
            @PathVariable Long ticketId) {
        return ResponseEntity.ok(ApiResponse.success(
                assignmentService.getCurrentAssignment(ticketId)));
    }
}
