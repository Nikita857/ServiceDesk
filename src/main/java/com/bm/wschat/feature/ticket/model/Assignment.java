package com.bm.wschat.feature.ticket.model;

import com.bm.wschat.feature.supportline.model.SupportLine;
import com.bm.wschat.feature.user.model.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import org.hibernate.proxy.HibernateProxy;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(
        name = "assignments",
        indexes = {
                // 1. Основной — по тикету (история назначений)
                @Index(name = "idx_assignment_ticket_created", columnList = "ticket_id, created_at DESC"),

                // 2. Кому назначено сейчас (активные назначения)
                @Index(
                        name = "idx_assignment_pending",
                        columnList = "to_user_id",
                        // ← Hibernate 6+ поддерживает это!
                        unique = false
                ),

                // 3. По линии (откуда/куда)
                @Index(name = "idx_assignment_lines", columnList = "from_line_id, to_line_id"),

                // 4. Поиск "мои текущие тикеты"
                @Index(name = "idx_assignment_active_user", columnList = "to_user_id, accepted_at"),

                // 5. Для аналитики "кто сколько взял"
                @Index(name = "idx_assignment_accepted", columnList = "accepted_at")
        }
)
@Audited
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Assignment {

    @Id
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "assignment_seq"
    )
    @SequenceGenerator(
            name = "assignment_seq",
            sequenceName = "assignments_id_seq",
            allocationSize = 1
    )
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id", nullable = false, updatable = false)
    private Ticket ticket;

    // Откуда
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_line_id")
    private SupportLine fromLine;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_user_id")
    @NotAudited
    private User fromUser;

    // Куда
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_line_id")
    private SupportLine toLine;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_user_id")
    @NotAudited
    private User toUser; // null = назначено на линию, не на конкретного

    @Column(length = 1000)
    private String note; // "Сложный кейс — нужна 2-я линия", "Клиент просил Петрова"

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AssignmentMode mode = AssignmentMode.FIRST_AVAILABLE;

    // ← КРИТИЧНО: статус назначения
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AssignmentStatus status = AssignmentStatus.PENDING;

    @Builder.Default
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "accepted_at")
    private Instant acceptedAt;

    @Column(name = "rejected_at")
    private Instant rejectedAt;

    @Column(name = "rejected_reason", length = 500)
    private String rejectedReason;

    // ← Soft delete (если нужно скрывать старые назначения)
    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Version
    private Long version;

    // === Удобные методы ===

    public boolean isPending() { return status == AssignmentStatus.PENDING; }
    public boolean isAccepted() { return status == AssignmentStatus.ACCEPTED; }
    public boolean isRejected() { return status == AssignmentStatus.REJECTED; }

    public void accept() {
        this.status = AssignmentStatus.ACCEPTED;
        this.acceptedAt = Instant.now();
    }

    public void reject(String reason) {
        this.status = AssignmentStatus.REJECTED;
        this.rejectedAt = Instant.now();
        this.rejectedReason = reason;
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy proxy ? proxy.getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy proxy ? proxy.getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        Assignment that = (Assignment) o;
        return getId() != null && Objects.equals(getId(), that.getId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy proxy ? proxy.getHibernateLazyInitializer().getPersistentClass().hashCode() : getClass().hashCode();
    }
}
