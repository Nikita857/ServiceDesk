CREATE SEQUENCE IF NOT EXISTS assignments_id_seq START WITH 1 INCREMENT BY 1;

CREATE SEQUENCE IF NOT EXISTS attachments_id_seq START WITH 1 INCREMENT BY 1;

CREATE SEQUENCE IF NOT EXISTS categories_id_seq START WITH 1 INCREMENT BY 1;

CREATE SEQUENCE IF NOT EXISTS direct_messages_id_seq START WITH 1 INCREMENT BY 1;

CREATE SEQUENCE IF NOT EXISTS friendships_id_seq START WITH 1 INCREMENT BY 1;

CREATE SEQUENCE IF NOT EXISTS messages_id_seq START WITH 1 INCREMENT BY 1;

CREATE SEQUENCE IF NOT EXISTS refresh_tokens_id_seq START WITH 1 INCREMENT BY 1;

CREATE SEQUENCE IF NOT EXISTS revinfo_seq START WITH 1 INCREMENT BY 50;

CREATE SEQUENCE IF NOT EXISTS support_lines_id_seq START WITH 1 INCREMENT BY 1;

CREATE SEQUENCE IF NOT EXISTS tickets_id_seq START WITH 1 INCREMENT BY 1;

CREATE SEQUENCE IF NOT EXISTS time_entries_id_seq START WITH 1 INCREMENT BY 1;

CREATE SEQUENCE IF NOT EXISTS users_id_seq START WITH 1 INCREMENT BY 1;

CREATE SEQUENCE IF NOT EXISTS wiki_articles_id_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE assignments
(
    accepted_at     TIMESTAMP WITHOUT TIME ZONE,
    created_at      TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    deleted_at      TIMESTAMP WITHOUT TIME ZONE,
    from_line_id    BIGINT,
    from_user_id    BIGINT,
    id              BIGINT      NOT NULL    DEFAULT nextval('assignments_id_seq'),
    rejected_at     TIMESTAMP WITHOUT TIME ZONE,
    ticket_id       BIGINT      NOT NULL,
    to_line_id      BIGINT,
    to_user_id      BIGINT,
    version         BIGINT,
    status          VARCHAR(20) NOT NULL,
    mode            VARCHAR(30) NOT NULL,
    rejected_reason VARCHAR(500),
    note            VARCHAR(1000),
    CONSTRAINT assignments_pkey PRIMARY KEY (id)
);

CREATE TABLE assignments_aud
(
    rev             INTEGER NOT NULL,
    revtype         SMALLINT,
    accepted_at     TIMESTAMP WITHOUT TIME ZONE,
    created_at      TIMESTAMP WITHOUT TIME ZONE,
    deleted_at      TIMESTAMP WITHOUT TIME ZONE,
    from_line_id    BIGINT,
    id              BIGINT  NOT NULL,
    rejected_at     TIMESTAMP WITHOUT TIME ZONE,
    ticket_id       BIGINT,
    to_line_id      BIGINT,
    status          VARCHAR(20),
    mode            VARCHAR(30),
    rejected_reason VARCHAR(500),
    note            VARCHAR(1000),
    CONSTRAINT assignments_aud_pkey PRIMARY KEY (rev, id)
);

CREATE TABLE attachments
(
    created_at        TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    deleted_at        TIMESTAMP WITHOUT TIME ZONE,
    direct_message_id BIGINT,
    file_size_bytes   BIGINT,
    id                BIGINT        NOT NULL    DEFAULT nextval('attachments_id_seq'),
    message_id        BIGINT,
    ticket_id         BIGINT,
    uploaded_by_id    BIGINT,
    version           BIGINT,
    type              VARCHAR(20)   NOT NULL,
    mime_type         VARCHAR(100),
    url               VARCHAR(2000) NOT NULL,
    filename          VARCHAR(255)  NOT NULL,
    CONSTRAINT attachments_pkey PRIMARY KEY (id)
);

CREATE TABLE categories
(
    display_order   INTEGER,
    user_selectable BOOLEAN      NOT NULL,
    created_at      TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    deleted_at      TIMESTAMP WITHOUT TIME ZONE,
    id              BIGINT       NOT NULL   DEFAULT nextval('categories_id_seq'),
    updated_at      TIMESTAMP WITHOUT TIME ZONE,
    version         BIGINT,
    type            VARCHAR(20)  NOT NULL,
    name            VARCHAR(150) NOT NULL,
    description     VARCHAR(500),
    CONSTRAINT categories_pkey PRIMARY KEY (id)
);

CREATE TABLE categories_aud
(
    display_order   INTEGER,
    rev             INTEGER NOT NULL,
    revtype         SMALLINT,
    user_selectable BOOLEAN,
    created_at      TIMESTAMP WITHOUT TIME ZONE,
    deleted_at      TIMESTAMP WITHOUT TIME ZONE,
    id              BIGINT  NOT NULL,
    updated_at      TIMESTAMP WITHOUT TIME ZONE,
    type            VARCHAR(20),
    name            VARCHAR(150),
    description     VARCHAR(500),
    CONSTRAINT categories_aud_pkey PRIMARY KEY (rev, id)
);

CREATE TABLE direct_messages
(
    created_at   TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    deleted_at   TIMESTAMP WITHOUT TIME ZONE,
    edited_at    TIMESTAMP WITHOUT TIME ZONE,
    id           BIGINT NOT NULL DEFAULT nextval('direct_messages_id_seq'),
    read_at      TIMESTAMP WITHOUT TIME ZONE,
    recipient_id BIGINT NOT NULL,
    sender_id    BIGINT NOT NULL,
    updated_at   TIMESTAMP WITHOUT TIME ZONE,
    version      BIGINT,
    content      TEXT   NOT NULL,
    CONSTRAINT direct_messages_pkey PRIMARY KEY (id)
);

CREATE TABLE friendships
(
    addressee_id BIGINT      NOT NULL,
    id           BIGINT      NOT NULL   DEFAULT nextval('friendships_id_seq'),
    requested_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    requester_id BIGINT      NOT NULL,
    responded_at TIMESTAMP WITHOUT TIME ZONE,
    status       VARCHAR(20) NOT NULL,
    CONSTRAINT friendships_pkey PRIMARY KEY (id)
);

CREATE TABLE friendships_aud
(
    rev          INTEGER NOT NULL,
    revtype      SMALLINT,
    id           BIGINT  NOT NULL,
    requested_at TIMESTAMP WITHOUT TIME ZONE,
    responded_at TIMESTAMP WITHOUT TIME ZONE,
    status       VARCHAR(20),
    CONSTRAINT friendships_aud_pkey PRIMARY KEY (rev, id)
);

CREATE TABLE messages
(
    is_internal           BOOLEAN     NOT NULL,
    created_at            TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    deleted_at            TIMESTAMP WITHOUT TIME ZONE,
    edited_at             TIMESTAMP WITHOUT TIME ZONE,
    id                    BIGINT      NOT NULL  DEFAULT nextval('messages_id_seq'),
    read_by_specialist_at TIMESTAMP WITHOUT TIME ZONE,
    read_by_user_at       TIMESTAMP WITHOUT TIME ZONE,
    sender_id             BIGINT,
    ticket_id             BIGINT      NOT NULL,
    updated_at            TIMESTAMP WITHOUT TIME ZONE,
    version               BIGINT,
    sender_type           VARCHAR(20) NOT NULL,
    content               TEXT        NOT NULL,
    CONSTRAINT messages_pkey PRIMARY KEY (id)
);

CREATE TABLE messages_aud
(
    is_internal           BOOLEAN,
    rev                   INTEGER NOT NULL,
    revtype               SMALLINT,
    created_at            TIMESTAMP WITHOUT TIME ZONE,
    deleted_at            TIMESTAMP WITHOUT TIME ZONE,
    edited_at             TIMESTAMP WITHOUT TIME ZONE,
    id                    BIGINT  NOT NULL,
    read_by_specialist_at TIMESTAMP WITHOUT TIME ZONE,
    read_by_user_at       TIMESTAMP WITHOUT TIME ZONE,
    ticket_id             BIGINT,
    updated_at            TIMESTAMP WITHOUT TIME ZONE,
    sender_type           VARCHAR(20),
    content               TEXT,
    CONSTRAINT messages_aud_pkey PRIMARY KEY (rev, id)
);

CREATE TABLE refresh_tokens
(
    expiry_date TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    id          BIGINT       NOT NULL   DEFAULT nextval('refresh_tokens_id_seq'),
    user_id     BIGINT,
    token       VARCHAR(255) NOT NULL,
    CONSTRAINT refresh_tokens_pkey PRIMARY KEY (id)
);

CREATE TABLE revinfo
(
    rev      INTEGER NOT NULL,
    revtstmp BIGINT,
    CONSTRAINT revinfo_pkey PRIMARY KEY (rev)
);

CREATE TABLE support_line_specialists
(
    line_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    CONSTRAINT support_line_specialists_pkey PRIMARY KEY (line_id, user_id)
);

CREATE TABLE support_lines
(
    display_order       INTEGER,
    last_assigned_index INTEGER,
    sla_minutes         INTEGER,
    created_at          TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    deleted_at          TIMESTAMP WITHOUT TIME ZONE,
    id                  BIGINT       NOT NULL   DEFAULT nextval('support_lines_id_seq'),
    updated_at          TIMESTAMP WITHOUT TIME ZONE,
    version             BIGINT,
    name                VARCHAR(100) NOT NULL,
    description         VARCHAR(500),
    assignment_mode     VARCHAR(255) NOT NULL,
    CONSTRAINT support_lines_pkey PRIMARY KEY (id)
);

CREATE TABLE support_lines_aud
(
    display_order       INTEGER,
    last_assigned_index INTEGER,
    rev                 INTEGER NOT NULL,
    revtype             SMALLINT,
    sla_minutes         INTEGER,
    created_at          TIMESTAMP WITHOUT TIME ZONE,
    deleted_at          TIMESTAMP WITHOUT TIME ZONE,
    id                  BIGINT  NOT NULL,
    updated_at          TIMESTAMP WITHOUT TIME ZONE,
    name                VARCHAR(100),
    description         VARCHAR(500),
    assignment_mode     VARCHAR(255),
    CONSTRAINT support_lines_aud_pkey PRIMARY KEY (rev, id)
);

CREATE TABLE tickets
(
    escalated                    BOOLEAN      NOT NULL,
    rating                       INTEGER,
    assigned_to_id               BIGINT,
    category_support_id          BIGINT,
    category_user_id             BIGINT,
    closed_at                    TIMESTAMP WITHOUT TIME ZONE,
    created_at                   TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    created_by_id                BIGINT       NOT NULL,
    deleted_at                   TIMESTAMP WITHOUT TIME ZONE,
    id                           BIGINT       NOT NULL  DEFAULT nextval('tickets_id_seq'),
    resolved_at                  TIMESTAMP WITHOUT TIME ZONE,
    sla_deadline                 TIMESTAMP WITHOUT TIME ZONE,
    support_line_id              BIGINT,
    telegram_last_bot_message_id BIGINT,
    telegram_message_thread_id   BIGINT,
    time_spent_seconds           BIGINT,
    updated_at                   TIMESTAMP WITHOUT TIME ZONE,
    version                      BIGINT,
    title                        VARCHAR(250) NOT NULL,
    link_1c                      VARCHAR(1000),
    description                  TEXT         NOT NULL,
    feedback                     VARCHAR(255),
    priority                     VARCHAR(255) NOT NULL,
    status                       VARCHAR(255) NOT NULL,
    CONSTRAINT tickets_pkey PRIMARY KEY (id)
);

CREATE TABLE tickets_aud
(
    escalated                    BOOLEAN,
    rating                       INTEGER,
    rev                          INTEGER NOT NULL,
    revtype                      SMALLINT,
    category_support_id          BIGINT,
    category_user_id             BIGINT,
    closed_at                    TIMESTAMP WITHOUT TIME ZONE,
    created_at                   TIMESTAMP WITHOUT TIME ZONE,
    deleted_at                   TIMESTAMP WITHOUT TIME ZONE,
    id                           BIGINT  NOT NULL,
    resolved_at                  TIMESTAMP WITHOUT TIME ZONE,
    sla_deadline                 TIMESTAMP WITHOUT TIME ZONE,
    support_line_id              BIGINT,
    telegram_last_bot_message_id BIGINT,
    telegram_message_thread_id   BIGINT,
    time_spent_seconds           BIGINT,
    updated_at                   TIMESTAMP WITHOUT TIME ZONE,
    title                        VARCHAR(250),
    link_1c                      VARCHAR(1000),
    description                  TEXT,
    feedback                     VARCHAR(255),
    priority                     VARCHAR(255),
    status                       VARCHAR(255),
    CONSTRAINT tickets_aud_pkey PRIMARY KEY (rev, id)
);

CREATE TABLE time_entries
(
    work_date        date,
    created_at       TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    deleted_at       TIMESTAMP WITHOUT TIME ZONE,
    duration_seconds BIGINT NOT NULL,
    entry_date       TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    id               BIGINT NOT NULL    DEFAULT nextval('tickets_id_seq'),
    specialist_id    BIGINT NOT NULL,
    ticket_id        BIGINT NOT NULL,
    updated_at       TIMESTAMP WITHOUT TIME ZONE,
    version          BIGINT,
    activity_type    VARCHAR(30),
    note             VARCHAR(1000),
    CONSTRAINT time_entries_pkey PRIMARY KEY (id)
);

CREATE TABLE time_entries_aud
(
    rev              INTEGER NOT NULL,
    revtype          SMALLINT,
    work_date        date,
    created_at       TIMESTAMP WITHOUT TIME ZONE,
    deleted_at       TIMESTAMP WITHOUT TIME ZONE,
    duration_seconds BIGINT,
    entry_date       TIMESTAMP WITHOUT TIME ZONE,
    id               BIGINT  NOT NULL,
    ticket_id        BIGINT,
    updated_at       TIMESTAMP WITHOUT TIME ZONE,
    activity_type    VARCHAR(30),
    note             VARCHAR(1000),
    CONSTRAINT time_entries_aud_pkey PRIMARY KEY (rev, id)
);

CREATE TABLE user_roles
(
    user_id BIGINT NOT NULL,
    role    VARCHAR(50)
);

CREATE TABLE users
(
    active                    BOOLEAN      NOT NULL,
    specialist                BOOLEAN      NOT NULL,
    created_at                TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    id                        BIGINT       NOT NULL     DEFAULT nextval('users_id_seq'),
    refresh_token_expiry_date TIMESTAMP WITHOUT TIME ZONE,
    telegram_id               BIGINT,
    updated_at                TIMESTAMP WITHOUT TIME ZONE,
    version                   BIGINT,
    username                  VARCHAR(100) NOT NULL,
    fio                       VARCHAR(150),
    email                     VARCHAR(200),
    domain_account            VARCHAR(255),
    password                  VARCHAR(255) NOT NULL,
    CONSTRAINT users_pkey PRIMARY KEY (id)
);

CREATE TABLE wiki_article_tags
(
    article_id BIGINT NOT NULL,
    tag        VARCHAR(255)
);

CREATE TABLE wiki_article_tags_aud
(
    rev        INTEGER      NOT NULL,
    revtype    SMALLINT,
    article_id BIGINT       NOT NULL,
    tag        VARCHAR(255) NOT NULL,
    CONSTRAINT wiki_article_tags_aud_pkey PRIMARY KEY (rev, article_id, tag)
);

CREATE TABLE wiki_articles
(
    category_id   BIGINT,
    created_at    TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    created_by_id BIGINT       NOT NULL,
    deleted_at    TIMESTAMP WITHOUT TIME ZONE,
    id            BIGINT       NOT NULL     DEFAULT nextval('wiki_articles_id_seq'),
    updated_at    TIMESTAMP WITHOUT TIME ZONE,
    updated_by_id BIGINT,
    version       BIGINT,
    view_count    BIGINT       NOT NULL,
    title         VARCHAR(250) NOT NULL,
    slug          VARCHAR(300) NOT NULL,
    excerpt       VARCHAR(500),
    content       TEXT         NOT NULL,
    CONSTRAINT wiki_articles_pkey PRIMARY KEY (id)
);

CREATE TABLE wiki_articles_aud
(
    rev         INTEGER NOT NULL,
    revtype     SMALLINT,
    category_id BIGINT,
    created_at  TIMESTAMP WITHOUT TIME ZONE,
    deleted_at  TIMESTAMP WITHOUT TIME ZONE,
    id          BIGINT  NOT NULL,
    updated_at  TIMESTAMP WITHOUT TIME ZONE,
    view_count  BIGINT,
    title       VARCHAR(250),
    slug        VARCHAR(300),
    excerpt     VARCHAR(500),
    content     TEXT,
    CONSTRAINT wiki_articles_aud_pkey PRIMARY KEY (rev, id)
);

ALTER TABLE categories
    ADD CONSTRAINT categories_name_key UNIQUE (name);

ALTER TABLE friendships
    ADD CONSTRAINT idx_friendship_pair UNIQUE (requester_id, addressee_id);

ALTER TABLE refresh_tokens
    ADD CONSTRAINT refresh_tokens_token_key UNIQUE (token);

ALTER TABLE support_lines
    ADD CONSTRAINT support_lines_name_deleted_at_key UNIQUE (name, deleted_at);

ALTER TABLE support_lines
    ADD CONSTRAINT support_lines_name_key UNIQUE (name);

ALTER TABLE categories
    ADD CONSTRAINT uk_category_name_deleted UNIQUE (name, deleted_at);

ALTER TABLE users
    ADD CONSTRAINT users_email_key UNIQUE (email);

ALTER TABLE users
    ADD CONSTRAINT users_telegram_id_key UNIQUE (telegram_id);

ALTER TABLE users
    ADD CONSTRAINT users_username_key UNIQUE (username);

ALTER TABLE wiki_articles
    ADD CONSTRAINT wiki_articles_slug_key UNIQUE (slug);

ALTER TABLE wiki_articles
    ADD CONSTRAINT wiki_articles_title_key UNIQUE (title);

CREATE INDEX idx_assignment_accepted ON assignments (accepted_at);

CREATE INDEX idx_assignment_active_user ON assignments (to_user_id, accepted_at);

CREATE INDEX idx_assignment_lines ON assignments (from_line_id, to_line_id);

CREATE INDEX idx_assignment_ticket_created ON assignments (ticket_id, created_at);

CREATE INDEX idx_attachment_active ON attachments (ticket_id, deleted_at);

CREATE INDEX idx_attachment_type ON attachments (type);

CREATE INDEX idx_category_deleted ON categories (deleted_at);

CREATE INDEX idx_dm_conversation ON direct_messages (sender_id, recipient_id);

CREATE INDEX idx_dm_recipient_unread ON direct_messages (recipient_id, read_at);

CREATE INDEX idx_dm_sender_recipient ON direct_messages (sender_id, recipient_id, created_at);

CREATE INDEX idx_friendship_status ON friendships (status);

CREATE INDEX idx_message_active ON messages (ticket_id, deleted_at, created_at);

CREATE INDEX idx_message_sender_type ON messages (sender_type);

CREATE INDEX idx_message_ticket_created ON messages (ticket_id, created_at);

CREATE INDEX idx_message_ticket_created_id ON messages (ticket_id, created_at, id);

CREATE INDEX idx_support_line_active ON support_lines (deleted_at);

CREATE INDEX idx_ticket_created_at ON tickets (created_at);

CREATE INDEX idx_ticket_deleted ON tickets (deleted_at);

CREATE INDEX idx_ticket_status ON tickets (status);

CREATE INDEX idx_ticket_telegram_thread ON tickets (telegram_message_thread_id);

CREATE INDEX idx_time_date ON time_entries (entry_date);

CREATE INDEX idx_time_specialist_date ON time_entries (specialist_id, entry_date);

CREATE INDEX idx_time_ticket_date ON time_entries (ticket_id, entry_date);

CREATE INDEX idx_user_domain_account ON users (domain_account);

CREATE INDEX idx_wiki_active ON wiki_articles (deleted_at, id);

CREATE INDEX idx_wiki_content_fts ON wiki_articles (content);

CREATE INDEX idx_wiki_popular ON wiki_articles (view_count);

CREATE INDEX idx_wiki_updated ON wiki_articles (updated_at);

ALTER TABLE refresh_tokens
    ADD CONSTRAINT fk1lih5y2npsf8u5o3vhdb9y0os FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE NO ACTION;

ALTER TABLE tickets_aud
    ADD CONSTRAINT fk1tdd5eyb1d15825plba3jmw8h FOREIGN KEY (rev) REFERENCES revinfo (rev) ON DELETE NO ACTION;

ALTER TABLE assignments
    ADD CONSTRAINT fk2wf5q3bootrcan6mgrfoluneu FOREIGN KEY (to_user_id) REFERENCES users (id) ON DELETE NO ACTION;

CREATE INDEX idx_assignment_pending ON assignments (to_user_id);

ALTER TABLE messages
    ADD CONSTRAINT fk4ui4nnwntodh6wjvck53dbk9m FOREIGN KEY (sender_id) REFERENCES users (id) ON DELETE NO ACTION;

CREATE INDEX idx_message_sender ON messages (sender_id);

ALTER TABLE assignments_aud
    ADD CONSTRAINT fk5joov63k7t6amwmxkpe2qf3b7 FOREIGN KEY (rev) REFERENCES revinfo (rev) ON DELETE NO ACTION;

ALTER TABLE messages
    ADD CONSTRAINT fk6iv985o3ybdk63srj731en4ba FOREIGN KEY (ticket_id) REFERENCES tickets (id) ON DELETE NO ACTION;

ALTER TABLE categories_aud
    ADD CONSTRAINT fk6ti58h8w0q47oacscu06hcite FOREIGN KEY (rev) REFERENCES revinfo (rev) ON DELETE NO ACTION;

ALTER TABLE wiki_articles
    ADD CONSTRAINT fk6usxey8ab1hpmmf9stmb668j3 FOREIGN KEY (category_id) REFERENCES categories (id) ON DELETE NO ACTION;

ALTER TABLE wiki_articles
    ADD CONSTRAINT fk7v7vlorft4xm23d5soibo36bk FOREIGN KEY (updated_by_id) REFERENCES users (id) ON DELETE NO ACTION;

ALTER TABLE assignments
    ADD CONSTRAINT fk7y8lle44cboe931vhxxrmgq2 FOREIGN KEY (to_line_id) REFERENCES support_lines (id) ON DELETE NO ACTION;

ALTER TABLE attachments
    ADD CONSTRAINT fk9imieba3a5c0ea27if65lg68b FOREIGN KEY (direct_message_id) REFERENCES direct_messages (id) ON DELETE NO ACTION;

CREATE INDEX idx_attachment_dm ON attachments (direct_message_id);

ALTER TABLE support_lines_aud
    ADD CONSTRAINT fk9rugabml7wu8q671xq6fjn4g2 FOREIGN KEY (rev) REFERENCES revinfo (rev) ON DELETE NO ACTION;

ALTER TABLE wiki_articles_aud
    ADD CONSTRAINT fkak5fij9xxyf5g1m4kyjeu6bjx FOREIGN KEY (rev) REFERENCES revinfo (rev) ON DELETE NO ACTION;

ALTER TABLE friendships
    ADD CONSTRAINT fkas6bp8so5n3pfcqtfxt72e1ii FOREIGN KEY (requester_id) REFERENCES users (id) ON DELETE NO ACTION;

CREATE INDEX idx_friendship_requester ON friendships (requester_id);

ALTER TABLE assignments
    ADD CONSTRAINT fkaxhh956nao1s3cdtq9x41l9cm FOREIGN KEY (from_user_id) REFERENCES users (id) ON DELETE NO ACTION;

ALTER TABLE attachments
    ADD CONSTRAINT fkay82o4g9v0hkdlh20yllwvutc FOREIGN KEY (ticket_id) REFERENCES tickets (id) ON DELETE NO ACTION;

CREATE INDEX idx_attachment_ticket ON attachments (ticket_id);

ALTER TABLE tickets
    ADD CONSTRAINT fkbqkeahox0j4dil7q0ji74okm3 FOREIGN KEY (category_user_id) REFERENCES categories (id) ON DELETE NO ACTION;

ALTER TABLE attachments
    ADD CONSTRAINT fkcf4ta8qdkixetfy7wnqfv3vkv FOREIGN KEY (message_id) REFERENCES messages (id) ON DELETE NO ACTION;

CREATE INDEX idx_attachment_message ON attachments (message_id);

ALTER TABLE wiki_articles
    ADD CONSTRAINT fkd42mj7vo39p05uxuqdkbh610p FOREIGN KEY (created_by_id) REFERENCES users (id) ON DELETE NO ACTION;

CREATE INDEX idx_wiki_author ON wiki_articles (created_by_id);

ALTER TABLE time_entries_aud
    ADD CONSTRAINT fkefpsnlr5g0jhglyblk22cn3de FOREIGN KEY (rev) REFERENCES revinfo (rev) ON DELETE NO ACTION;

ALTER TABLE friendships
    ADD CONSTRAINT fkeq5r8dvxs43wkt7or9pdno9av FOREIGN KEY (addressee_id) REFERENCES users (id) ON DELETE NO ACTION;

CREATE INDEX idx_friendship_addressee ON friendships (addressee_id);

ALTER TABLE tickets
    ADD CONSTRAINT fkfcerrkbskjjjsypxkctcy8mi6 FOREIGN KEY (support_line_id) REFERENCES support_lines (id) ON DELETE NO ACTION;

CREATE INDEX idx_ticket_support_line ON tickets (support_line_id);

ALTER TABLE user_roles
    ADD CONSTRAINT fkhfh9dx7w3ubf1co1vdev94g3f FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE NO ACTION;

ALTER TABLE wiki_article_tags
    ADD CONSTRAINT fkhow5tirde3fh7ys79rcvpe4yc FOREIGN KEY (article_id) REFERENCES wiki_articles (id) ON DELETE NO ACTION;

ALTER TABLE support_line_specialists
    ADD CONSTRAINT fkiybh5u28atk55kvnrqsft320f FOREIGN KEY (line_id) REFERENCES support_lines (id) ON DELETE NO ACTION;

ALTER TABLE tickets
    ADD CONSTRAINT fkjk9tjr46cg6r175fn1uhje2va FOREIGN KEY (category_support_id) REFERENCES categories (id) ON DELETE NO ACTION;

ALTER TABLE time_entries
    ADD CONSTRAINT fkjqnoxiyk0wkjy0lxylyau2s4y FOREIGN KEY (specialist_id) REFERENCES users (id) ON DELETE NO ACTION;

ALTER TABLE time_entries
    ADD CONSTRAINT fkk7nygqr8cn2qcyeck3ucvwn2n FOREIGN KEY (ticket_id) REFERENCES tickets (id) ON DELETE NO ACTION;

ALTER TABLE direct_messages
    ADD CONSTRAINT fkkyyi2q21gbyogarwvwsr1hmik FOREIGN KEY (recipient_id) REFERENCES users (id) ON DELETE NO ACTION;

ALTER TABLE tickets
    ADD CONSTRAINT fkmyfs6v8v389r3g1rq49cutsda FOREIGN KEY (created_by_id) REFERENCES users (id) ON DELETE NO ACTION;

ALTER TABLE assignments
    ADD CONSTRAINT fko9wcactgruf2l6m8tw49vm7kv FOREIGN KEY (ticket_id) REFERENCES tickets (id) ON DELETE NO ACTION;

ALTER TABLE tickets
    ADD CONSTRAINT fkp0yqkqtkkwu2c92h784tfwf77 FOREIGN KEY (assigned_to_id) REFERENCES users (id) ON DELETE NO ACTION;

CREATE INDEX idx_ticket_assigned_to ON tickets (assigned_to_id);

ALTER TABLE assignments
    ADD CONSTRAINT fkp88hfu9mghobni2psslu890kd FOREIGN KEY (from_line_id) REFERENCES support_lines (id) ON DELETE NO ACTION;

ALTER TABLE support_line_specialists
    ADD CONSTRAINT fkrwhh7u3opbgf1moejdxkcnp57 FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE NO ACTION;

ALTER TABLE friendships_aud
    ADD CONSTRAINT fkrwpuby8q6s3tdx6f77wumidoe FOREIGN KEY (rev) REFERENCES revinfo (rev) ON DELETE NO ACTION;

ALTER TABLE wiki_article_tags_aud
    ADD CONSTRAINT fks31kexveb4wu0rc67389t2of1 FOREIGN KEY (rev) REFERENCES revinfo (rev) ON DELETE NO ACTION;

ALTER TABLE attachments
    ADD CONSTRAINT fktj5qjndi69v9hltsn7q7ddx8g FOREIGN KEY (uploaded_by_id) REFERENCES users (id) ON DELETE NO ACTION;

CREATE INDEX idx_attachment_uploader ON attachments (uploaded_by_id);

ALTER TABLE direct_messages
    ADD CONSTRAINT fktkui7g5fw94e5iwkb85cb0j2o FOREIGN KEY (sender_id) REFERENCES users (id) ON DELETE NO ACTION;

ALTER TABLE messages_aud
    ADD CONSTRAINT fkyo2qpmxpfw9xsr51xq0r5ep7 FOREIGN KEY (rev) REFERENCES revinfo (rev) ON DELETE NO ACTION;

CREATE INDEX idx_category_name ON categories (name);

CREATE INDEX idx_support_line_name ON support_lines (name);

CREATE INDEX idx_user_telegram_id ON users (telegram_id);

CREATE INDEX idx_user_username ON users (username);

CREATE INDEX idx_wiki_title ON wiki_articles (title);