package com.bm.wschat.feature.wiki.model;

import com.bm.wschat.feature.user.model.User;
import com.bm.wschat.shared.model.Category;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import org.hibernate.proxy.HibernateProxy;

import java.time.Instant;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "wiki_articles", indexes = {
        // 1. Поиск по заголовку
        @Index(name = "idx_wiki_title", columnList = "title"),

        // 2. Поиск по содержимому (для полнотекстового поиска)
        @Index(name = "idx_wiki_content_fts", columnList = "content"), // только PostgreSQL!

        // 3. Soft delete + быстрый доступ
        @Index(name = "idx_wiki_active", columnList = "deleted_at, id"),

        // 4. По автору
        @Index(name = "idx_wiki_author", columnList = "created_by_id"),

        // 5. По дате обновления (главная страница — "недавно изменённые")
        @Index(name = "idx_wiki_updated", columnList = "updated_at DESC"),

        // 6. По тегам (если используешь отдельную таблицу — см. ниже)
        @Index(name = "idx_wiki_popular", columnList = "views_total DESC")
})
@Audited
@SQLRestriction("deleted_at IS NULL")
@SQLDelete(sql = "UPDATE wiki_articles SET deleted_at = CURRENT_TIMESTAMP WHERE id = ? AND version = ?")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WikiArticle {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "wiki_seq")
    @SequenceGenerator(name = "wiki_seq", sequenceName = "wiki_articles_id_seq", allocationSize = 1)
    @EqualsAndHashCode.Include
    private Long id;

    @NotBlank
    @Column(nullable = false, length = 250, unique = true)
    private String title;

    @NotBlank
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content; // Markdown или HTML

    // ← Slug для красивых URL: /wiki/how-to-reset-1c
    @Column(nullable = false, unique = true, length = 300)
    private String slug;

    // ← Краткое описание (для превью в поиске)
    @Column(length = 500)
    private String excerpt;

    @Builder.Default
    @ElementCollection
    @CollectionTable(name = "wiki_article_tags", joinColumns = @JoinColumn(name = "article_id"))
    @Column(name = "tag", nullable = false)
    @BatchSize(size = 50)
    private Set<String> tagSet = new HashSet<>();

    // ← Категория (если есть иерархия)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id", nullable = false, updatable = false)
    @NotAudited
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by_id")
    @NotAudited
    private User updatedBy;

    @Builder.Default
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Builder.Default
    @Column(name = "updated_at")
    private Instant updatedAt = Instant.now();

    // ← Статистика
    @Builder.Default
    @NotAudited
    @OneToMany(mappedBy = "article", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<WikiArticleView> views = new HashSet<>();

    @Builder.Default
    @Column(name = "views_total", nullable = false)
    private Long viewsTotal = 0L;

    // ← Soft delete
    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Version
    private Long version;

    @PrePersist
    private void onCreate() {
        this.setViewsTotal(0L);
    }

    @PreUpdate
    private void preUpdate() {
        this.updatedAt = Instant.now();
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy proxy ? proxy.getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy proxy ? proxy.getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        WikiArticle that = (WikiArticle) o;
        return getId() != null && Objects.equals(getId(), that.getId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy proxy ? proxy.getHibernateLazyInitializer().getPersistentClass().hashCode() : getClass().hashCode();
    }
}
