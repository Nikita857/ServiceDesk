package com.bm.wschat.feature.report.controller;

import com.bm.wschat.feature.report.dto.response.*;
import com.bm.wschat.feature.report.service.ReportService;
import com.bm.wschat.shared.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
@Tag(name = "Reports", description = "REST контроллер для создания отчетов о работе")
public class ReportController {

    private final ReportService reportService;

    // =====================================================================
    // TIME REPORTS
    // =====================================================================
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/time/by-specialist")
    @Operation(summary = "Отчет по времени по специалистам", description = "Формирует отчет по затраченному времени, сгруппированный по специалистам за указанный период.")
    public ResponseEntity<ApiResponse<List<TimeReportBySpecialistResponse>>> getTimeReportBySpecialist(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(ApiResponse.success(
                reportService.getTimeReportBySpecialist(from, to)));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/time/by-line")
    @Operation(summary = "Отчет по времени по линиям поддержки", description = "Формирует отчет по затраченному времени, сгруппированный по линиям поддержки за указанный период.")
    public ResponseEntity<ApiResponse<List<TimeReportByLineResponse>>> getTimeReportByLine(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(ApiResponse.success(
                reportService.getTimeReportByLine(from, to)));
    }

    // =====================================================================
    // TICKET STATISTICS
    // =====================================================================

    @GetMapping("/tickets/by-status")
    @Operation(summary = "Статистика тикетов по статусам", description = "Возвращает количество тикетов в каждом статусе.")
    public ResponseEntity<ApiResponse<List<TicketStatsByStatusResponse>>> getTicketStatsByStatus() {
        return ResponseEntity.ok(ApiResponse.success(
                reportService.getTicketStatsByStatus()));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/tickets/by-user-category")
    @Operation(summary = "Статистика тикетов по пользовательским категориям", description = "Возвращает количество тикетов в каждой пользовательской категории.")
    public ResponseEntity<ApiResponse<List<TicketStatsByCategoryResponse>>> getTicketStatsByUserCategory() {
        return ResponseEntity.ok(ApiResponse.success(
                reportService.getTicketStatsByUserCategory()));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/tickets/by-support-category")
    @Operation(summary = "Статистика тикетов по категориям поддержки", description = "Возвращает количество тикетов в каждой категории поддержки.")
    public ResponseEntity<ApiResponse<List<TicketStatsByCategoryResponse>>> getTicketStatsBySupportCategory() {
        return ResponseEntity.ok(ApiResponse.success(
                reportService.getTicketStatsBySupportCategory()));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/tickets/resolution-time")
    @Operation(summary = "Статистика по времени решения тикетов", description = "Возвращает среднее, минимальное и максимальное время решения тикетов.")
    public ResponseEntity<ApiResponse<ResolutionTimeResponse>> getResolutionTimeStats() {
        return ResponseEntity.ok(ApiResponse.success(
                reportService.getResolutionTimeStats()));
    }

    // =====================================================================
    // SPECIALIST WORKLOAD
    // =====================================================================

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/specialists/workload")
    @Operation(summary = "Отчет по загрузке специалистов", description = "Показывает количество открытых, закрытых и просроченных тикетов для каждого специалиста.")
    public ResponseEntity<ApiResponse<List<SpecialistWorkloadResponse>>> getSpecialistWorkload() {
        return ResponseEntity.ok(ApiResponse.success(
                reportService.getSpecialistWorkload()));
    }

    // =====================================================================
    // TICKET HISTORY
    // =====================================================================

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/tickets/all")
    @Operation(summary = "Все тикеты включая удалённые", description = "Возвращает список всех тикетов (включая soft-deleted) для отчётов.")
    public ResponseEntity<ApiResponse<org.springframework.data.domain.Page<TicketReportListResponse>>> getAllTickets(
            org.springframework.data.domain.Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(
                reportService.getAllTicketsIncludingDeleted(pageable)));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/tickets/{id}/history")
    @Operation(summary = "История тикета", description = "Возвращает полную историю тикета с временной статистикой по каждому статусу.")
    public ResponseEntity<ApiResponse<TicketHistoryResponse>> getTicketHistory(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                reportService.getTicketHistory(id)));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/tickets/{id}/assignments")
    @Operation(summary = "История переназначений", description = "Возвращает историю переназначений тикета с ФИО от кого и на кого.")
    public ResponseEntity<ApiResponse<List<ReassignmentHistoryResponse>>> getReassignmentHistory(
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                reportService.getReassignmentHistory(id)));
    }
}
