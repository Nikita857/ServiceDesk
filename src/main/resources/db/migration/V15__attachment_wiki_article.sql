-- Добавление связи вложений со статьями Wiki
ALTER TABLE attachments ADD COLUMN wiki_article_id BIGINT;

-- Внешний ключ
ALTER TABLE attachments 
    ADD CONSTRAINT fk_attachment_wiki_article 
    FOREIGN KEY (wiki_article_id) REFERENCES wiki_articles(id);

-- Индекс для быстрого поиска вложений статьи
CREATE INDEX idx_attachment_wiki ON attachments(wiki_article_id);
