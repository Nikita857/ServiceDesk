-- V3: Добавление системы друзей и вложений к личным сообщениям

-- =====================================================
-- 1. Последовательность для таблицы friendships
-- =====================================================
CREATE SEQUENCE IF NOT EXISTS friendships_id_seq START WITH 1 INCREMENT BY 1;

-- =====================================================
-- 2. Таблица friendships (система друзей)
-- =====================================================
CREATE TABLE friendships
(
    id           BIGINT NOT NULL DEFAULT nextval('friendships_id_seq') PRIMARY KEY,
    requester_id BIGINT NOT NULL,
    addressee_id BIGINT NOT NULL,
    status       VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    requested_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    responded_at TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT fk_friendship_requester FOREIGN KEY (requester_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_friendship_addressee FOREIGN KEY (addressee_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT uk_friendship_pair UNIQUE (requester_id, addressee_id)
);

-- Индексы для friendships
CREATE INDEX idx_friendship_requester ON friendships (requester_id);
CREATE INDEX idx_friendship_addressee ON friendships (addressee_id);
CREATE INDEX idx_friendship_status ON friendships (status);

-- Привязка sequence к таблице
ALTER SEQUENCE friendships_id_seq OWNED BY friendships.id;

-- =====================================================
-- 3. Добавление колонки direct_message_id в attachments
-- =====================================================
ALTER TABLE attachments
    ADD COLUMN IF NOT EXISTS direct_message_id BIGINT;

-- Foreign key для связи с direct_messages
ALTER TABLE attachments
    ADD CONSTRAINT fk_attachment_direct_message
    FOREIGN KEY (direct_message_id) REFERENCES direct_messages (id) ON DELETE SET NULL;

-- Индекс для поиска вложений по личному сообщению
CREATE INDEX idx_attachment_dm ON attachments (direct_message_id);
