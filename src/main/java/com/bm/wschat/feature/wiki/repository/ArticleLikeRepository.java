package com.bm.wschat.feature.wiki.repository;

import com.bm.wschat.feature.user.model.User;
import com.bm.wschat.feature.wiki.model.ArticleLike;
import com.bm.wschat.feature.wiki.model.WikiArticle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ArticleLikeRepository extends JpaRepository<ArticleLike, Long> {
    // Проверка, поставил ли пользователь лайк на статью
    Optional<ArticleLike> findByArticleAndUser(WikiArticle wikiArticle, User user);

    // Подсчет количества лайков для статьи
    long countByArticle(WikiArticle wikiArticle);

    // Удаление лайка пользователя по статье
    void deleteByArticleAndUser(WikiArticle wikiArticle, User user);

    // Проверка, если ли лайк по конкретной статье и пользователю (дополнительный метод для упрощения работы)
    boolean existsByArticleAndUser(WikiArticle wikiArticle, User user);
}
