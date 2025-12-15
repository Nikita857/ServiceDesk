package com.bm.wschat.feature.wiki.repository;

import com.bm.wschat.feature.wiki.model.WikiArticle;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WikiArticleRepository extends JpaRepository<WikiArticle, Long> {

    // По slug
    Optional<WikiArticle> findBySlug(String slug);

    boolean existsBySlug(String slug);

    boolean existsByTitle(String title);

    // С категорией и тегами
    @Query("SELECT a FROM WikiArticle a LEFT JOIN FETCH a.category LEFT JOIN FETCH a.createdBy LEFT JOIN FETCH a.tagSet WHERE a.slug = :slug")
    Optional<WikiArticle> findBySlugWithDetails(@Param("slug") String slug);

    // Все статьи (с подгрузкой связей)
    @EntityGraph(attributePaths = { "category", "createdBy", "tagSet" })
    Page<WikiArticle> findAllByOrderByUpdatedAtDesc(Pageable pageable);

    // По категории
    @EntityGraph(attributePaths = { "category", "createdBy", "tagSet" })
    Page<WikiArticle> findByCategoryIdOrderByUpdatedAtDesc(Long categoryId, Pageable pageable);

    // Популярные
    @EntityGraph(attributePaths = { "category", "createdBy", "tagSet" })
    Page<WikiArticle> findAllByOrderByViewCountDesc(Pageable pageable);

    // Поиск по title и content
    @EntityGraph(attributePaths = { "category", "createdBy", "tagSet" })
    @Query("SELECT a FROM WikiArticle a WHERE " +
            "LOWER(a.title) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(a.content) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "ORDER BY a.viewCount DESC")
    Page<WikiArticle> search(@Param("query") String query, Pageable pageable);

    // Поиск по тегам
    @Query("SELECT DISTINCT a FROM WikiArticle a JOIN a.tagSet t WHERE LOWER(t) = LOWER(:tag)")
    List<WikiArticle> findByTag(@Param("tag") String tag);

    // Инкремент просмотров
    @Modifying
    @Query("UPDATE WikiArticle a SET a.viewCount = a.viewCount + 1 WHERE a.id = :id")
    void incrementViewCount(@Param("id") Long id);

    // Статьи автора
    @EntityGraph(attributePaths = { "category", "createdBy", "tagSet" })
    Page<WikiArticle> findByCreatedByIdOrderByUpdatedAtDesc(Long userId, Pageable pageable);
}
