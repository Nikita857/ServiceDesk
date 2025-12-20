package com.bm.wschat.shared.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.envers.Audited;
import org.hibernate.proxy.HibernateProxy;

import java.time.Instant;
import java.util.Objects;

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
@SQLRestriction("deleted_at IS NULL")
@SQLDelete(sql = "UPDATE categories SET deleted_at = CURRENT_TIMESTAMP WHERE id = ? AND version = ?")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Category {

    @Id
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "category_seq"
    )
    @SequenceGenerator(
            name = "category_seq",
            sequenceName = "categories_id_seq",
            allocationSize = 1
    )
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

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy proxy ? proxy.getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy proxy ? proxy.getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        Category category = (Category) o;
        return getId() != null && Objects.equals(getId(), category.getId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy proxy ? proxy.getHibernateLazyInitializer().getPersistentClass().hashCode() : getClass().hashCode();
    }
}
