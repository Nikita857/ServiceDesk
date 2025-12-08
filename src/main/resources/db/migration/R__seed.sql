-- This is an idempotent seeder file to populate the database with initial data.
-- Passwords for all users are 'password'

-----------------------------------------------------------
-- USERS (idempotent UPSERT)
-----------------------------------------------------------
INSERT INTO users (id, username, fio, email, password, active, specialist, created_at, updated_at, version)
VALUES
    (1, 'admin', 'Администратор Системы', 'admin@example.com',
     '$2a$10$MatqCqVMwAnZd5jfxxM9mORJMfMOi9X4vYv5CLiWB0VnIV9c3khl2', true, true, NOW(), NOW(), 0),

    (2, 'specialist', 'Специалист Поддержки', 'specialist@example.com',
     '$2a$10$MatqCqVMwAnZd5jfxxM9mORJMfMOi9X4vYv5CLiWB0VnIV9c3khl2', true, true, NOW(), NOW(), 0),

    (3, 'user', 'Обычный Пользователь', 'user@example.com',
     '$2a$10$MatqCqVMwAnZd5jfxxM9mORJMfMOi9X4vYv5CLiWB0VnIV9c3khl2', true, false, NOW(), NOW(), 0)
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
-----------------------------------------------------------
INSERT INTO user_roles (user_id, role) VALUES
                                           (1, 'ADMIN'),
                                           (1, 'SPECIALIST'),
                                           (1, 'USER'),
                                           (2, 'SPECIALIST'),
                                           (2, 'USER'),
                                           (3, 'USER')
    ON CONFLICT (user_id, role) DO NOTHING;

-----------------------------------------------------------
-- CATEGORIES (idempotent)
-----------------------------------------------------------
INSERT INTO categories (id, name, description, type, user_selectable, display_order, created_at, updated_at, version)
VALUES
    (1, 'Техническая поддержка', 'Проблемы с оборудованием и ПО', 'GENERAL', true, 1, NOW(), NOW(), 0),
    (2, 'Вопросы по 1С', 'Консультации и ошибки в 1С', 'GENERAL', true, 2, NOW(), NOW(), 0),
    (3, 'Общие вопросы', 'Вопросы, не вошедшие в другие категории', 'GENERAL', true, 3, NOW(), NOW(), 0),
    (4, 'Внутренняя категория поддержки', 'Используется только специалистами', 'HIDDEN', false, 4, NOW(), NOW(), 0),
    (5, 'База знаний', 'Категория для статей Wiki', 'GENERAL', false, 1, NOW(), NOW(), 0)
    ON CONFLICT (id) DO UPDATE
                            SET name = EXCLUDED.name,
                            description = EXCLUDED.description,
                            type = EXCLUDED.type,
                            user_selectable = EXCLUDED.user_selectable,
                            display_order = EXCLUDED.display_order,
                            updated_at = NOW();

-----------------------------------------------------------
-- SUPPORT LINES (idempotent)
-----------------------------------------------------------
INSERT INTO support_lines (id, name, description, assignment_mode, created_at, updated_at, version)
VALUES
    (1, 'Первая линия поддержки', 'Основная линия для приема заявок', 'FIRST_AVAILABLE', NOW(), NOW(), 0)
    ON CONFLICT (id) DO UPDATE
                            SET name = EXCLUDED.name,
                            description = EXCLUDED.description,
                            assignment_mode = EXCLUDED.assignment_mode,
                            updated_at = NOW();

-----------------------------------------------------------
-- SPECIALISTS IN LINES (idempotent)
-----------------------------------------------------------
INSERT INTO support_line_specialists (line_id, user_id)
VALUES (1, 2)
    ON CONFLICT (line_id, user_id) DO NOTHING;

-----------------------------------------------------------
-- TICKETS (idempotent)
-----------------------------------------------------------
INSERT INTO tickets (
    id, title, description, status, priority,
    created_by_id, created_at, updated_at,
    category_user_id, escalated, version
)
VALUES
    (1, 'Не работает принтер',
     'Принтер не печатает, горит красная лампочка. Модель HP LaserJet 1010.',
     'NEW', 'MEDIUM', 3, NOW(), NOW(), 1, false, 0),

    (2, 'Ошибка в отчете 1С',
     'При формировании отчета "Анализ продаж" за прошлый месяц возникает ошибка "Деление на ноль".',
     'OPEN', 'HIGH', 3, NOW(), NOW(), 2, false, 0),

    (3, 'Как сменить пароль?',
     'Не могу найти в личном кабинете, где можно сменить пароль от входа в систему.',
     'RESOLVED', 'LOW', 3, NOW(), NOW(), 3, false, 0)
    ON CONFLICT (id) DO UPDATE
                            SET title = EXCLUDED.title,
                            description = EXCLUDED.description,
                            status = EXCLUDED.status,
                            priority = EXCLUDED.priority,
                            updated_at = NOW(),
                            category_user_id = EXCLUDED.category_user_id;

-- Assign ticket 2
UPDATE tickets
SET assigned_to_id = 2, support_line_id = 1, status = 'OPEN'
WHERE id = 2;

-- Resolve ticket 3
UPDATE tickets
SET resolved_at = NOW(), closed_at = NOW()
WHERE id = 3;

-----------------------------------------------------------
-- SEQUENCE FIXES
-----------------------------------------------------------
SELECT setval('users_id_seq', (SELECT MAX(id) FROM users));
SELECT setval('categories_id_seq', (SELECT MAX(id) FROM categories));
SELECT setval('support_lines_id_seq', (SELECT MAX(id) FROM support_lines));
SELECT setval('tickets_id_seq', (SELECT MAX(id) FROM tickets));
