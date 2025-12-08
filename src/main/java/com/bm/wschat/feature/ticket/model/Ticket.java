package com.bm.wschat.feature.ticket.model;

import com.bm.wschat.feature.attachment.model.Attachment;
import com.bm.wschat.feature.message.model.Message;
import com.bm.wschat.feature.supportline.model.SupportLine;
import com.bm.wschat.feature.user.model.User;

import com.bm.wschat.shared.model.Category;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Entity
@Audited
@Table(name = "tickets", indexes = {
        @Index(name = "idx_ticket_status", columnList = "status"),
        @Index(name = "idx_ticket_created_at", columnList = "created_at"),
        @Index(name = "idx_ticket_assigned_to", columnList = "assigned_to_id"),
        @Index(name = "idx_ticket_support_line", columnList = "support_line_id"),
        @Index(name = "idx_ticket_deleted", columnList = "deleted_at"), // для soft delete
        @Index(name = "idx_ticket_telegram_thread", columnList = "telegram_message_thread_id")
})
@SQLRestriction("deleted_at IS NULL")
@SQLDelete(sql = "UPDATE tickets SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false, length = 250)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "link_1c", length = 1000)
    private String link1c;

    @ManyToOne(optional = false)
    @JoinColumn(name = "created_by_id", nullable = false)
    @NotAudited
    private User createdBy;

    @ManyToOne
    @JoinColumn(name = "assigned_to_id")
    @NotAudited
    private User assignedTo;

    @ManyToOne
    @JoinColumn(name = "support_line_id")
    private SupportLine supportLine;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TicketStatus status = TicketStatus.NEW;

    @ManyToOne
    @JoinColumn(name = "category_user_id")
    private Category categoryUser;

    @ManyToOne
    @JoinColumn(name = "category_support_id")
    private Category categorySupport;

    @Column(name = "time_spent_seconds")
    @Builder.Default
    private Long timeSpentSeconds = 0L;

    @Builder.Default
    @OneToMany(mappedBy = "ticket", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Message> messages = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "ticket", cascade = CascadeType.ALL, orphanRemoval = true)
    @NotAudited
    private List<Attachment> attachments = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "ticket", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Assignment> assignments = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "ticket", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TimeEntry> timeEntries = new ArrayList<>();

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TicketPriority priority = TicketPriority.MEDIUM;

    private Instant slaDeadline;
    private Instant resolvedAt;
    private Instant closedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    private Integer rating;
    private String feedback;

    @Column(name = "telegram_message_thread_id")
    private Long telegramMessageThreadId;

    @Column(name = "telegram_last_bot_message_id")
    private Long telegramLastBotMessageId;

    @Builder.Default
    private boolean escalated = false;

    @Version
    private Long version;

    @Builder.Default
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Builder.Default
    @Column(name = "updated_at")
    private Instant updatedAt = Instant.now();

    public void addTime(long seconds) {
        if (seconds <= 0)
            return;
        this.timeSpentSeconds = Optional.ofNullable(this.timeSpentSeconds).orElse(0L) + seconds;
    }

    public void touchUpdated() {
        this.updatedAt = Instant.now();
    }
}