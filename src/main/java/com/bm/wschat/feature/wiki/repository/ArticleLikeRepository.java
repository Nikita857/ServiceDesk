package com.bm.wschat.feature.wiki.repository;

import com.bm.wschat.feature.user.model.User;
import com.bm.wschat.feature.wiki.model.ArticleLike;
import com.bm.wschat.feature.wiki.model.WikiArticle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ArticleLikeRepository extends JpaRepository<ArticleLike, Long> {
    // Проверка, поставил ли пользователь лайк на статью
    Optional<ArticleLike> findByArticleAndUser(WikiArticle wikiArticle, User user);

    // Подсчет количества лайков для статьи
    long countByArticle(WikiArticle wikiArticle);

    // Подсчет количества лайков по ID статьи
    @Query("SELECT COUNT(l) FROM ArticleLike l WHERE l.article.id = :articleId")
    long countByArticleId(@Param("articleId") Long articleId);

    // Проверка лайка по ID статьи и пользователя
    @Query("SELECT CASE WHEN COUNT(l) > 0 THEN true ELSE false END FROM ArticleLike l WHERE l.article.id = :articleId AND l.user.id = :userId")
    boolean existsByArticleIdAndUserId(@Param("articleId") Long articleId, @Param("userId") Long userId);

    // Удаление лайка пользователя по статье
    void deleteByArticleAndUser(WikiArticle wikiArticle, User user);

    // Проверка, есть ли лайк по конкретной статье и пользователю (дополнительный
    // метод для упрощения работы)
    boolean existsByArticleAndUser(WikiArticle wikiArticle, User user);
}
