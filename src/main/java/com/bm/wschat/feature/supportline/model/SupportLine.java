package com.bm.wschat.feature.supportline.model;

import com.bm.wschat.feature.ticket.model.AssignmentMode;
import com.bm.wschat.feature.user.model.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(
        name = "support_lines",
        indexes = {
                @Index(name = "idx_support_line_name", columnList = "name"),
                @Index(name = "idx_support_line_active", columnList = "deleted_at")
        },
        uniqueConstraints = @UniqueConstraint(columnNames = {"name", "deleted_at"})
)
@Audited
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupportLine {

    @Id
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "support_line_seq"
    )
    @SequenceGenerator(
            name = "support_line_seq",
            sequenceName = "support_lines_id_seq",
            allocationSize = 1
    )
    private Long id;

    @Column(nullable = false, length = 100, unique = true)
    private String name; // "1-я линия", "Бухгалтерия", "Разработка"

    @Column(length = 500)
    private String description;

    // SLA в минутах
    @Builder.Default
    @Column(name = "sla_minutes")
    private Integer slaMinutes = 1440; // 24 часа по умолчанию

    // Режим распределения
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AssignmentMode assignmentMode = AssignmentMode.FIRST_AVAILABLE;

    // Специалисты в этой линии
    @Builder.Default
    @ManyToMany
    @JoinTable(
            name = "support_line_specialists",
            joinColumns = @JoinColumn(name = "line_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    @NotAudited
    private Set<User> specialists = new HashSet<>();

    // Для round-robin
    @Builder.Default
    @Column(name = "last_assigned_index")
    private Integer lastAssignedIndex = 0;

    // Порядок отображения
    @Builder.Default
    @Column(name = "display_order")
    private Integer displayOrder = 100;

    @Column(name = "telegram_chat_id")
    private Long telegramChatId;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at")
    @Builder.Default
    private Instant updatedAt = Instant.now();

    @Version
    private Long version;

    @PreUpdate
    private void preUpdate() {
        this.updatedAt = Instant.now();
    }
}
