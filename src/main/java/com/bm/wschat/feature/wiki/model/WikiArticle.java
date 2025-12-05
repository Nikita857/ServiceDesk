package com.bm.wschat.feature.wiki.model;

import com.bm.wschat.feature.user.model.User;
import com.bm.wschat.shared.model.Category;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import org.hibernate.envers.RelationTargetAuditMode;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(
        name = "wiki_articles",
        indexes = {
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
                @Index(name = "idx_wiki_popular", columnList = "view_count DESC")
        }
)
@Audited
@SQLRestriction("deleted_at IS NULL")
@SQLDelete(sql = "UPDATE wiki_articles SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class WikiArticle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @NotBlank
    @Column(nullable = false, length = 250, unique = true)
    private String title;

    @NotBlank
    @Lob
    @Column(nullable = false)
    private String content; // Markdown или HTML

    // ← Slug для красивых URL: /wiki/how-to-reset-1c
    @Column(nullable = false, unique = true, length = 300)
    private String slug;

    // ← Краткое описание (для превью в поиске)
    @Column(length = 500)
    private String excerpt;

    // ← Теги (быстро и просто — через String + split)
    @Column(length = 1000)
    private String tags; // "1c, зарплата, ошибка"

    // ← Или лучше — отдельная связь @ElementCollection
    @Builder.Default
    @ElementCollection
    @CollectionTable(name = "wiki_article_tags", joinColumns = @JoinColumn(name = "article_id"))
    @Column(name = "tag")
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
    @Column(name = "view_count", nullable = false)
    private Long viewCount = 0L;

    @Builder.Default
    @Column(name = "like_count", nullable = false)
    private Long likeCount = 0L;

    // ← Soft delete
    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Version
    private Long version;

    // === Удобства ===

    @PreUpdate
    private void preUpdate() {
        this.updatedAt = Instant.now();
    }

    public void incrementView() {
        this.viewCount++;
    }

    public void incrementLike() {
        this.likeCount++;
    }

    // Для UI: "1С / Зарплата / Ошибка"
    public String getTagsAsString() {
        return tagSet != null ? String.join(" / ", tagSet) : "";
    }
}
