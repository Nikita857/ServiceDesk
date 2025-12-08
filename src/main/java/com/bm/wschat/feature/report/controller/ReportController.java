package com.bm.wschat.feature.report.controller;

import com.bm.wschat.feature.report.dto.response.*;
import com.bm.wschat.feature.report.service.ReportService;
import com.bm.wschat.shared.common.ApiResponse;
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
@PreAuthorize("hasAnyRole('SPECIALIST', 'ADMIN')")
public class ReportController {

    private final ReportService reportService;

    // =====================================================================
    // TIME REPORTS
    // =====================================================================

    /**
     * Отчет по времени сгруппированный по специалистам
     * GET /api/v1/reports/time/by-specialist?from=2024-01-01&to=2024-01-31
     */
    @GetMapping("/time/by-specialist")
    public ResponseEntity<ApiResponse<List<TimeReportBySpecialistResponse>>> getTimeReportBySpecialist(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(ApiResponse.success(
                reportService.getTimeReportBySpecialist(from, to)));
    }

    /**
     * Отчет по времени сгруппированный по линиям поддержки
     * GET /api/v1/reports/time/by-line?from=2024-01-01&to=2024-01-31
     */
    @GetMapping("/time/by-line")
    public ResponseEntity<ApiResponse<List<TimeReportByLineResponse>>> getTimeReportByLine(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(ApiResponse.success(
                reportService.getTimeReportByLine(from, to)));
    }

    // =====================================================================
    // TICKET STATISTICS
    // =====================================================================

    /**
     * Статистика тикетов по статусам
     * GET /api/v1/reports/tickets/by-status
     */
    @GetMapping("/tickets/by-status")
    public ResponseEntity<ApiResponse<List<TicketStatsByStatusResponse>>> getTicketStatsByStatus() {
        return ResponseEntity.ok(ApiResponse.success(
                reportService.getTicketStatsByStatus()));
    }

    /**
     * Статистика тикетов по категориям пользователей
     * GET /api/v1/reports/tickets/by-user-category
     */
    @GetMapping("/tickets/by-user-category")
    public ResponseEntity<ApiResponse<List<TicketStatsByCategoryResponse>>> getTicketStatsByUserCategory() {
        return ResponseEntity.ok(ApiResponse.success(
                reportService.getTicketStatsByUserCategory()));
    }

    /**
     * Статистика тикетов по категориям поддержки
     * GET /api/v1/reports/tickets/by-support-category
     */
    @GetMapping("/tickets/by-support-category")
    public ResponseEntity<ApiResponse<List<TicketStatsByCategoryResponse>>> getTicketStatsBySupportCategory() {
        return ResponseEntity.ok(ApiResponse.success(
                reportService.getTicketStatsBySupportCategory()));
    }

    /**
     * Статистика по времени решения тикетов
     * GET /api/v1/reports/tickets/resolution-time
     */
    @GetMapping("/tickets/resolution-time")
    public ResponseEntity<ApiResponse<ResolutionTimeResponse>> getResolutionTimeStats() {
        return ResponseEntity.ok(ApiResponse.success(
                reportService.getResolutionTimeStats()));
    }

    // =====================================================================
    // SPECIALIST WORKLOAD
    // =====================================================================

    /**
     * Загрузка специалистов
     * GET /api/v1/reports/specialists/workload
     */
    @GetMapping("/specialists/workload")
    public ResponseEntity<ApiResponse<List<SpecialistWorkloadResponse>>> getSpecialistWorkload() {
        return ResponseEntity.ok(ApiResponse.success(
                reportService.getSpecialistWorkload()));
    }
}
