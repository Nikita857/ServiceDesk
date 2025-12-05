package com.bm.wschat.feature.auth.controller;

import com.bm.wschat.feature.ticket.dto.assignment.request.AssignmentCreateRequest;
import com.bm.wschat.feature.ticket.dto.assignment.response.AssignmentResponse;
import com.bm.wschat.feature.ticket.service.AssignmentService;
import com.bm.wschat.shared.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tickets")
@RequiredArgsConstructor
public class TicketController {
    private final AssignmentService assignmentService;

    @PreAuthorize("hasAnyAuthority('ADMIN, SPECIALIST, DEVELOPER')")
    @PostMapping("/assignments")
    public ResponseEntity<ApiResponse<AssignmentResponse>> createAssignment(
            @Valid AssignmentCreateRequest request)
    {
        return ResponseEntity.ok(
                ApiResponse.success(
                        assignmentService.assignTicket(request)));
    }
}
