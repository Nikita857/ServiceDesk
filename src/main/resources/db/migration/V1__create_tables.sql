-- Создание таблиц для системы поддержки пользователей

-- 1. Таблица пользователей
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    fio VARCHAR(150),
    email VARCHAR(200) UNIQUE,
    telegram_id BIGINT UNIQUE,
    domain_account VARCHAR(255),
    specialist BOOLEAN NOT NULL DEFAULT false,
    refresh_token_expiry_date TIMESTAMP WITH TIME ZONE,
    roles TEXT,
    active BOOLEAN NOT NULL DEFAULT true,
    version BIGINT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE
);

-- 2. Таблица ролей пользователей
CREATE TABLE IF NOT EXISTS user_roles (
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role VARCHAR(50) NOT NULL,
    PRIMARY KEY (user_id, role)
);

-- 3. Таблица категорий
CREATE TABLE IF NOT EXISTS categories (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    description VARCHAR(500),
    type VARCHAR(20) NOT NULL DEFAULT 'GENERAL',
    display_order INTEGER DEFAULT 100,
    user_selectable BOOLEAN NOT NULL DEFAULT true,
    deleted_at TIMESTAMP WITH TIME ZONE,
    version BIGINT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE,
    UNIQUE (name, deleted_at)
);

-- 4. Таблица линий поддержки
CREATE TABLE IF NOT EXISTS support_lines (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(500),
    sla_minutes INTEGER DEFAULT 1440,
    assignment_mode VARCHAR(50) NOT NULL DEFAULT 'FIRST_AVAILABLE',
    last_assigned_index INTEGER DEFAULT 0,
    display_order INTEGER DEFAULT 100,
    deleted_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE,
    version BIGINT
);

-- 5. Таблица связи линий поддержки и специалистов
CREATE TABLE IF NOT EXISTS support_line_specialists (
    id BIGSERIAL PRIMARY KEY,
    line_id BIGINT NOT NULL REFERENCES support_lines(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE (line_id, user_id)
);

-- 6. Таблица тикетов
CREATE TABLE IF NOT EXISTS tickets (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(250) NOT NULL,
    description TEXT NOT NULL,
    link_1c VARCHAR(1000),
    created_by_id BIGINT NOT NULL REFERENCES users(id),
    assigned_to_id BIGINT REFERENCES users(id),
    support_line_id BIGINT REFERENCES support_lines(id),
    status VARCHAR(50) NOT NULL DEFAULT 'NEW',
    category_user_id BIGINT REFERENCES categories(id),
    category_support_id BIGINT REFERENCES categories(id),
    time_spent_seconds BIGINT DEFAULT 0,
    priority VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    sla_deadline TIMESTAMP WITH TIME ZONE,
    resolved_at TIMESTAMP WITH TIME ZONE,
    closed_at TIMESTAMP WITH TIME ZONE,
    deleted_at TIMESTAMP WITH TIME ZONE,
    rating INTEGER,
    feedback TEXT,
    telegram_message_thread_id BIGINT,
    telegram_last_bot_message_id BIGINT,
    escalated BOOLEAN DEFAULT false,
    version BIGINT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE
);

-- 7. Таблица сообщений
CREATE TABLE IF NOT EXISTS messages (
    id BIGSERIAL PRIMARY KEY,
    ticket_id BIGINT NOT NULL REFERENCES tickets(id) ON DELETE CASCADE,
    content TEXT NOT NULL,
    sender_id BIGINT REFERENCES users(id),
    sender_type VARCHAR(20) NOT NULL DEFAULT 'USER',
    is_internal BOOLEAN NOT NULL DEFAULT false,
    read_by_user_at TIMESTAMP WITH TIME ZONE,
    read_by_specialist_at TIMESTAMP WITH TIME ZONE,
    deleted_at TIMESTAMP WITH TIME ZONE,
    edited_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE,
    version BIGINT
);

-- 8. Таблица вложений
CREATE TABLE IF NOT EXISTS attachments (
    id BIGSERIAL PRIMARY KEY,
    ticket_id BIGINT REFERENCES tickets(id) ON DELETE CASCADE,
    message_id BIGINT REFERENCES messages(id) ON DELETE CASCADE,
    filename VARCHAR(255) NOT NULL,
    url VARCHAR(2000) NOT NULL,
    file_size_bytes BIGINT,
    mime_type VARCHAR(100),
    type VARCHAR(20) NOT NULL DEFAULT 'SCREENSHOT',
    uploaded_by_id BIGINT REFERENCES users(id),
    deleted_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    version BIGINT,
    CHECK (
        (ticket_id IS NOT NULL AND message_id IS NULL) OR 
        (ticket_id IS NULL AND message_id IS NOT NULL)
    )
);

-- 9. Таблица заданий (назначения)
CREATE TABLE IF NOT EXISTS assignments (
    id BIGSERIAL PRIMARY KEY,
    ticket_id BIGINT NOT NULL REFERENCES tickets(id) ON DELETE CASCADE,
    assigned_by_id BIGINT REFERENCES users(id),
    assigned_to_id BIGINT REFERENCES users(id),
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    assigned_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMP WITH TIME ZONE
);

-- 10. Таблица записей времени
CREATE TABLE IF NOT EXISTS time_entries (
    id BIGSERIAL PRIMARY KEY,
    ticket_id BIGINT NOT NULL REFERENCES tickets(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id),
    duration_seconds BIGINT NOT NULL,
    description TEXT,
    type VARCHAR(50) NOT NULL DEFAULT 'WORK',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    version BIGINT
);

-- 11. Таблица статей вики
CREATE TABLE IF NOT EXISTS wiki_articles (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(250) NOT NULL UNIQUE,
    content TEXT NOT NULL,
    slug VARCHAR(300) NOT NULL UNIQUE,
    excerpt VARCHAR(500),
    tags VARCHAR(1000),
    category_id BIGINT REFERENCES categories(id),
    created_by_id BIGINT NOT NULL REFERENCES users(id),
    updated_by_id BIGINT REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE,
    view_count BIGINT DEFAULT 0,
    like_count BIGINT DEFAULT 0,
    deleted_at TIMESTAMP WITH TIME ZONE,
    version BIGINT
);

-- 12. Таблица тегов статей вики
CREATE TABLE IF NOT EXISTS wiki_article_tags (
    article_id BIGINT NOT NULL REFERENCES wiki_articles(id) ON DELETE CASCADE,
    tag VARCHAR(100) NOT NULL,
    PRIMARY KEY (article_id, tag)
);

-- 13. Таблица токенов обновления
CREATE TABLE IF NOT EXISTS refresh_tokens (
    token VARCHAR(255) PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    expiry_date TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Создание индексов для улучшения производительности
CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);
CREATE INDEX IF NOT EXISTS idx_users_telegram_id ON users(telegram_id);
CREATE INDEX IF NOT EXISTS idx_users_domain_account ON users(domain_account);

CREATE INDEX IF NOT EXISTS idx_support_lines_name ON support_lines(name);
CREATE INDEX IF NOT EXISTS idx_support_line_specialists_line ON support_line_specialists(line_id);
CREATE INDEX IF NOT EXISTS idx_support_line_specialists_user ON support_line_specialists(user_id);

CREATE INDEX IF NOT EXISTS idx_tickets_status ON tickets(status);
CREATE INDEX IF NOT EXISTS idx_tickets_created_at ON tickets(created_at);
CREATE INDEX IF NOT EXISTS idx_tickets_assigned_to ON tickets(assigned_to_id);
CREATE INDEX IF NOT EXISTS idx_tickets_support_line ON tickets(support_line_id);
CREATE INDEX IF NOT EXISTS idx_tickets_deleted ON tickets(deleted_at);
CREATE INDEX IF NOT EXISTS idx_tickets_telegram_thread ON tickets(telegram_message_thread_id);

CREATE INDEX IF NOT EXISTS idx_messages_ticket_created ON messages(ticket_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_messages_sender ON messages(sender_id);
CREATE INDEX IF NOT EXISTS idx_messages_sender_type ON messages(sender_type);

CREATE INDEX IF NOT EXISTS idx_attachments_ticket ON attachments(ticket_id);
CREATE INDEX IF NOT EXISTS idx_attachments_message ON attachments(message_id);
CREATE INDEX IF NOT EXISTS idx_attachments_uploader ON attachments(uploaded_by_id);

CREATE INDEX IF NOT EXISTS idx_categories_name ON categories(name);
CREATE INDEX IF NOT EXISTS idx_categories_deleted ON categories(deleted_at);

CREATE INDEX IF NOT EXISTS idx_wiki_title ON wiki_articles(title);
CREATE INDEX IF NOT EXISTS idx_wiki_content_fts ON wiki_articles USING gin(to_tsvector('russian', content));
CREATE INDEX IF NOT EXISTS idx_wiki_author ON wiki_articles(created_by_id);
CREATE INDEX IF NOT EXISTS idx_wiki_updated ON wiki_articles(updated_at DESC);