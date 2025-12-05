package com.bm.wschat.feature.ticket.controller;

import com.bm.wschat.feature.ticket.dto.ticket.request.ChangeStatusRequest;
import com.bm.wschat.feature.ticket.dto.ticket.request.CreateTicketRequest;
import com.bm.wschat.feature.ticket.dto.ticket.request.UpdateTicketRequest;
import com.bm.wschat.feature.ticket.dto.ticket.response.TicketListResponse;
import com.bm.wschat.feature.ticket.dto.ticket.response.TicketResponse;
import com.bm.wschat.feature.ticket.model.TicketStatus;
import com.bm.wschat.feature.ticket.service.TicketService;
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

@RestController
@RequestMapping("/api/v1/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;

    @PostMapping
    public ResponseEntity<ApiResponse<TicketResponse>> createTicket(
            @Valid @RequestBody CreateTicketRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Ticket created successfully",
                        ticketService.createTicket(request, user.getId())));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TicketResponse>> getTicket(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(ticketService.getTicketById(id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<TicketResponse>> updateTicket(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTicketRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Ticket updated successfully",
                ticketService.updateTicket(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteTicket(@PathVariable Long id) {
        ticketService.deleteTicket(id);
        return ResponseEntity.ok(ApiResponse.success("Ticket deleted successfully"));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('SPECIALIST', 'ADMIN')")
    public ResponseEntity<ApiResponse<TicketResponse>> changeStatus(
            @PathVariable Long id,
            @Valid @RequestBody ChangeStatusRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Status changed successfully",
                ticketService.changeStatus(id, request)));
    }

    @PatchMapping("/{id}/assign-line")
    @PreAuthorize("hasAnyRole('SPECIALIST', 'ADMIN')")
    public ResponseEntity<ApiResponse<TicketResponse>> assignToLine(
            @PathVariable Long id,
            @RequestParam Long lineId) {
        return ResponseEntity.ok(ApiResponse.success("Assigned to line successfully",
                ticketService.assignToLine(id, lineId)));
    }

    @PatchMapping("/{id}/assign-specialist")
    @PreAuthorize("hasAnyRole('SPECIALIST', 'ADMIN')")
    public ResponseEntity<ApiResponse<TicketResponse>> assignToSpecialist(
            @PathVariable Long id,
            @RequestParam Long specialistId) {
        return ResponseEntity.ok(ApiResponse.success("Assigned to specialist successfully",
                ticketService.assignToSpecialist(id, specialistId)));
    }

    @PatchMapping("/{id}/category-user")
    public ResponseEntity<ApiResponse<TicketResponse>> setUserCategory(
            @PathVariable Long id,
            @RequestParam Long categoryId) {
        return ResponseEntity.ok(ApiResponse.success("User category set successfully",
                ticketService.setUserCategory(id, categoryId)));
    }

    @PatchMapping("/{id}/category-support")
    @PreAuthorize("hasAnyRole('SPECIALIST', 'ADMIN')")
    public ResponseEntity<ApiResponse<TicketResponse>> setSupportCategory(
            @PathVariable Long id,
            @RequestParam Long categoryId) {
        return ResponseEntity.ok(ApiResponse.success("Support category set successfully",
                ticketService.setSupportCategory(id, categoryId)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<TicketListResponse>>> listTickets(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(ticketService.listTickets(pageable)));
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<Page<TicketListResponse>>> getMyTickets(
            @AuthenticationPrincipal User user,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(ticketService.getMyTickets(user.getId(), pageable)));
    }

    @GetMapping("/assigned")
    @PreAuthorize("hasAnyRole('SPECIALIST', 'ADMIN')")
    public ResponseEntity<ApiResponse<Page<TicketListResponse>>> getAssignedTickets(
            @AuthenticationPrincipal User user,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(ticketService.getAssignedTickets(user.getId(), pageable)));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<ApiResponse<Page<TicketListResponse>>> getTicketsByStatus(
            @PathVariable TicketStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(ticketService.getTicketsByStatus(status, pageable)));
    }

    @GetMapping("/line/{lineId}")
    public ResponseEntity<ApiResponse<Page<TicketListResponse>>> getTicketsByLine(
            @PathVariable Long lineId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(ticketService.getTicketsByLine(lineId, pageable)));
    }
}
