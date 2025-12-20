package com.bm.wschat.feature.user.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "user_activity_logs", indexes = {
        @Index(name = "idx_user_activity_user", columnList = "user_id"),
        @Index(name = "idx_user_activity_event_time", columnList = "eventTime")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserActivityLog {

    @Id
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "user_activity_log_seq"
    )
    @SequenceGenerator(
            name = "user_activity_log_seq",
            sequenceName = "user_activity_log_id_seq",
            allocationSize = 1
    )
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private UserActivityEventType eventType;

    @Column(name = "event_time", nullable = false, updatable = false)
    private Instant eventTime;

    @PrePersist
    protected void onCreate() {
        eventTime = Instant.now();
    }
}
