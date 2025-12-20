package com.bm.wschat.feature.dm.model;

import com.bm.wschat.feature.user.model.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.proxy.HibernateProxy;

import java.time.Instant;
import java.util.Objects;

/**
 * Direct Message - личное сообщение между двумя пользователями
 */
@Entity
@Table(name = "direct_messages", indexes = {
        @Index(name = "idx_dm_sender_recipient", columnList = "sender_id, recipient_id, created_at DESC"),
        @Index(name = "idx_dm_conversation", columnList = "sender_id, recipient_id"),
        @Index(name = "idx_dm_recipient_unread", columnList = "recipient_id, read_at")
})
@SQLRestriction("deleted_at IS NULL")
@SQLDelete(sql = "UPDATE direct_messages SET deleted_at = CURRENT_TIMESTAMP WHERE id = ? and version = ?")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DirectMessage {

    @Id
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "direct_msg_seq"
    )
    @SequenceGenerator(
            name = "direct_msg_seq",
            sequenceName = "direct_messages_id_seq",
            allocationSize = 1
    )
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_id", nullable = false)
    private User recipient;

    @NotBlank
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "read_at")
    private Instant readAt;

    @Column(name = "edited_at")
    private Instant editedAt;

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

    @PreUpdate
    private void preUpdate() {
        this.updatedAt = Instant.now();
    }

    public void markAsRead() {
        if (this.readAt == null) {
            this.readAt = Instant.now();
        }
    }

    public boolean isRead() {
        return readAt != null;
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy proxy ? proxy.getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy proxy ? proxy.getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        DirectMessage that = (DirectMessage) o;
        return getId() != null && Objects.equals(getId(), that.getId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy proxy ? proxy.getHibernateLazyInitializer().getPersistentClass().hashCode() : getClass().hashCode();
    }
}
