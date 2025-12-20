package com.bm.wschat.feature.wiki.model;

import com.bm.wschat.feature.user.model.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.proxy.HibernateProxy;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(
        name = "wiki_article_views",
        uniqueConstraints = @UniqueConstraint(columnNames = {"article_id", "user_id"})
)
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class WikiArticleView {

    @EmbeddedId
    @EqualsAndHashCode.Include
    private WikiArticleViewId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("articleId")
    private WikiArticle article;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    private User user;

    @CreationTimestamp
    private Instant viewedAt;

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy proxy ? proxy.getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy proxy ? proxy.getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        WikiArticleView that = (WikiArticleView) o;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public final int hashCode() {
        return Objects.hash(id);
    }
}
