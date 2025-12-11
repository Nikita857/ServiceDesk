-- This is an idempotent seeder file to populate the database with initial data.
-- Passwords for all users are 'password'
-----------------------------------------------------------
-- USERS (idempotent UPSERT)
-----------------------------------------------------------
INSERT INTO users (username, fio, email, password, active, specialist, created_at, updated_at, version) VALUES
-- Developers
('developer1', 'Бугаков Н.В.', 'dev1@example.com', '$2a$10$MatqCqVMwAnZd5jfxxM9mORJMfMOi9X4vYv5CLiWB0VnIV9c3khl2', true, true, NOW(), NOW(), 0),
('developer2', 'Иванов И.И.', 'dev2@example.com', '$2a$10$MatqCqVMwAnZd5jfxxM9mORJMfMOi9X4vYv5CLiWB0VnIV9c3khl2', true, true, NOW(), NOW(), 0),
('developer3', 'Петров П.П.', 'dev3@example.com', '$2a$10$MatqCqVMwAnZd5jfxxM9mORJMfMOi9X4vYv5CLiWB0VnIV9c3khl2', true, true, NOW(), NOW(), 0),
-- 1C Specialists
('specialist1', 'Смиронов А.Е.', 'specialist1@example.com', '$2a$10$MatqCqVMwAnZd5jfxxM9mORJMfMOi9X4vYv5CLiWB0VnIV9c3khl2', true, true, NOW(), NOW(), 0),
('specialist2', 'Белозеров А.К.', 'specialist2@example.com', '$2a$10$MatqCqVMwAnZd5jfxxM9mORJMfMOi9X4vYv5CLiWB0VnIV9c3khl2', true, true, NOW(), NOW(), 0),
('specialist3', 'Селянин С.А.', 'specialist3@example.com', '$2a$10$MatqCqVMwAnZd5jfxxM9mORJMfMOi9X4vYv5CLiWB0VnIV9c3khl2', true, true, NOW(), NOW(), 0),
-- Sysadmins
('sysadmin1', 'Сивков С.С.', 'sys1@example.com', '$2a$10$MatqCqVMwAnZd5jfxxM9mORJMfMOi9X4vYv5CLiWB0VnIV9c3khl2', true, true, NOW(), NOW(), 0),
('sysadmin2', 'Фогель Е.В.', 'sys2@example.com', '$2a$10$MatqCqVMwAnZd5jfxxM9mORJMfMOi9X4vYv5CLiWB0VnIV9c3khl2', true, true, NOW(), NOW(), 0),
('sysadmin3', 'Касьянов М.С.', 'sys3@example.com', '$2a$10$MatqCqVMwAnZd5jfxxM9mORJMfMOi9X4vYv5CLiWB0VnIV9c3khl2', true, true, NOW(), NOW(), 0),
-- Regular users
('user1', 'Сарипов А.Е.', 'user1@example.com', '$2a$10$MatqCqVMwAnZd5jfxxM9mORJMfMOi9X4vYv5CLiWB0VnIV9c3khl2', true, false, NOW(), NOW(), 0),
('user2', 'Канаев В.А.', 'user2@example.com', '$2a$10$MatqCqVMwAnZd5jfxxM9mORJMfMOi9X4vYv5CLiWB0VnIV9c3khl2', true, false, NOW(), NOW(), 0),
('user3', 'Петенков С.С.', 'user3@example.com', '$2a$10$MatqCqVMwAnZd5jfxxM9mORJMfMOi9X4vYv5CLiWB0VnIV9c3khl2', true, false, NOW(), NOW(), 0),
-- Administrator
('admin', 'Важный Х.У.', 'admin1@example.com', '$2a$10$MatqCqVMwAnZd5jfxxM9mORJMfMOi9X4vYv5CLiWB0VnIV9c3khl2', true, true, NOW(), NOW(), 0)
ON CONFLICT (id) DO UPDATE SET
                               username = EXCLUDED.username,
                               fio = EXCLUDED.fio,
                               email = EXCLUDED.email,
                               password = EXCLUDED.password,
                               active = EXCLUDED.active,
                               specialist = EXCLUDED.specialist,
                               updated_at = NOW();

-----------------------------------------------------------
-- USER ROLES (idempotent) — теперь у каждого ровно одна роль
-----------------------------------------------------------
INSERT INTO user_roles (user_id, role) VALUES
-- Developers — только DEVELOPER
(1, 'DEVELOPER'),
(2, 'DEVELOPER'),
(3, 'DEVELOPER'),

-- 1C Specialists — только DEV1C
(4, 'DEV1C'),
(5, 'DEV1C'),
(6, 'DEV1C'),

-- Sysadmins — только SYSADMIN
(7, 'SYSADMIN'),
(8, 'SYSADMIN'),
(9, 'SYSADMIN'),

-- Regular users — только USER
(10, 'USER'),
(11, 'USER'),
(12, 'USER'),

-- Administrator — только ADMIN
(13, 'ADMIN')
ON CONFLICT (user_id, role) DO NOTHING;

-- Если раньше были лишние роли — их можно удалить (опционально)
DELETE FROM user_roles WHERE user_id IN (1,2,3) AND role != 'DEVELOPER';
DELETE FROM user_roles WHERE user_id IN (4,5,6) AND role != 'DEV1C';
DELETE FROM user_roles WHERE user_id IN (7,8,9) AND role != 'SYSADMIN';
DELETE FROM user_roles WHERE user_id IN (10,11,12) AND role != 'USER';
DELETE FROM user_roles WHERE user_id = 13 AND role != 'ADMIN';

-----------------------------------------------------------
-- CATEGORIES
-----------------------------------------------------------
INSERT INTO categories (name, description, type, user_selectable, display_order, created_at, updated_at, version) VALUES
                                                                                                                      ('Техническая поддержка', 'Оборудование, ПО, сеть – первая линия (SYSADMIN)', 'GENERAL', true, 1, NOW(), NOW(), 0),
                                                                                                                      ('Вопросы по 1С', 'Ошибки и консультации по 1С – линия DEV1C', 'GENERAL', true, 2, NOW(), NOW(), 0),
                                                                                                                      ('Сложные вопросы', 'Эскалация разработчикам – линия DEVELOPER', 'GENERAL', false, 3, NOW(), NOW(), 0),
                                                                                                                      ('Внутренняя категория поддержки', 'Используется только специалистами', 'HIDDEN', false, 4, NOW(), NOW(), 0),
                                                                                                                      ('База знаний', 'Категория для статей Wiki', 'GENERAL', false, 5, NOW(), NOW(), 0)
ON CONFLICT (id) DO UPDATE SET
                               name = EXCLUDED.name,
                               description = EXCLUDED.description,
                               type = EXCLUDED.type,
                               user_selectable = EXCLUDED.user_selectable,
                               display_order = EXCLUDED.display_order,
                               updated_at = NOW();

-----------------------------------------------------------
-- SUPPORT LINES
-----------------------------------------------------------
INSERT INTO support_lines (name, description, assignment_mode, created_at, updated_at, version) VALUES
                                                                                                    ('Первая линия (SYSADMIN)', 'Техническая поддержка и общие вопросы', 'FIRST_AVAILABLE', NOW(), NOW(), 0),
                                                                                                    ('Линия 1С (DEV1C)', 'Специалисты по 1С', 'FIRST_AVAILABLE', NOW(), NOW(), 0),
                                                                                                    ('Линия разработчиков (DEVELOPER)', 'Сложные, нестандартные или эскалированные обращения', 'LEAST_LOADED', NOW(), NOW(), 0)
ON CONFLICT (id) DO UPDATE SET
                               name = EXCLUDED.name,
                               description = EXCLUDED.description,
                               assignment_mode = EXCLUDED.assignment_mode,
                               updated_at = NOW();

-----------------------------------------------------------
-- SPECIALISTS IN LINES
-----------------------------------------------------------
INSERT INTO support_line_specialists (line_id, user_id) VALUES
                                                            (1, 7), (1, 8), (1, 9),
                                                            (2, 4), (2, 5), (2, 6),
                                                            (3, 1), (3, 2), (3, 3)
ON CONFLICT (line_id, user_id) DO NOTHING;

-----------------------------------------------------------
-- SAMPLE TICKETS — только от пользователей с ролью USER (по 2 от каждого)
-----------------------------------------------------------
INSERT INTO tickets (
    title, description, status, priority, created_by_id, created_at, updated_at,
    category_user_id, escalated, version
) VALUES
-- user1 (id 10)
('Не могу войти в систему', 'После ввода логина и пароля появляется ошибка "Неверные данные".', 'NEW', 'HIGH', 10, NOW(), NOW(), 1, false, 0),
('Не приходит письмо с подтверждением', 'Зарегистрировался, но письмо с подтверждением не приходит уже 30 минут.', 'NEW', 'MEDIUM', 10, NOW(), NOW(), 1, false, 0),

-- user2 (id 11)
('Проблема с печатью документов', 'При попытке распечатать документ из личного кабинета выходит пустой лист.', 'NEW', 'MEDIUM', 11, NOW(), NOW(), 1, false, 0),
('Не отображается история платежей', 'В разделе "Платежи" ничего не видно, хотя платежи были.', 'NEW', 'HIGH', 11, NOW(), NOW(), 1, false, 0),

-- user3 (id 12)
('Ошибка при загрузке файла', 'При попытке прикрепить файл к заявке выдаёт "Недопустимый формат".', 'NEW', 'MEDIUM', 12, NOW(), NOW(), 1, false, 0),
('Не могу сбросить пароль', 'Нажала "Забыли пароль?", но письмо с ссылкой не приходит.', 'NEW', 'HIGH', 12, NOW(), NOW(), 1, false, 0)
ON CONFLICT (id) DO UPDATE SET
                               title = EXCLUDED.title,
                               description = EXCLUDED.description,
                               status = EXCLUDED.status,
                               priority = EXCLUDED.priority,
                               updated_at = NOW(),
                               category_user_id = EXCLUDED.category_user_id;

-----------------------------------------------------------
-- FIX SEQUENCES
-----------------------------------------------------------
SELECT setval('users_id_seq', (SELECT MAX(id) FROM users));
SELECT setval('categories_id_seq', (SELECT MAX(id) FROM categories));
SELECT setval('support_lines_id_seq', (SELECT MAX(id) FROM support_lines));
SELECT setval('tickets_id_seq', (SELECT MAX(id) FROM tickets));