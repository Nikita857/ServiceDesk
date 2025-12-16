-- V2__wiki_article_views_and_views_total.sql

-- =====================================================
-- 1. Создаём таблицу для уникальных просмотров
-- =====================================================
CREATE TABLE IF NOT EXISTS wiki_article_views
(
    article_id BIGINT NOT NULL,
    user_id    BIGINT NOT NULL,
    viewed_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_article_views PRIMARY KEY (article_id, user_id),
    CONSTRAINT fk_article_views_article
        FOREIGN KEY (article_id) REFERENCES wiki_articles (id) ON DELETE CASCADE,
    CONSTRAINT fk_article_views_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

-- =====================================================
-- 2. Добавляем новое агрегированное поле views_total
-- =====================================================
ALTER TABLE wiki_articles
    ADD COLUMN IF NOT EXISTS views_total BIGINT NOT NULL DEFAULT 0;

-- =====================================================
-- 3. Добавляем колонку в аудиторную таблицу (Envers)
-- =====================================================
ALTER TABLE wiki_articles_aud
    ADD COLUMN IF NOT EXISTS views_total BIGINT;
-- =====================================================
-- 4. Удаляем старое поле view_count
-- =====================================================
ALTER TABLE wiki_articles
    DROP COLUMN IF EXISTS view_count;

ALTER TABLE wiki_articles_aud
    DROP COLUMN IF EXISTS view_count;
