-- 1. Сделать user_id NOT NULL
ALTER TABLE refresh_tokens
    ALTER COLUMN user_id SET NOT NULL;

-- 2. Добавить уникальный индекс / ограничение
ALTER TABLE refresh_tokens
    ADD CONSTRAINT refresh_tokens_user_id_key UNIQUE (user_id);

-- 3. Изменить внешний ключ, если нужно
ALTER TABLE refresh_tokens
    DROP CONSTRAINT fk1lih5y2npsf8u5o3vhdb9y0os;

ALTER TABLE refresh_tokens
    ADD CONSTRAINT fk_refresh_tokens_user
        FOREIGN KEY (user_id)
            REFERENCES users(id)
            ON DELETE CASCADE;
