package com.bm.wschat.feature.attachment.model;

import com.bm.wschat.feature.dm.model.DirectMessage;
import com.bm.wschat.feature.message.model.Message;
import com.bm.wschat.feature.ticket.model.Ticket;
import com.bm.wschat.feature.user.model.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.Instant;

@Entity
@Table(name = "attachments", indexes = {
        // 1. Основной — по тикету (самый частый запрос)
        @Index(name = "idx_attachment_ticket", columnList = "ticket_id"),

        // 2. По сообщению (загрузка вложений к сообщению)
        @Index(name = "idx_attachment_message", columnList = "message_id"),

        // 3. По личному сообщению
        @Index(name = "idx_attachment_dm", columnList = "direct_message_id"),

        // 4. Soft delete + быстрый доступ к живым
        @Index(name = "idx_attachment_active", columnList = "ticket_id, deleted_at"),

        // 5. По загрузившему (аналитика, "мои вложения")
        @Index(name = "idx_attachment_uploader", columnList = "uploaded_by_id"),

        // 6. По типу (фильтр "только фото")
        @Index(name = "idx_attachment_type", columnList = "type") })
@SQLRestriction("deleted_at IS NULL")
@SQLDelete(sql = "UPDATE attachments SET deleted_at = CURRENT_TIMESTAMP WHERE id = ? AND version = ?")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Attachment {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "attachment_seq")
    @SequenceGenerator(name = "attachment_seq", sequenceName = "attachments_id_seq", allocationSize = 1)
    @EqualsAndHashCode.Include
    private Long id;

    // ← Один из двух: либо к тикету, либо к сообщению (но не оба!)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id")
    private Ticket ticket;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id", nullable = true)
    private Message message;

    // Личное сообщение (DirectMessage)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "direct_message_id")
    private DirectMessage directMessage;

    // ← Ограничение: только один из трёх не null
    @Transient
    public boolean isValid() {
        int count = 0;
        if (ticket != null)
            count++;
        if (message != null)
            count++;
        if (directMessage != null)
            count++;
        return count == 1;
    }

    @NotBlank
    @Column(nullable = false, length = 255)
    private String filename;

    @NotBlank
    @Column(nullable = false, length = 2000)
    private String url; // S3, MinIO, Telegram file_id, локальный путь

    // ← Размер файла (для UI и ограничений)
    @Column(name = "file_size_bytes")
    private Long fileSize;

    // ← MIME-тип (image/png, application/pdf и т.д.)
    @Column(name = "mime_type", length = 100)
    private String mimeType;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AttachmentType type = AttachmentType.SCREENSHOT;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by_id")
    private User uploadedBy;

    // ← Soft delete
    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Version
    private Long version;

    // === Удобства ===

    public boolean isImage() {
        return type == AttachmentType.PHOTO || type == AttachmentType.SCREENSHOT;
    }

    public boolean isFromTelegram() {
        return url != null && url.startsWith("telegram:");
    }
}