package com.bm.wschat.feature.ticket.controller;

import com.bm.wschat.feature.ticket.dto.timeentry.request.CreateTimeEntryRequest;
import com.bm.wschat.feature.ticket.dto.timeentry.request.UpdateTimeEntryRequest;
import com.bm.wschat.feature.ticket.dto.timeentry.response.TimeEntryResponse;
import com.bm.wschat.feature.ticket.dto.timeentry.response.TimeTotalResponse;
import com.bm.wschat.feature.ticket.service.TimeEntryService;
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
public class TimeEntryController {

    private final TimeEntryService timeEntryService;

    /**
     * Добавить запись времени к тикету
     */
    @PostMapping("/tickets/{ticketId}/time-entries")
    @PreAuthorize("hasAnyRole('SPECIALIST', 'ADMIN')")
    public ResponseEntity<ApiResponse<TimeEntryResponse>> createTimeEntry(
            @PathVariable Long ticketId,
            @Valid @RequestBody CreateTimeEntryRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Time entry created",
                        timeEntryService.createTimeEntry(ticketId, request, user.getId())));
    }

    /**
     * Записи времени тикета
     */
    @GetMapping("/tickets/{ticketId}/time-entries")
    public ResponseEntity<ApiResponse<List<TimeEntryResponse>>> getTicketTimeEntries(
            @PathVariable Long ticketId) {
        return ResponseEntity.ok(ApiResponse.success(
                timeEntryService.getByTicketId(ticketId)));
    }

    /**
     * Итого времени по тикету
     */
    @GetMapping("/tickets/{ticketId}/time-total")
    public ResponseEntity<ApiResponse<TimeTotalResponse>> getTimeTotal(@PathVariable Long ticketId) {
        return ResponseEntity.ok(ApiResponse.success(timeEntryService.getTimeTotal(ticketId)));
    }

    /**
     * Получить запись по ID
     */
    @GetMapping("/time-entries/{id}")
    public ResponseEntity<ApiResponse<TimeEntryResponse>> getTimeEntry(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(timeEntryService.getById(id)));
    }

    /**
     * Мои записи времени
     */
    @GetMapping("/time-entries/my")
    @PreAuthorize("hasAnyRole('SPECIALIST', 'ADMIN')")
    public ResponseEntity<ApiResponse<Page<TimeEntryResponse>>> getMyTimeEntries(
            @PageableDefault(size = 20, sort = "entryDate", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success(
                timeEntryService.getMyEntries(user.getId(), pageable)));
    }

    /**
     * Обновить запись
     */
    @PutMapping("/time-entries/{id}")
    @PreAuthorize("hasAnyRole('SPECIALIST', 'ADMIN')")
    public ResponseEntity<ApiResponse<TimeEntryResponse>> updateTimeEntry(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTimeEntryRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success("Time entry updated",
                timeEntryService.updateTimeEntry(id, request, user.getId())));
    }

    /**
     * Удалить запись
     */
    @DeleteMapping("/time-entries/{id}")
    @PreAuthorize("hasAnyRole('SPECIALIST', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteTimeEntry(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        timeEntryService.deleteTimeEntry(id, user.getId());
        return ResponseEntity.ok(ApiResponse.success("Time entry deleted"));
    }
}
