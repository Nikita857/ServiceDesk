package com.bm.wschat.shared.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.envers.Audited;

import java.time.Instant;

@Entity
@Table(
        name = "categories",
        indexes = {
                @Index(name = "idx_category_name", columnList = "name"),
                @Index(name = "idx_category_deleted", columnList = "deleted_at")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_category_name_deleted", columnNames = {"name", "deleted_at"})
                // позволяет восстанавливать удалённые категории с тем же именем
        }
)
@Audited
@SQLRestriction("deleted_at IS NULL")                                   // soft delete
@SQLDelete(sql = "UPDATE categories SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@Getter @Setter
@Builder @NoArgsConstructor @AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @NotBlank
    @Column(nullable = false, length = 150, unique = true)
    private String name;

    @Column(length = 500)
    private String description;

    // ← Добавь тип категории — это критично!
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CategoryType type = CategoryType.GENERAL;

    // ← Приоритет/порядок отображения
    @Builder.Default
    @Column(name = "display_order")
    private Integer displayOrder = 100;

    // ← Можно ли пользователю выбирать эту категорию?
    @Builder.Default
    @Column(name = "user_selectable", nullable = false)
    private boolean userSelectable = true;

    // ← Soft delete
    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Version
    private Long version;

    @Builder.Default
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Builder.Default
    @Column(name = "updated_at")
    private Instant updatedAt = Instant.now();

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }
}
