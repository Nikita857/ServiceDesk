-- This is an idempotent seeder file to populate the database with initial data.
-- Passwords for all users are 'password'

-----------------------------------------------------------
-- USERS (idempotent UPSERT)
-----------------------------------------------------------
INSERT INTO users (username, fio, email, password, active, specialist, created_at, updated_at, version)
VALUES
    -- Developers (самые сложные вопросы)
    ('developer1', 'Бугаков Н.В.', 'dev1@example.com',
     '$2a$10$MatqCqVMwAnZd5jfxxM9mORJMfMOi9X4vYv5CLiWB0VnIV9c3khl2', true, true, NOW(), NOW(), 0),
    ('developer2', 'Иванов И.И.', 'dev2@example.com',
     '$2a$10$MatqCqVMwAnZd5jfxxM9mORJMfMOi9X4vYv5CLiWB0VnIV9c3khl2', true, true, NOW(), NOW(), 0),
    ('developer3', 'Петров П.П.', 'dev3@example.com',
     '$2a$10$MatqCqVMwAnZd5jfxxM9mORJMfMOi9X4vYv5CLiWB0VnIV9c3khl2', true, true, NOW(), NOW(), 0),

    -- 1C Specialists
    ('specialist1', 'Смиронов А.Е.', 'specialist1@example.com',
     '$2a$10$MatqCqVMwAnZd5jfxxM9mORJMfMOi9X4vYv5CLiWB0VnIV9c3khl2', true, true, NOW(), NOW(), 0),
    ('specialist2', 'Белозеров А.К.', 'specialist2@example.com',
     '$2a$10$MatqCqVMwAnZd5jfxxM9mORJMfMOi9X4vYv5CLiWB0VnIV9c3khl2', true, true, NOW(), NOW(), 0),
    ('specialist3', 'Селянин С.А.', 'specialist3@example.com',
     '$2a$10$MatqCqVMwAnZd5jfxxM9mORJMfMOi9X4vYv5CLiWB0VnIV9c3khl2', true, true, NOW(), NOW(), 0),

    -- Sysadmins — первая линия
    ('sysadmin1', 'Сивков С.С.', 'sys1@example.com',
     '$2a$10$MatqCqVMwAnZd5jfxxM9mORJMfMOi9X4vYv5CLiWB0VnIV9c3khl2', true, true, NOW(), NOW(), 0),
    ('sysadmin2', 'Фогель Е.В.', 'sys2@example.com',
     '$2a$10$MatqCqVMwAnZd5jfxxM9mORJMfMOi9X4vYv5CLiWB0VnIV9c3khl2', true, true, NOW(), NOW(), 0),
    ('sysadmin3', 'Касьянов М.С.', 'sys3@example.com',
     '$2a$10$MatqCqVMwAnZd5jfxxM9mORJMfMOi9X4vYv5CLiWB0VnIV9c3khl2', true, true, NOW(), NOW(), 0),

    -- Users
    ('user1', 'Сарипов А.Е.', 'user1@example.com',
     '$2a$10$MatqCqVMwAnZd5jfxxM9mORJMfMOi9X4vYv5CLiWB0VnIV9c3khl2', true, false, NOW(), NOW(), 0),
    ('user2', 'Канаев В.А.', 'user2@example.com',
     '$2a$10$MatqCqVMwAnZd5jfxxM9mORJMfMOi9X4vYv5CLiWB0VnIV9c3khl2', true, false, NOW(), NOW(), 0),
    ('user3', 'Петенков С.С.', 'user3@example.com',
     '$2a$10$MatqCqVMwAnZd5jfxxM9mORJMfMOi9X4vYv5CLiWB0VnIV9c3khl2', true, false, NOW(), NOW(), 0),

    -- Administrator
    ('admin', 'Важный Х.У.', 'admin1@example.com',
    '$2a$10$MatqCqVMwAnZd5jfxxM9mORJMfMOi9X4vYv5CLiWB0VnIV9c3khl2', true, true, NOW(), NOW(), 0)

ON CONFLICT (id) DO UPDATE
    SET username = EXCLUDED.username,
        fio = EXCLUDED.fio,
        email = EXCLUDED.email,
        password = EXCLUDED.password,
        active = EXCLUDED.active,
        specialist = EXCLUDED.specialist,
        updated_at = NOW();

-----------------------------------------------------------
-- USER ROLES (idempotent)
-- Новая ролевая модель
-----------------------------------------------------------

INSERT INTO user_roles (user_id, role) VALUES

                                           -- Developers: сложные вопросы
                                           (1, 'DEVELOPER'), (1, 'USER'),
                                           (2, 'DEVELOPER'), (2, 'USER'),
                                           (3, 'DEVELOPER'), (3, 'USER'),

                                           -- 1C specialists: линия вопросов по 1С
                                           (4, 'DEV1C'), (4, 'USER'),
                                           (5, 'DEV1C'), (5, 'USER'),
                                           (6, 'DEV1C'), (6, 'USER'),

                                           -- Sysadmins: основная линия поддержки
                                           (7, 'SYSADMIN'), (7, 'USER'),
                                           (8, 'SYSADMIN'), (8, 'USER'),
                                           (9, 'SYSADMIN'), (9, 'USER'),

                                           -- Regular users
                                           (10, 'USER'),
                                           (11, 'USER'),
                                           (12, 'USER'),

                                            --Administrator
                                           (13, 'ADMIN'),
                                           (13, 'USER')

ON CONFLICT (user_id, role) DO NOTHING;

-----------------------------------------------------------
-- CATEGORIES (направления заявок)
-----------------------------------------------------------

INSERT INTO categories (name, description, type, user_selectable, display_order, created_at, updated_at, version)
VALUES
    ('Техническая поддержка', 'Оборудование, ПО, сеть – первая линия (SYSADMIN)', 'GENERAL', true, 1, NOW(), NOW(), 0),
    ('Вопросы по 1С', 'Ошибки и консультации по 1С – линия DEV1C', 'GENERAL', true, 2, NOW(), NOW(), 0),
    ('Сложные вопросы', 'Эскалация разработчикам – линия DEVELOPER', 'GENERAL', false, 3, NOW(), NOW(), 0),
    ('Внутренняя категория поддержки', 'Используется только специалистами', 'HIDDEN', false, 4, NOW(), NOW(), 0),
    ('База знаний', 'Категория для статей Wiki', 'GENERAL', false, 5, NOW(), NOW(), 0)

ON CONFLICT (id) DO UPDATE
    SET name = EXCLUDED.name,
        description = EXCLUDED.description,
        type = EXCLUDED.type,
        user_selectable = EXCLUDED.user_selectable,
        display_order = EXCLUDED.display_order,
        updated_at = NOW();

-----------------------------------------------------------
-- SUPPORT LINES (3 линии поддержки)
-----------------------------------------------------------

INSERT INTO support_lines (name, description, assignment_mode, created_at, updated_at, version)
VALUES
    ( 'Первая линия (SYSADMIN)', 'Техническая поддержка и общие вопросы', 'FIRST_AVAILABLE', NOW(), NOW(), 0),
    ( 'Линия 1С (DEV1C)', 'Специалисты по 1С', 'FIRST_AVAILABLE', NOW(), NOW(), 0),
    ( 'Линия разработчиков (DEVELOPER)', 'Сложные, нестандартные или эскалированные обращения', 'LEAST_LOADED', NOW(), NOW(), 0)

ON CONFLICT (id) DO UPDATE
    SET name = EXCLUDED.name,
        description = EXCLUDED.description,
        assignment_mode = EXCLUDED.assignment_mode,
        updated_at = NOW();

-----------------------------------------------------------
-- SPECIALISTS IN LINES (кто работает на какой линии)
-----------------------------------------------------------

INSERT INTO support_line_specialists (line_id, user_id) VALUES
                                                            -- Первая линия (SYSADMIN)
                                                            (1, 7), (1, 8), (1, 9),

                                                            -- Линия 1С (DEV1C)
                                                            (2, 4), (2, 5), (2, 6),

                                                            -- Линия разработчиков
                                                            (3, 1), (3, 2), (3, 3)

ON CONFLICT (line_id, user_id) DO NOTHING;

-----------------------------------------------------------
-- SAMPLE TICKETS
-----------------------------------------------------------

INSERT INTO tickets (
    title, description, status, priority,
    created_by_id, created_at, updated_at,
    category_user_id, escalated, version
)
VALUES
    -- Техническая проблема (1 линия)
    ('Не работает принтер',
     'Принтер не печатает, горит красная лампочка.',
     'NEW', 'MEDIUM', 3, NOW(), NOW(), 1, false, 0),

    -- Вопрос 1С (2 линия)
    ('Ошибка в отчете 1С',
     'Ошибка "Деление на ноль" в отчёте "Анализ продаж".',
     'OPEN', 'HIGH', 3, NOW(), NOW(), 2, false, 0),

    -- Простой пользовательский вопрос
    ('Как сменить пароль?',
     'Где в личном кабинете сменить пароль?',
     'RESOLVED', 'LOW', 3, NOW(), NOW(), 3, false, 0)

ON CONFLICT (id) DO UPDATE
    SET title = EXCLUDED.title,
        description = EXCLUDED.description,
        status = EXCLUDED.status,
        priority = EXCLUDED.priority,
        updated_at = NOW(),
        category_user_id = EXCLUDED.category_user_id;

-- Назначение тикета #2 на линию 1С
UPDATE tickets
SET assigned_to_id = 4, support_line_id = 2, status = 'OPEN'
WHERE id = 2;

-- Тикет #3 закрыт
UPDATE tickets
SET resolved_at = NOW(), closed_at = NOW()
WHERE id = 3;

-----------------------------------------------------------
-- FIX SEQUENCES
-----------------------------------------------------------
SELECT setval('users_id_seq', (SELECT MAX(id) FROM users));
SELECT setval('categories_id_seq', (SELECT MAX(id) FROM categories));
SELECT setval('support_lines_id_seq', (SELECT MAX(id) FROM support_lines));
SELECT setval('tickets_id_seq', (SELECT MAX(id) FROM tickets));
