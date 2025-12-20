package com.bm.wschat.feature.ticket.model;

import com.bm.wschat.feature.user.model.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Duration;
import java.time.Instant;

/**
 * История статусов тикета для учёта времени.
 * Каждая запись фиксирует время входа в статус и выхода из него.
 */
@Entity
@Table(name = "ticket_status_history", indexes = {
        @Index(name = "idx_status_history_ticket", columnList = "ticket_id"),
        @Index(name = "idx_status_history_ticket_status", columnList = "ticket_id, status"),
        @Index(name = "idx_status_history_entered", columnList = "entered_at")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticket_id", nullable = false)
    private Ticket ticket;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TicketStatus status;

    /** Время входа в статус */
    @Column(name = "entered_at", nullable = false)
    @Builder.Default
    private Instant enteredAt = Instant.now();

    /** Время выхода из статуса (null если текущий статус) */
    @Column(name = "exited_at")
    private Instant exitedAt;

    /** Время в статусе в секундах (заполняется при выходе) */
    @Column(name = "duration_seconds")
    private Long durationSeconds;

    /** Кто сменил статус */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "changed_by_id")
    private User changedBy;

    /** Комментарий к смене статуса */
    @Column(columnDefinition = "TEXT")
    private String comment;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    /**
     * Закрыть текущую запись (выход из статуса).
     * Автоматически вычисляет duration.
     */
    public void close() {
        if (this.exitedAt == null) {
            this.exitedAt = Instant.now();
            this.durationSeconds = Duration.between(enteredAt, exitedAt).getSeconds();
        }
    }

    /**
     * Получить длительность в формате "Xч Yм Zс".
     */
    public String getDurationFormatted() {
        if (durationSeconds == null) {
            return "-";
        }
        long hours = durationSeconds / 3600;
        long minutes = (durationSeconds % 3600) / 60;
        long seconds = durationSeconds % 60;

        if (hours > 0) {
            return String.format("%dч %dм %dс", hours, minutes, seconds);
        } else if (minutes > 0) {
            return String.format("%dм %dс", minutes, seconds);
        } else {
            return String.format("%dс", seconds);
        }
    }
}
