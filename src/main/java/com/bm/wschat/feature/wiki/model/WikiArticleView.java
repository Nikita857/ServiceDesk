package com.bm.wschat.feature.wiki.model;

import com.bm.wschat.feature.user.model.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(
        name = "wiki_article_views",
        uniqueConstraints = @UniqueConstraint(columnNames = {"article_id", "user_id"})
)
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
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
}
