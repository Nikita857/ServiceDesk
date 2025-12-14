package com.bm.wschat.feature.friendship.model;

import com.bm.wschat.feature.user.model.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

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
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
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
    private User requester;

    /**
     * Пользователь, которому отправлен запрос
     */
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "addressee_id", nullable = false)
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
}
