package com.bm.wschat.feature.message.model;

import com.bm.wschat.feature.attachment.model.Attachment;
import com.bm.wschat.feature.ticket.model.Ticket;
import com.bm.wschat.feature.user.model.SenderType;
import com.bm.wschat.feature.user.model.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "messages", indexes = {
        // 1. Основной индекс — по тикету + дата (99% всех запросов)
        @Index(name = "idx_message_ticket_created", columnList = "ticket_id, created_at DESC"),

        // 2. Для пагинации "загрузить старые сообщения"
        @Index(name = "idx_message_ticket_created_id", columnList = "ticket_id, created_at DESC, id DESC"),

        // 3. Soft delete + быстрый поиск живых сообщений
        @Index(name = "idx_message_active", columnList = "ticket_id, deleted_at, created_at"),

        // 4. По отправителю (аналитика, фильтр "мои сообщения")
        @Index(name = "idx_message_sender", columnList = "sender_id"),

        // 5. По типу (системные, внутренние и т.д.)
        @Index(name = "idx_message_sender_type", columnList = "sender_type")
})
@Audited
@SQLRestriction("deleted_at IS NULL") // только живые сообщения
@SQLDelete(sql = "UPDATE messages SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id", nullable = false, updatable = false)
    private Ticket ticket;

    @NotBlank
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id")
    @NotAudited
    private User sender; // null = системное сообщение

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "sender_type", nullable = false, length = 20)
    private SenderType senderType = SenderType.USER;

    // ← ВАЖНО: внутренние сообщения (видны только специалистам)
    @Builder.Default
    @Column(name = "is_internal", nullable = false)
    private boolean internal = false;

    // ← Прочитано пользователем / специалистом
    @Column(name = "read_by_user_at")
    private Instant readByUserAt;

    @Column(name = "read_by_specialist_at")
    private Instant readBySpecialistAt;

    // ← Soft delete
    @Column(name = "deleted_at")
    private Instant deletedAt;

    // ← Для редактирования сообщений (редко, но бывает)
    @Column(name = "edited_at")
    private Instant editedAt;

    // ← Вложения
    @Builder.Default
    @OneToMany(mappedBy = "message", cascade = CascadeType.ALL, orphanRemoval = true)
    @NotAudited
    private List<Attachment> attachments = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at")
    @Builder.Default
    private Instant updatedAt = Instant.now();

    @Version
    private Long version;

    // === Удобные методы ===

    @PreUpdate
    private void preUpdate() {
        this.updatedAt = Instant.now();
        if (this.editedAt == null) {
            this.editedAt = Instant.now();
        }
    }

    public void markAsReadByUser() {
        this.readByUserAt = Instant.now();
    }

    public void markAsReadBySpecialist() {
        this.readBySpecialistAt = Instant.now();
    }

    public boolean isReadBy(User user) {
        if (user == null)
            return false;
        if (user.isSpecialist()) {
            return readBySpecialistAt != null;
        }
        return readByUserAt != null;
    }
}
