package com.bm.wschat.feature.ticket.controller;

import com.bm.wschat.feature.ticket.dto.assignment.request.AssignmentCreateRequest;
import com.bm.wschat.feature.ticket.dto.assignment.request.AssignmentRejectRequest;
import com.bm.wschat.feature.ticket.dto.assignment.response.AssignmentResponse;
import com.bm.wschat.feature.ticket.service.AssignmentService;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class AssignmentController {

    private final AssignmentService assignmentService;

    /**
     * Создать назначение тикета
     */
    @PostMapping("/assignments")
    @PreAuthorize("hasAnyRole('SPECIALIST', 'ADMIN')")
    public ResponseEntity<ApiResponse<AssignmentResponse>> createAssignment(
            @Valid @RequestBody AssignmentCreateRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Assignment created successfully",
                        assignmentService.createAssignment(request, user.getId())));
    }

    /**
     * Принять назначение
     */
    @PostMapping("/assignments/{id}/accept")
    @PreAuthorize("hasAnyRole('SPECIALIST', 'ADMIN')")
    public ResponseEntity<ApiResponse<AssignmentResponse>> acceptAssignment(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success("Assignment accepted",
                assignmentService.acceptAssignment(id, user.getId())));
    }

    /**
     * Отклонить назначение
     */
    @PostMapping("/assignments/{id}/reject")
    @PreAuthorize("hasAnyRole('SPECIALIST', 'ADMIN')")
    public ResponseEntity<ApiResponse<AssignmentResponse>> rejectAssignment(
            @PathVariable Long id,
            @Valid @RequestBody AssignmentRejectRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success("Assignment rejected",
                assignmentService.rejectAssignment(id, request, user.getId())));
    }

    /**
     * История назначений тикета
     */
    @GetMapping("/tickets/{ticketId}/assignments")
    public ResponseEntity<ApiResponse<List<AssignmentResponse>>> getTicketAssignments(
            @PathVariable Long ticketId) {
        return ResponseEntity.ok(ApiResponse.success(
                assignmentService.getTicketAssignments(ticketId)));
    }

    /**
     * Получить назначение по ID
     */
    @GetMapping("/assignments/{id}")
    public ResponseEntity<ApiResponse<AssignmentResponse>> getAssignment(
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(assignmentService.getById(id)));
    }

    /**
     * Мои ожидающие назначения
     */
    @GetMapping("/assignments/pending")
    @PreAuthorize("hasAnyRole('SPECIALIST', 'ADMIN')")
    public ResponseEntity<ApiResponse<Page<AssignmentResponse>>> getMyPendingAssignments(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success(
                assignmentService.getMyPendingAssignments(user.getId(), pageable)));
    }

    /**
     * Количество ожидающих назначений
     */
    @GetMapping("/assignments/pending-count")
    @PreAuthorize("hasAnyRole('SPECIALIST', 'ADMIN')")
    public ResponseEntity<ApiResponse<Long>> getPendingCount(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success(
                assignmentService.getPendingCount(user.getId())));
    }

    /**
     * Текущее назначение тикета
     */
    @GetMapping("/tickets/{ticketId}/current-assignment")
    public ResponseEntity<ApiResponse<AssignmentResponse>> getCurrentAssignment(
            @PathVariable Long ticketId) {
        return ResponseEntity.ok(ApiResponse.success(
                assignmentService.getCurrentAssignment(ticketId)));
    }
}
