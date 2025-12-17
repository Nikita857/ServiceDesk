package com.bm.wschat.feature.wiki.repository;

import com.bm.wschat.feature.user.model.User;
import com.bm.wschat.feature.wiki.model.WikiArticle;
import com.bm.wschat.feature.wiki.model.WikiArticleView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface WikiArticleViewRepository extends JpaRepository<WikiArticleView, Long> {
    //Самописный метод для обновления счетчика просмотров (не изменяет дату обновления статьи)
    //Передаем в него ID статьи
    @Modifying
    @Query("UPDATE WikiArticle a SET a.viewsTotal = a.viewsTotal + 1 WHERE a.id = :id")
    void incrementViewsTotal(@Param("id") Long id);
    boolean existsByArticleAndUser(WikiArticle article, User user);
}
