package com.bm.wschat.feature.friendship.model;

import com.bm.wschat.feature.user.model.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import org.hibernate.proxy.HibernateProxy;

import java.time.Instant;
import java.util.Objects;

/**
 * Friendship - запрос/связь дружбы между пользователями
 */
@Entity
@Table(name = "friendships", indexes = {
        @Index(name = "idx_friendship_requester", columnList = "requester_id"),
        @Index(name = "idx_friendship_addressee", columnList = "addressee_id"),
        @Index(name = "idx_friendship_status", columnList = "status"),
        @Index(name = "idx_friendship_pair", columnList = "requester_id, addressee_id", unique = true)
})
@Getter
@Setter
@Builder
@Audited
@NoArgsConstructor
@AllArgsConstructor
public class Friendship {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "friendship_seq")
    @SequenceGenerator(name = "friendship_seq", sequenceName = "friendships_id_seq", allocationSize = 1)
    @EqualsAndHashCode.Include
    private Long id;

    /**
     * Пользователь, отправивший запрос в друзья
     */
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_id", nullable = false)
    @NotAudited
    private User requester;

    /**
     * Пользователь, которому отправлен запрос
     */
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "addressee_id", nullable = false)
    @NotAudited
    private User addressee;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private FriendshipStatus status = FriendshipStatus.PENDING;

    @Column(name = "requested_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant requestedAt = Instant.now();

    @Column(name = "responded_at")
    private Instant respondedAt;

    /**
     * Принять запрос в друзья
     */
    public void accept() {
        this.status = FriendshipStatus.ACCEPTED;
        this.respondedAt = Instant.now();
    }

    /**
     * Отклонить запрос в друзья
     */
    public void reject() {
        this.status = FriendshipStatus.REJECTED;
        this.respondedAt = Instant.now();
    }

    /**
     * Заблокировать пользователя
     */
    public void block() {
        this.status = FriendshipStatus.BLOCKED;
        this.respondedAt = Instant.now();
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy proxy ? proxy.getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy proxy ? proxy.getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        Friendship that = (Friendship) o;
        return getId() != null && Objects.equals(getId(), that.getId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy proxy ? proxy.getHibernateLazyInitializer().getPersistentClass().hashCode() : getClass().hashCode();
    }
}
