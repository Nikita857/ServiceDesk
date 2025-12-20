package com.bm.wschat.feature.ticket.controller;

import com.bm.wschat.feature.ticket.dto.ticket.request.ChangeStatusRequest;
import com.bm.wschat.feature.ticket.dto.ticket.request.CreateTicketRequest;
import com.bm.wschat.feature.ticket.dto.ticket.request.UpdateTicketRequest;
import com.bm.wschat.feature.ticket.dto.ticket.response.TicketListResponse;
import com.bm.wschat.feature.ticket.dto.ticket.response.TicketResponse;
import com.bm.wschat.feature.ticket.dto.ticket.response.TicketStatusHistoryResponse;
import com.bm.wschat.feature.ticket.model.TicketStatus;
import com.bm.wschat.feature.ticket.service.TicketService;
import com.bm.wschat.feature.ticket.service.TicketTimeTrackingService;
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
@RequestMapping("/api/v1/tickets")
@RequiredArgsConstructor
@Tag(name = "Tickets", description = "Управление тикетами")
public class TicketController {

        private final TicketService ticketService;
        private final TicketTimeTrackingService timeTrackingService;

        @PostMapping
        @PreAuthorize("(hasAnyRole('USER', 'ADMIN'))")
        @Operation(summary = "Создать новый тикет", description = "Создает новый тикет от имени текущего аутентифицированного пользователя.")
        public ResponseEntity<ApiResponse<TicketResponse>> createTicket(
                        @Valid @RequestBody CreateTicketRequest request,
                        @AuthenticationPrincipal User user) {
                return ResponseEntity.status(HttpStatus.CREATED)
                                .body(ApiResponse.success("Тикет создан",
                                                ticketService.createTicket(request, user.getId())));
        }

        @GetMapping("/{id}")
        @Operation(summary = "Получить тикет по ID", description = "Возвращает полную информацию о тикете по его уникальному идентификатору (с проверкой доступа).")
        public ResponseEntity<ApiResponse<TicketResponse>> getTicket(
                        @PathVariable Long id,
                        @AuthenticationPrincipal User user) {
                return ResponseEntity.ok(ApiResponse.success(ticketService.getTicketById(id, user)));
        }

        @PutMapping("/{id}")
        @Operation(summary = "Обновить информацию о тикете", description = "Обновляет основные данные существующего тикета.")
        public ResponseEntity<ApiResponse<TicketResponse>> updateTicket(
                        @PathVariable Long id,
                        @Valid @RequestBody UpdateTicketRequest request) {
                return ResponseEntity.ok(ApiResponse.success("Тикет обновлен",
                                ticketService.updateTicket(id, request)));
        }

        @DeleteMapping("/{id}")
        @PreAuthorize("hasRole('ADMIN')")
        @Operation(summary = "Удалить тикет", description = "Удаляет тикет по его уникальному идентификатору (логическое удаление).")
        public ResponseEntity<ApiResponse<Void>> deleteTicket(@PathVariable Long id) {
                ticketService.deleteTicket(id);
                return ResponseEntity.ok(ApiResponse.success("Тикет удален"));
        }

        @PatchMapping("/{id}/status")
        @PreAuthorize("hasAnyRole('USER','SYSADMIN','1CSUPPORT','DEV1C','DEVELOPER','ADMIN')")
        @Operation(summary = "Изменить статус тикета", description = "Изменяет статус тикета. Для закрытия требуется двухфакторное подтверждение: "
                        +
                        "специалист переводит в PENDING_CLOSURE, пользователь подтверждает в CLOSED. " +
                        "Администратор может закрыть принудительно.")
        public ResponseEntity<ApiResponse<TicketResponse>> changeStatus(
                        @PathVariable Long id,
                        @AuthenticationPrincipal User user,
                        @Valid @RequestBody ChangeStatusRequest request) {
                return ResponseEntity.ok(ApiResponse.success("Статус тикета обновлен",
                                ticketService.changeStatus(id, user, request)));
        }

        /**
         * Подтвердить закрытие тикета (для пользователя-создателя).
         * Переводит тикет из PENDING_CLOSURE в CLOSED.
         */
        @PostMapping("/{id}/confirm-closure")
        @Operation(summary = "Подтвердить закрытие тикета", description = "Пользователь подтверждает закрытие тикета, который находится в статусе PENDING_CLOSURE.")
        public ResponseEntity<ApiResponse<TicketResponse>> confirmClosure(
                        @PathVariable Long id,
                        @AuthenticationPrincipal User user) {
                var request = new ChangeStatusRequest(TicketStatus.CLOSED, "Закрытие подтверждено пользователем");
                return ResponseEntity.ok(ApiResponse.success("Тикет закрыт",
                                ticketService.changeStatus(id, user, request)));
        }

        /**
         * Отклонить закрытие тикета (для пользователя-создателя).
         * Переводит тикет из PENDING_CLOSURE в REOPENED.
         */
        @PostMapping("/{id}/reject-closure")
        @Operation(summary = "Отклонить закрытие тикета", description = "Пользователь отклоняет закрытие тикета, который находится в статусе PENDING_CLOSURE. Тикет будет переоткрыт.")
        public ResponseEntity<ApiResponse<TicketResponse>> rejectClosure(
                        @PathVariable Long id,
                        @RequestParam(required = false) String reason,
                        @AuthenticationPrincipal User user) {
                String comment = reason != null ? "Закрытие отклонено: " + reason : "Закрытие отклонено пользователем";
                var request = new ChangeStatusRequest(TicketStatus.REOPENED, comment);
                return ResponseEntity.ok(ApiResponse.success("Закрытие отклонено, тикет переоткрыт",
                                ticketService.changeStatus(id, user, request)));
        }

        /**
         * Получить историю статусов тикета для учёта времени.
         */
        @GetMapping("/{id}/status-history")
        @Operation(summary = "Получить историю статусов тикета", description = "Возвращает список всех статусов тикета с временными метками для учёта времени работы.")
        public ResponseEntity<ApiResponse<List<TicketStatusHistoryResponse>>> getStatusHistory(
                        @PathVariable Long id) {
                var history = timeTrackingService.getStatusHistory(id);
                var response = history.stream()
                                .map(h -> new TicketStatusHistoryResponse(
                                                h.getId(),
                                                h.getStatus().name(),
                                                h.getEnteredAt(),
                                                h.getExitedAt(),
                                                h.getDurationSeconds(),
                                                h.getDurationFormatted(),
                                                h.getChangedBy() != null ? h.getChangedBy().getUsername() : null,
                                                h.getChangedBy() != null ? h.getChangedBy().getFio() : null,
                                                h.getComment()))
                                .toList();
                return ResponseEntity.ok(ApiResponse.success(response));
        }

        /**
         * Оценить качество обслуживания.
         * Доступно только создателю тикета после его закрытия.
         */
        @PostMapping("/{id}/rate")
        @Operation(summary = "Оценить качество обслуживания", description = "Пользователь ставит оценку от 1 до 5 и оставляет отзыв после закрытия тикета.")
        public ResponseEntity<ApiResponse<TicketResponse>> rateTicket(
                        @PathVariable Long id,
                        @Valid @RequestBody com.bm.wschat.feature.ticket.dto.ticket.request.RateTicketRequest request,
                        @AuthenticationPrincipal User user) {
                return ResponseEntity.ok(ApiResponse.success("Спасибо за оценку!",
                                ticketService.rateTicket(id, user, request.rating(), request.feedback())));
        }

        @PatchMapping("/{id}/assign-line")
        @PreAuthorize("hasAnyRole('SYSADMIN','1CSUPPORT','DEV1C','DEVELOPER','ADMIN')")
        @Operation(summary = "Назначить тикет линии поддержки", description = "Назначает тикет указанной линии поддержки.")
        public ResponseEntity<ApiResponse<TicketResponse>> assignToLine(
                        @PathVariable Long id,
                        @RequestParam Long lineId) {
                return ResponseEntity.ok(ApiResponse.success("Тикет назначен на линию",
                                ticketService.assignToLine(id, lineId)));
        }

        @PatchMapping("/{id}/assign-specialist")
        @PreAuthorize("hasAnyRole('SYSADMIN','1CSUPPORT','DEV1C','DEVELOPER','ADMIN')")
        @Operation(summary = "Назначить тикет специалисту", description = "Назначает тикет указанному специалисту.")
        public ResponseEntity<ApiResponse<TicketResponse>> assignToSpecialist(
                        @PathVariable Long id,
                        @RequestParam Long specialistId) {
                return ResponseEntity.ok(ApiResponse.success("Тикет назначен на специалиста",
                                ticketService.assignToSpecialist(id, specialistId)));
        }

        @PostMapping("/{id}/take")
        @PreAuthorize("hasAnyRole('SYSADMIN','1CSUPPORT','DEV1C','DEVELOPER','ADMIN')")
        @Operation(summary = "Взять тикет в работу", description = "Специалист берёт неназначенный тикет в работу и становится его исполнителем.")
        public ResponseEntity<ApiResponse<TicketResponse>> takeTicket(
                        @PathVariable Long id,
                        @AuthenticationPrincipal User user) {
                return ResponseEntity.ok(ApiResponse.success("Тикет взят в работу",
                                ticketService.takeTicket(id, user.getId())));
        }

        @PatchMapping("/{id}/category-user")
        @Operation(summary = "Установить пользовательскую категорию для тикета", description = "Устанавливает или изменяет пользовательскую категорию для тикета.")
        public ResponseEntity<ApiResponse<TicketResponse>> setUserCategory(
                        @PathVariable Long id,
                        @RequestParam Long categoryId) {
                return ResponseEntity.ok(ApiResponse.success("Пользовательская категория установлена",
                                ticketService.setUserCategory(id, categoryId)));
        }

        @PatchMapping("/{id}/category-support")
        @PreAuthorize("hasAnyRole('SYSADMIN','1CSUPPORT','DEV1C','DEVELOPER')")
        @Operation(summary = "Установить категорию поддержки для тикета", description = "Устанавливает или изменяет категорию поддержки для тикета.")
        public ResponseEntity<ApiResponse<TicketResponse>> setSupportCategory(
                        @PathVariable Long id,
                        @RequestParam Long categoryId) {
                return ResponseEntity.ok(ApiResponse.success("Категория по мнению поддержки установлена",
                                ticketService.setSupportCategory(id, categoryId)));
        }

        @GetMapping
        @Operation(summary = "Получить список доступных тикетов", description = "Возвращает пагинированный список тикетов, видимых текущему пользователю в зависимости от его роли.")
        public ResponseEntity<ApiResponse<Page<TicketListResponse>>> listTickets(
                        @AuthenticationPrincipal User user,
                        @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
                return ResponseEntity.ok(ApiResponse.success(
                                ticketService.getVisibleTickets(user, pageable)));
        }

        @GetMapping("/my")
        @Operation(summary = "Получить список моих тикетов", description = "Возвращает пагинированный список тикетов, созданных текущим аутентифицированным пользователем.")
        public ResponseEntity<ApiResponse<Page<TicketListResponse>>> getMyTickets(
                        @AuthenticationPrincipal User user,
                        @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
                return ResponseEntity.ok(ApiResponse.success(
                                ticketService.getMyTickets(user.getId(), pageable)));
        }

        @GetMapping("/assigned")
        @PreAuthorize("hasAnyRole('SYSADMIN','1CSUPPORT','DEV1C','DEVELOPER')")
        @Operation(summary = "Получить список назначенных мне тикетов", description = "Возвращает пагинированный список тикетов, назначенных текущему аутентифицированному специалисту.")
        public ResponseEntity<ApiResponse<Page<TicketListResponse>>> getAssignedTickets(
                        @AuthenticationPrincipal User user,
                        @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
                return ResponseEntity.ok(ApiResponse.success(
                                ticketService.getAssignedTickets(user.getId(), pageable)));
        }

        @GetMapping("/status/{status}")
        @Operation(summary = "Получить список тикетов по статусу", description = "Возвращает пагинированный список тикетов с указанным статусом.")
        public ResponseEntity<ApiResponse<Page<TicketListResponse>>> getTicketsByStatus(
                        @PathVariable TicketStatus status,
                        @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
                return ResponseEntity.ok(ApiResponse.success(
                                ticketService.getTicketsByStatus(status, pageable)));
        }

        @GetMapping("/line/{lineId}")
        @Operation(summary = "Получить список тикетов по линии поддержки", description = "Возвращает пагинированный список тикетов, относящихся к указанной линии поддержки.")
        public ResponseEntity<ApiResponse<Page<TicketListResponse>>> getTicketsByLine(
                        @PathVariable Long lineId,
                        @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
                return ResponseEntity.ok(ApiResponse.success(
                                ticketService.getTicketsByLine(lineId, pageable)));
        }
}
