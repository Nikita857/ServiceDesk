package com.bm.wschat.feature.ticket.controller;

import com.bm.wschat.feature.supportline.model.SupportLine;
import com.bm.wschat.feature.supportline.repository.SupportLineRepository;
import com.bm.wschat.feature.ticket.dto.ticket.response.LineTicketStatsResponse;
import com.bm.wschat.feature.ticket.dto.ticket.response.UserTicketStatsResponse;
import com.bm.wschat.feature.ticket.service.TicketStatsService;
import com.bm.wschat.feature.user.model.User;
import com.bm.wschat.shared.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Контроллер для статистики тикетов.
 * 
 * Бизнес-логика доступа:
 * - Все пользователи: своя статистика (/my)
 * - Специалисты (DEVELOPER, SYSADMIN, DEV1C): статистика по своим линиям
 * - ADMIN: любая статистика
 */
@RestController
@RequestMapping("/api/v1/stats/tickets")
@RequiredArgsConstructor
@Tag(name = "Ticket Statistics", description = "Статистика тикетов")
public class TicketStatsController {

    private final TicketStatsService ticketStatsService;
    private final SupportLineRepository supportLineRepository;

    // === User Statistics (доступно всем) ===

    @GetMapping("/my")
    @Operation(summary = "Моя статистика тикетов", description = "Возвращает статистику по тикетам, созданным текущим пользователем.")
    public ResponseEntity<ApiResponse<UserTicketStatsResponse>> getMyStats(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success(ticketStatsService.getMyStats(user)));
    }

    // === Line Statistics (специалисты — свои линии, ADMIN — все) ===

    @GetMapping("/by-line")
    @Operation(summary = "Статистика по линиям", description = "Специалисты видят только свои линии, ADMIN — все.")
    public ResponseEntity<ApiResponse<List<LineTicketStatsResponse>>> getStatsByAllLines(
            @AuthenticationPrincipal User user) {

        // ADMIN видит все линии
        if (user.isAdmin()) {
            return ResponseEntity.ok(ApiResponse.success(ticketStatsService.getStatsForAllLines()));
        }

        // Специалист видит только свои линии
        if (user.isSpecialist()) {
            List<SupportLine> userLines = supportLineRepository.findBySpecialist(user);
            List<LineTicketStatsResponse> stats = userLines.stream()
                    .filter(line -> line.getDeletedAt() == null)
                    .map(line -> ticketStatsService.getStatsForLine(line.getId()))
                    .toList();
            return ResponseEntity.ok(ApiResponse.success(stats));
        }

        throw new AccessDeniedException("У вас нет доступа к статистике линий");
    }

    @GetMapping("/by-line/{lineId}")
    @Operation(summary = "Статистика для линии", description = "Специалисты могут видеть только свои линии, ADMIN — любую.")
    public ResponseEntity<ApiResponse<LineTicketStatsResponse>> getStatsByLine(
            @PathVariable Long lineId,
            @AuthenticationPrincipal User user) {

        // ADMIN может смотреть любую линию
        if (user.isAdmin()) {
            return ResponseEntity.ok(ApiResponse.success(ticketStatsService.getStatsForLine(lineId)));
        }

        // Специалист - проверяем что он в этой линии
        if (user.isSpecialist()) {
            List<SupportLine> userLines = supportLineRepository.findBySpecialist(user);
            boolean hasAccess = userLines.stream()
                    .anyMatch(line -> line.getId().equals(lineId));

            if (hasAccess) {
                return ResponseEntity.ok(ApiResponse.success(ticketStatsService.getStatsForLine(lineId)));
            }
        }

        throw new AccessDeniedException("У вас нет доступа к статистике этой линии");
    }

    // === Global Statistics (только ADMIN) ===

    @GetMapping("/global")
    @Operation(summary = "Глобальная статистика", description = "Общая статистика по всем тикетам. Только для ADMIN.")
    public ResponseEntity<ApiResponse<UserTicketStatsResponse>> getGlobalStats(
            @AuthenticationPrincipal User user) {

        if (!user.isAdmin()) {
            throw new AccessDeniedException("Только администратор может видеть глобальную статистику");
        }

        return ResponseEntity.ok(ApiResponse.success(ticketStatsService.getGlobalStats()));
    }
}
