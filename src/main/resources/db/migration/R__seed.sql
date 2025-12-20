-- This is an idempotent seeder file to populate the database with initial data.
-- Passwords for all users are 'password'
-----------------------------------------------------------
-- USERS (idempotent UPSERT)
-----------------------------------------------------------
INSERT INTO users (username, fio, email, password, active, specialist, created_at, updated_at, version)
VALUES
-- Developers (DEVELOPER - 3-я линия)
('developer1', 'Бугаков Н.В.', 'dev1@example.com', '$2a$10$MatqCqVMwAnZd5jfxxM9mORJMfMOi9X4vYv5CLiWB0VnIV9c3khl2', true,
 true, NOW(), NOW(), 0),
('developer2', 'Иванов И.И.', 'dev2@example.com', '$2a$10$MatqCqVMwAnZd5jfxxM9mORJMfMOi9X4vYv5CLiWB0VnIV9c3khl2', true,
 true, NOW(), NOW(), 0),
('developer3', 'Петров П.П.', 'dev3@example.com', '$2a$10$MatqCqVMwAnZd5jfxxM9mORJMfMOi9X4vYv5CLiWB0VnIV9c3khl2', true,
 true, NOW(), NOW(), 0),
-- 1C Specialists (DEV1C - 2-я линия)
('specialist1', 'Смиронов А.Е.', 'specialist1@example.com',
 '$2a$10$MatqCqVMwAnZd5jfxxM9mORJMfMOi9X4vYv5CLiWB0VnIV9c3khl2', true, true, NOW(), NOW(), 0),
('specialist2', 'Белозеров А.К.', 'specialist2@example.com',
 '$2a$10$MatqCqVMwAnZd5jfxxM9mORJMfMOi9X4vYv5CLiWB0VnIV9c3khl2', true, true, NOW(), NOW(), 0),
('specialist3', 'Селянин С.А.', 'specialist3@example.com',
 '$2a$10$MatqCqVMwAnZd5jfxxM9mORJMfMOi9X4vYv5CLiWB0VnIV9c3khl2', true, true, NOW(), NOW(), 0),
-- Sysadmins (SYSADMIN - 1-я линия)
('sysadmin1', 'Сивков С.С.', 'sys1@example.com', '$2a$10$MatqCqVMwAnZd5jfxxM9mORJMfMOi9X4vYv5CLiWB0VnIV9c3khl2', true,
 true, NOW(), NOW(), 0),
('sysadmin2', 'Фогель Е.В.', 'sys2@example.com', '$2a$10$MatqCqVMwAnZd5jfxxM9mORJMfMOi9X4vYv5CLiWB0VnIV9c3khl2', true,
 true, NOW(), NOW(), 0),
('sysadmin3', 'Касьянов М.С.', 'sys3@example.com', '$2a$10$MatqCqVMwAnZd5jfxxM9mORJMfMOi9X4vYv5CLiWB0VnIV9c3khl2', true,
 true, NOW(), NOW(), 0),
-- 1C Support (1CSUPPORT - 4-я линия, выбирается пользователем)
('1csupport1', 'Козлов К.К.', '1csup1@example.com', '$2a$10$MatqCqVMwAnZd5jfxxM9mORJMfMOi9X4vYv5CLiWB0VnIV9c3khl2', true,
 true, NOW(), NOW(), 0),
('1csupport2', 'Волков В.В.', '1csup2@example.com', '$2a$10$MatqCqVMwAnZd5jfxxM9mORJMfMOi9X4vYv5CLiWB0VnIV9c3khl2', true,
 true, NOW(), NOW(), 0),
('1csupport3', 'Медведев М.М.', '1csup3@example.com', '$2a$10$MatqCqVMwAnZd5jfxxM9mORJMfMOi9X4vYv5CLiWB0VnIV9c3khl2', true,
 true, NOW(), NOW(), 0),
-- Regular users
('user1', 'Сарипов А.Е.', 'user1@example.com', '$2a$10$MatqCqVMwAnZd5jfxxM9mORJMfMOi9X4vYv5CLiWB0VnIV9c3khl2', true,
 false, NOW(), NOW(), 0),
('user2', 'Канаев В.А.', 'user2@example.com', '$2a$10$MatqCqVMwAnZd5jfxxM9mORJMfMOi9X4vYv5CLiWB0VnIV9c3khl2', true,
 false, NOW(), NOW(), 0),
('user3', 'Петенков С.С.', 'user3@example.com', '$2a$10$MatqCqVMwAnZd5jfxxM9mORJMfMOi9X4vYv5CLiWB0VnIV9c3khl2', true,
 false, NOW(), NOW(), 0),
-- Administrator
('admin', 'Важный Х.У.', 'admin1@example.com', '$2a$10$MatqCqVMwAnZd5jfxxM9mORJMfMOi9X4vYv5CLiWB0VnIV9c3khl2', true,
 true, NOW(), NOW(), 0)
ON CONFLICT (username) DO UPDATE
    SET fio        = EXCLUDED.fio,
        email      = EXCLUDED.email,
        active     = EXCLUDED.active,
        specialist = EXCLUDED.specialist,
        updated_at = NOW();

-----------------------------------------------------------
-- USER ROLES (idempotent) — теперь у каждого ровно одна роль
-----------------------------------------------------------
INSERT INTO user_roles (user_id, role)
SELECT u.id, 'DEVELOPER'
FROM users u
WHERE u.username IN ('developer1', 'developer2', 'developer3')
ON CONFLICT (user_id, role) DO NOTHING;

INSERT INTO user_roles (user_id, role)
SELECT u.id, 'DEV1C'
FROM users u
WHERE u.username IN ('specialist1', 'specialist2', 'specialist3')
ON CONFLICT (user_id, role) DO NOTHING;

INSERT INTO user_roles (user_id, role)
SELECT u.id, 'SYSADMIN'
FROM users u
WHERE u.username IN ('sysadmin1', 'sysadmin2', 'sysadmin3')
ON CONFLICT (user_id, role) DO NOTHING;

-- Новая роль 1CSUPPORT для 4-й линии
INSERT INTO user_roles (user_id, role)
SELECT u.id, '1CSUPPORT'
FROM users u
WHERE u.username IN ('1csupport1', '1csupport2', '1csupport3')
ON CONFLICT (user_id, role) DO NOTHING;

INSERT INTO user_roles (user_id, role)
SELECT u.id, 'USER'
FROM users u
WHERE u.username IN ('user1', 'user2', 'user3')
ON CONFLICT (user_id, role) DO NOTHING;

INSERT INTO user_roles (user_id, role)
SELECT u.id, 'ADMIN'
FROM users u
WHERE u.username = 'admin'
ON CONFLICT (user_id, role) DO NOTHING;

-- Если раньше были лишние роли — их можно удалить (опционально)
DELETE
FROM user_roles ur
    USING users u
WHERE ur.user_id = u.id
  AND u.username IN ('developer1', 'developer2', 'developer3')
  AND ur.role <> 'DEVELOPER';


-----------------------------------------------------------
-- CATEGORIES
-----------------------------------------------------------
INSERT INTO categories (name, description, type, user_selectable, display_order, created_at, updated_at, version)
VALUES ('Техническая поддержка', 'Оборудование, ПО, сеть – первая линия (SYSADMIN)', 'GENERAL', true, 1, NOW(), NOW(),
        0),
       ('Вопросы по 1С', 'Ошибки и консультации по 1С – линия DEV1C', 'GENERAL', true, 2, NOW(), NOW(), 0),
       ('Поддержка 1С', 'Общая поддержка по 1С – линия 1CSUPPORT', 'GENERAL', true, 3, NOW(), NOW(), 0),
       ('Сложные вопросы', 'Эскалация разработчикам – линия DEVELOPER', 'GENERAL', false, 4, NOW(), NOW(), 0),
       ('Внутренняя категория поддержки', 'Используется только специалистами', 'HIDDEN', false, 5, NOW(), NOW(), 0),
       ('База знаний', 'Категория для статей Wiki', 'GENERAL', false, 6, NOW(), NOW(), 0)
ON CONFLICT (name) DO UPDATE
    SET description     = EXCLUDED.description,
        type            = EXCLUDED.type,
        user_selectable = EXCLUDED.user_selectable,
        display_order   = EXCLUDED.display_order,
        updated_at      = NOW();

-----------------------------------------------------------
-- SUPPORT LINES
-- Схема переадресации:
-- USER       → SYSADMIN (1), 1CSUPPORT (4)
-- SYSADMIN   → 1CSUPPORT (4)
-- 1CSUPPORT  → SYSADMIN (1), DEV1C (2)
-- DEV1C      → SYSADMIN (1), 1CSUPPORT (4), DEV1C (2), DEVELOPER (3)
-- DEVELOPER  → все линии
-- ADMIN      → любая линия
-----------------------------------------------------------
INSERT INTO support_lines (display_order, name, description, assignment_mode, created_at, updated_at, version)
VALUES (1, 'Первая линия (SYSADMIN)', 'Техническая поддержка и общие вопросы', 'FIRST_AVAILABLE', NOW(), NOW(), 0),
       (2, 'Линия 1С (DEV1C)', 'Специалисты по 1С - консультации', 'FIRST_AVAILABLE', NOW(), NOW(), 0),
       (3, 'Линия разработчиков (DEVELOPER)', 'Сложные, нестандартные или эскалированные обращения', 'LEAST_LOADED', NOW(), NOW(), 0),
       (4, 'Поддержка 1С (1CSUPPORT)', 'Общая поддержка по 1С - выбирается пользователем', 'FIRST_AVAILABLE', NOW(), NOW(), 0)
ON CONFLICT (name) DO UPDATE
    SET description     = EXCLUDED.description,
        assignment_mode = EXCLUDED.assignment_mode,
        display_order   = EXCLUDED.display_order,
        updated_at      = NOW();

-----------------------------------------------------------
-- SPECIALISTS IN LINES
-----------------------------------------------------------
-- Первая линия: SYSADMIN
INSERT INTO support_line_specialists (line_id, user_id)
SELECT sl.id, u.id
FROM support_lines sl
         JOIN users u ON u.username IN ('sysadmin1', 'sysadmin2', 'sysadmin3')
WHERE sl.name = 'Первая линия (SYSADMIN)'
ON CONFLICT DO NOTHING;

-- Вторая линия: DEV1C
INSERT INTO support_line_specialists (line_id, user_id)
SELECT sl.id, u.id
FROM support_lines sl
         JOIN users u ON u.username IN ('specialist1', 'specialist2', 'specialist3')
WHERE sl.name = 'Линия 1С (DEV1C)'
ON CONFLICT DO NOTHING;

-- Третья линия: DEVELOPER
INSERT INTO support_line_specialists (line_id, user_id)
SELECT sl.id, u.id
FROM support_lines sl
         JOIN users u ON u.username IN ('developer1', 'developer2', 'developer3')
WHERE sl.name = 'Линия разработчиков (DEVELOPER)'
ON CONFLICT DO NOTHING;

-- Четвёртая линия: 1CSUPPORT
INSERT INTO support_line_specialists (line_id, user_id)
SELECT sl.id, u.id
FROM support_lines sl
         JOIN users u ON u.username IN ('1csupport1', '1csupport2', '1csupport3')
WHERE sl.name = 'Поддержка 1С (1CSUPPORT)'
ON CONFLICT DO NOTHING;

-----------------------------------------------------------
-- FRIENDSHIPS — тестовые данные для системы друзей
-----------------------------------------------------------
INSERT INTO friendships (requester_id, addressee_id, status, requested_at, responded_at)
VALUES
-- Дружба между разработчиками (все приняты)
(1, 2, 'ACCEPTED', NOW() - INTERVAL '7 days', NOW() - INTERVAL '6 days'),
(1, 3, 'ACCEPTED', NOW() - INTERVAL '5 days', NOW() - INTERVAL '4 days'),
(2, 3, 'ACCEPTED', NOW() - INTERVAL '3 days', NOW() - INTERVAL '2 days'),

-- Дружба между специалистами 1С
(4, 5, 'ACCEPTED', NOW() - INTERVAL '10 days', NOW() - INTERVAL '9 days'),
(5, 6, 'ACCEPTED', NOW() - INTERVAL '8 days', NOW() - INTERVAL '7 days'),

-- Дружба между сисадминами
(7, 8, 'ACCEPTED', NOW() - INTERVAL '14 days', NOW() - INTERVAL '13 days'),

-- Ожидающие запросы (PENDING)
(13, 1, 'PENDING', NOW() - INTERVAL '1 day', NULL),                        -- user1 → developer1
(14, 4, 'PENDING', NOW() - INTERVAL '2 days', NULL),                       -- user2 → specialist1
(15, 7, 'PENDING', NOW() - INTERVAL '3 days', NULL),                       -- user3 → sysadmin1

-- Отклонённый запрос
(13, 16, 'REJECTED', NOW() - INTERVAL '5 days', NOW() - INTERVAL '4 days') -- user1 → admin (отклонён)
ON CONFLICT (requester_id, addressee_id) DO NOTHING;

-----------------------------------------------------------
-- FIX SEQUENCES
-----------------------------------------------------------
SELECT setval('users_id_seq', (SELECT COALESCE(MAX(id), 1) FROM users));
SELECT setval('categories_id_seq', (SELECT COALESCE(MAX(id), 1) FROM categories));
SELECT setval('support_lines_id_seq', (SELECT COALESCE(MAX(id), 1) FROM support_lines));
SELECT setval('tickets_id_seq', (SELECT COALESCE(MAX(id), 1) FROM tickets));
SELECT setval('friendships_id_seq', (SELECT COALESCE(MAX(id), 1) FROM friendships));