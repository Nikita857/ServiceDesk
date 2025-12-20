package com.bm.wschat.feature.ticket.controller;

import com.bm.wschat.feature.ticket.dto.timeentry.request.CreateTimeEntryRequest;
import com.bm.wschat.feature.ticket.dto.timeentry.request.UpdateTimeEntryRequest;
import com.bm.wschat.feature.ticket.dto.timeentry.response.TimeEntryResponse;
import com.bm.wschat.feature.ticket.dto.timeentry.response.TimeTotalResponse;
import com.bm.wschat.feature.ticket.service.TimeEntryService;
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
@Tag(name = "Time Entries", description = "Учет рабочего времени по тикетам")
public class TimeEntryController {

    private final TimeEntryService timeEntryService;

    @PostMapping("/tickets/{ticketId}/time-entries")
    @PreAuthorize("hasAnyRole('SYSADMIN','1CSUPPORT','DEV1C','DEVELOPER','ADMIN')")
    @Operation(summary = "Записать время по тикету", description = "Добавляет новую запись о затраченном времени для указанного тикета.")
    public ResponseEntity<ApiResponse<TimeEntryResponse>> createTimeEntry(
            @PathVariable Long ticketId,
            @Valid @RequestBody CreateTimeEntryRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Запись времени создана",
                        timeEntryService.createTimeEntry(ticketId, request, user.getId())));
    }

    @GetMapping("/tickets/{ticketId}/time-entries")
    @Operation(summary = "Получить записи времени для тикета", description = "Возвращает список всех записей времени, связанных с указанным тикетом.")
    public ResponseEntity<ApiResponse<List<TimeEntryResponse>>> getTicketTimeEntries(
            @PathVariable Long ticketId) {
        return ResponseEntity.ok(ApiResponse.success(
                timeEntryService.getByTicketId(ticketId)));
    }

    @GetMapping("/tickets/{ticketId}/time-total")
    @Operation(summary = "Получить суммарное время по тикету", description = "Возвращает общее количество времени (в секундах), затраченное на указанный тикет.")
    public ResponseEntity<ApiResponse<TimeTotalResponse>> getTimeTotal(@PathVariable Long ticketId) {
        return ResponseEntity.ok(ApiResponse.success(timeEntryService.getTimeTotal(ticketId)));
    }

    @GetMapping("/time-entries/{id}")
    @Operation(summary = "Получить запись времени по ID", description = "Возвращает информацию о конкретной записи времени по ее уникальному идентификатору.")
    public ResponseEntity<ApiResponse<TimeEntryResponse>> getTimeEntry(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(timeEntryService.getById(id)));
    }

    @GetMapping("/time-entries/my")
    @PreAuthorize("hasAnyRole('SYSADMIN','1CSUPPORT','DEV1C','DEVELOPER','ADMIN')")
    @Operation(summary = "Получить мои записи времени", description = "Возвращает пагинированный список записей времени, созданных текущим аутентифицированным специалистом.")
    public ResponseEntity<ApiResponse<Page<TimeEntryResponse>>> getMyTimeEntries(
            @PageableDefault(size = 20, sort = "entryDate", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success(
                timeEntryService.getMyEntries(user.getId(), pageable)));
    }

    @PutMapping("/time-entries/{id}")
    @PreAuthorize("hasAnyRole('SYSADMIN','1CSUPPORT','DEV1C','DEVELOPER','ADMIN')")
    @Operation(summary = "Обновить запись времени", description = "Обновляет существующую запись о затраченном времени.")
    public ResponseEntity<ApiResponse<TimeEntryResponse>> updateTimeEntry(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTimeEntryRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success("Запись времени обновлена",
                timeEntryService.updateTimeEntry(id, request, user.getId())));
    }

    @DeleteMapping("/time-entries/{id}")
    @PreAuthorize("hasAnyRole('SYSADMIN','1CSUPPORT','DEV1C','DEVELOPER','ADMIN')")
    @Operation(summary = "Удалить запись времени", description = "Удаляет указанную запись о затраченном времени.")
    public ResponseEntity<ApiResponse<Void>> deleteTimeEntry(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        timeEntryService.deleteTimeEntry(id, user.getId());
        return ResponseEntity.ok(ApiResponse.success("Запись времени удалена"));
    }
}
