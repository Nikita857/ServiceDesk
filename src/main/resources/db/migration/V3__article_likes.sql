-----------------------------------------------------------
-- ARTICLE LIKES
-----------------------------------------------------------

-- SEQUENCE
CREATE SEQUENCE IF NOT EXISTS likes_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

-- TABLE
CREATE TABLE IF NOT EXISTS wiki_article_likes
(
    id          BIGINT NOT NULL DEFAULT nextval('likes_id_seq'),
    article_id BIGINT NOT NULL,
    user_id    BIGINT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,

                             CONSTRAINT pk_article_likes
                             PRIMARY KEY (id),

    CONSTRAINT fk_article_likes_article
    FOREIGN KEY (article_id)
    REFERENCES wiki_articles (id)
                         ON DELETE CASCADE,

    CONSTRAINT fk_article_likes_user
    FOREIGN KEY (user_id)
    REFERENCES users (id)
                         ON DELETE CASCADE,

    CONSTRAINT uk_article_like
    UNIQUE (article_id, user_id)
    );

-----------------------------------------------------------
-- INDEXES
-----------------------------------------------------------

-- For counting likes by article
CREATE INDEX IF NOT EXISTS idx_article_likes_article
    ON wiki_article_likes (article_id);

-- For checking if user liked an article
CREATE INDEX IF NOT EXISTS idx_article_likes_user
    ON wiki_article_likes (user_id);

-- Optional composite index for frequent queries
CREATE INDEX IF NOT EXISTS idx_article_likes_article_user
    ON wiki_article_likes (article_id, user_id);

-----------------------------------------------------------
-- SEQUENCE OWNERSHIP
-----------------------------------------------------------
ALTER SEQUENCE likes_id_seq
    OWNED BY wiki_article_likes.id;
