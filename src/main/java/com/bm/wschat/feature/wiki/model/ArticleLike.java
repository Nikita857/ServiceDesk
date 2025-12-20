package com.bm.wschat.feature.wiki.model;

import com.bm.wschat.feature.user.model.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.proxy.HibernateProxy;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(
        name = "wiki_article_likes",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_article_like",
                columnNames = {"article_id", "user_id"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ArticleLike {

    @Id
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "likes_seq"
    )
    @SequenceGenerator(
            name = "likes_seq",
            sequenceName = "likes_id_seq",
            allocationSize = 1
    )
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "article_id", nullable = false)
    private WikiArticle article;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy proxy ? proxy.getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy proxy ? proxy.getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        ArticleLike that = (ArticleLike) o;
        return getId() != null && Objects.equals(getId(), that.getId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy proxy ? proxy.getHibernateLazyInitializer().getPersistentClass().hashCode() : getClass().hashCode();
    }
}
