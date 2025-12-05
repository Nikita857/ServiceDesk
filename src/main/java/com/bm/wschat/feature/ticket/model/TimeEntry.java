package com.bm.wschat.feature.ticket.model;

import com.bm.wschat.feature.user.model.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "time_entries", indexes = {
        // 1. Основной — по тикету (история времени)
        @Index(name = "idx_time_ticket_date", columnList = "ticket_id, entry_date DESC"),

        // 2. По специалисту + дата (отчёты, таймшиты)
        @Index(name = "idx_time_specialist_date", columnList = "specialist_id, entry_date DESC"),

        // 3. По дате (ежедневные/ежемесячные отчёты)
        @Index(name = "idx_time_date", columnList = "entry_date"),

        // 4. Комбинированный для аналитики: "кто сколько наработал за период"
        @Index(name = "idx_time_specialist_period", columnList = "specialist_id, entry_date"),

        // 5. Для биллинга (если платная поддержка)
        @Index(name = "idx_time_billable", columnList = "billable, entry_date")})
@SQLRestriction("deleted_at IS NULL") // soft delete (очень рекомендуется!)
@Audited
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class TimeEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id", nullable = false, updatable = false)
    private Ticket ticket;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "specialist_id", nullable = false, updatable = false)
    @NotAudited
    private User specialist;

    @NotNull
    @Positive(message = "Время должно быть положительным")
    @Column(name = "duration_seconds", nullable = false)
    private Long durationSeconds; // Long, а не long — чтобы можно было null при валидации

    @Column(length = 1000)
    private String note;

    // ← Критично: дата и время работы (не обязательно = created_at!)
    @Column(name = "entry_date", nullable = false)
    private Instant entryDate;

    // ← Для ручного ввода — можно указать дату отдельно
    @Column(name = "work_date")
    private LocalDate workDate; // например, "вчера работал 2 часа"

    // ← Биллинг: оплачивается ли клиенту?
    @Builder.Default
    @Column(nullable = false)
    private boolean billable = true;

    // ← Тип активности (для аналитики)
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "activity_type", length = 30)
    private TimeEntryType activityType = TimeEntryType.WORK;

    // ← Soft delete
    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Builder.Default
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Builder.Default
    @Column(name = "updated_at")
    private Instant updatedAt = Instant.now();

    @Version
    private Long version;

    // === Удобства ===

    @PreUpdate
    private void preUpdate() {
        this.updatedAt = Instant.now();
    }

    public String getFormattedDuration() {
        long hours = durationSeconds / 3600;
        long minutes = (durationSeconds % 3600) / 60;
        return String.format("%dh %02dm", hours, minutes);
    }
}
