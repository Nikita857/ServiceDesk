-- V2__test_data.sql
-- Миграция для добавления тестовых данных (PostgreSQL, Flyway-ready)

SET search_path TO public;

---------------------------------------------------------------------
-- 1. Безопасное удаление существующих данных
---------------------------------------------------------------------
DO
$$
    BEGIN
        DELETE FROM wiki_article_tags;
        DELETE FROM attachments;
        DELETE FROM messages;
        DELETE FROM time_entries;
        DELETE FROM assignments;
        DELETE FROM tickets;
        DELETE FROM support_line_specialists;
        DELETE FROM user_roles;
        DELETE FROM wiki_articles;
        DELETE FROM support_lines;
        DELETE FROM categories;
        DELETE FROM users;
        DELETE FROM refresh_tokens;
    END
$$ LANGUAGE plpgsql;

---------------------------------------------------------------------
-- 2. Сброс последовательностей (если существуют)
---------------------------------------------------------------------
DO
$$
    BEGIN
        ALTER SEQUENCE IF EXISTS users_id_seq RESTART WITH 1;
        ALTER SEQUENCE IF EXISTS support_lines_id_seq RESTART WITH 1;
        ALTER SEQUENCE IF EXISTS categories_id_seq RESTART WITH 1;
        ALTER SEQUENCE IF EXISTS tickets_id_seq RESTART WITH 1;
        ALTER SEQUENCE IF EXISTS assignments_id_seq RESTART WITH 1;
        ALTER SEQUENCE IF EXISTS messages_id_seq RESTART WITH 1;
        ALTER SEQUENCE IF EXISTS attachments_id_seq RESTART WITH 1;
        ALTER SEQUENCE IF EXISTS time_entries_id_seq RESTART WITH 1;
        ALTER SEQUENCE IF EXISTS wiki_articles_id_seq RESTART WITH 1;
    END
$$ LANGUAGE plpgsql;

---------------------------------------------------------------------
-- 3. Пользователи
---------------------------------------------------------------------
INSERT INTO users (username, password, fio, email, domain_account, active, specialist, telegram_id, created_at,
                   updated_at, version)
VALUES ('admin', '$2a$10$kMKZA/1Uj3rU6ML3e8hzUOsGDvTpy5WUha2wwqzc7tAbetLRNZgAe', 'Администратор Системы',
        'admin@company.com', 'COMPANY\\admin', true, true, 999999999, NOW(), NOW(), 1)
ON CONFLICT (username) DO NOTHING;

INSERT INTO users (username, password, fio, email, domain_account, active, specialist, telegram_id, created_at,
                   updated_at, version)
VALUES ('ivanovii', '$2a$10$kMKZA/1Uj3rU6ML3e8hzUOsGDvTpy5WUha2wwqzc7tAbetLRNZgAe', 'Иванов Иван Иванович',
        'ivanov@company.com', 'COMPANY\\ivanovii', true, true, 123456789, NOW(), NOW(), 1)
ON CONFLICT (username) DO NOTHING;

INSERT INTO users (username, password, fio, email, domain_account, active, specialist, telegram_id, created_at,
                   updated_at, version)
VALUES ('petrovap', '$2a$10$kMKZA/1Uj3rU6ML3e8hzUOsGDvTpy5WUha2wwqzc7tAbetLRNZgAe', 'Петрова Анна Сергеевна',
        'petrova@company.com', 'COMPANY\\petrovap', true, true, 987654321, NOW(), NOW(), 1)
ON CONFLICT (username) DO NOTHING;

INSERT INTO users (username, password, fio, email, domain_account, active, specialist, telegram_id, created_at,
                   updated_at, version)
VALUES ('sidorovms', '$2a$10$kMKZA/1Uj3rU6ML3e8hzUOsGDvTpy5WUha2wwqzc7tAbetLRNZgAe', 'Сидоров Михаил Владимирович',
        'sidorov@company.com', 'COMPANY\\sidorovms', true, true, 555555555, NOW(), NOW(), 1)
ON CONFLICT (username) DO NOTHING;

INSERT INTO users (username, password, fio, email, domain_account, active, specialist, telegram_id, created_at,
                   updated_at, version)
VALUES ('smirnovaek', '$2a$10$kMKZA/1Uj3rU6ML3e8hzUOsGDvTpy5WUha2wwqzc7tAbetLRNZgAe', 'Смирнова Екатерина Андреевна',
        'smirnova@company.com', 'COMPANY\\smirnovaek', true, true, 111222333, NOW(), NOW(), 1)
ON CONFLICT (username) DO NOTHING;

INSERT INTO users (username, password, fio, email, domain_account, active, specialist, telegram_id, created_at,
                   updated_at, version)
VALUES ('kuznetsovda', '$2a$10$kMKZA/1Uj3rU6ML3e8hzUOsGDvTpy5WUha2wwqzc7tAbetLRNZgAe', 'Кузнецов Дмитрий Александрович',
        'kuznetsov@company.com', 'COMPANY\\kuznetsovda', true, false, NULL, NOW(), NOW(), 1)
ON CONFLICT (username) DO NOTHING;

INSERT INTO users (username, password, fio, email, domain_account, active, specialist, telegram_id, created_at,
                   updated_at, version)
VALUES ('nikolaevai', '$2a$10$kMKZA/1Uj3rU6ML3e8hzUOsGDvTpy5WUha2wwqzc7tAbetLRNZgAe', 'Николаева Инна Петровна',
        'nikolaeva@company.com', 'COMPANY\\nikolaevai', true, false, NULL, NOW(), NOW(), 1)
ON CONFLICT (username) DO NOTHING;

INSERT INTO users (username, password, fio, email, domain_account, active, specialist, telegram_id, created_at,
                   updated_at, version)
VALUES ('support1', '$2a$10$kMKZA/1Uj3rU6ML3e8hzUOsGDvTpy5WUha2wwqzc7tAbetLRNZgAe', 'Специалист поддержки 1',
        'support1@company.com', 'COMPANY\\support1', true, true, 888888888, NOW(), NOW(), 1)
ON CONFLICT (username) DO NOTHING;

---------------------------------------------------------------------
-- 4. Роли пользователей (идемпотентно)
---------------------------------------------------------------------
WITH new_roles (user_id, role) AS (VALUES (1, 'ROLE_ADMIN'),
                                          (1, 'ROLE_SPECIALIST'),
                                          (1, 'ROLE_DEVELOPER'),
                                          (2, 'ROLE_SPECIALIST'),
                                          (2, 'ROLE_TEAM_LEAD'),
                                          (3, 'ROLE_SPECIALIST'),
                                          (4, 'ROLE_SPECIALIST'),
                                          (5, 'ROLE_USER'),
                                          (6, 'ROLE_USER'),
                                          (8, 'ROLE_SPECIALIST'))
INSERT
INTO user_roles (user_id, role)
SELECT nr.user_id, nr.role
FROM new_roles nr
WHERE NOT EXISTS (SELECT 1 FROM user_roles ur WHERE ur.user_id = nr.user_id AND ur.role = nr.role);

---------------------------------------------------------------------
-- 5. Линии поддержки
---------------------------------------------------------------------
INSERT INTO support_lines (name, description, assignment_mode, sla_minutes, display_order, last_assigned_index,
                           created_at, updated_at, version)
VALUES ('Техническая поддержка', 'Решение технических вопросов по ПО и оборудованию', 'ROUND_ROBIN', 240, 1, 0, NOW(),
        NOW(), 1)
ON CONFLICT (name) DO NOTHING;

INSERT INTO support_lines (name, description, assignment_mode, sla_minutes, display_order, last_assigned_index,
                           created_at, updated_at, version)
VALUES ('Бухгалтерия и финансы', 'Вопросы по зарплате, отчетности и финансам', 'FIRST_AVAILABLE', 480, 2, 0, NOW(),
        NOW(), 1)
ON CONFLICT (name) DO NOTHING;

INSERT INTO support_lines (name, description, assignment_mode, sla_minutes, display_order, last_assigned_index,
                           created_at, updated_at, version)
VALUES ('Кадровая служба', 'Вопросы по кадрам, отпускам, больничным', 'LEAST_LOADED', 360, 3, 0, NOW(), NOW(), 1)
ON CONFLICT (name) DO NOTHING;

INSERT INTO support_lines (name, description, assignment_mode, sla_minutes, display_order, last_assigned_index,
                           created_at, updated_at, version)
VALUES ('ИТ-инфраструктура', 'Сервера, сети, телефония', 'DIRECT', 120, 4, 0, NOW(), NOW(), 1)
ON CONFLICT (name) DO NOTHING;

INSERT INTO support_lines (name, description, assignment_mode, sla_minutes, display_order, last_assigned_index,
                           created_at, updated_at, version)
VALUES ('Общие вопросы', 'Общие организационные вопросы', 'ROUND_ROBIN', 1440, 5, 0, NOW(), NOW(), 1)
ON CONFLICT (name) DO NOTHING;

---------------------------------------------------------------------
-- 6. Специалисты в линиях (идемпотентно)
---------------------------------------------------------------------
WITH new_sls (line_id, user_id) AS (VALUES (1, 2),
                                           (1, 3),
                                           (1, 4),
                                           (2, 2),
                                           (2, 4),
                                           (3, 2),
                                           (3, 4),
                                           (4, 3),
                                           (4, 1),
                                           (5, 2),
                                           (5, 3),
                                           (5, 4),
                                           (5, 8))
INSERT
INTO support_line_specialists (line_id, user_id)
SELECT ns.line_id, ns.user_id
FROM new_sls ns
WHERE NOT EXISTS (SELECT 1 FROM support_line_specialists s WHERE s.line_id = ns.line_id AND s.user_id = ns.user_id);

---------------------------------------------------------------------
-- 7. Категории
---------------------------------------------------------------------
INSERT INTO categories (name, description, type, display_order, user_selectable, created_at, updated_at, version)
VALUES ('Проблемы с ПО', 'Проблемы с установкой, запуском или работой программного обеспечения', 'GENERAL', 1, true,
        NOW(), NOW(), 1)
ON CONFLICT (name) DO NOTHING;

INSERT INTO categories (name, description, type, display_order, user_selectable, created_at, updated_at, version)
VALUES ('Оборудование', 'Неисправности компьютеров, принтеров, сканеров и другого оборудования', 'GENERAL', 2, true,
        NOW(), NOW(), 1)
ON CONFLICT (name) DO NOTHING;

INSERT INTO categories (name, description, type, display_order, user_selectable, created_at, updated_at, version)
VALUES ('Сеть и интернет', 'Проблемы с подключением к сети, интернету, Wi-Fi', 'GENERAL', 3, true, NOW(), NOW(), 1)
ON CONFLICT (name) DO NOTHING;

INSERT INTO categories (name, description, type, display_order, user_selectable, created_at, updated_at, version)
VALUES ('Учетная запись', 'Проблемы с входом в систему, сброс пароля', 'GENERAL', 4, true, NOW(), NOW(), 1)
ON CONFLICT (name) DO NOTHING;

INSERT INTO categories (name, description, type, display_order, user_selectable, created_at, updated_at, version)
VALUES ('Зарплата', 'Вопросы по начислению зарплаты, премиям', 'GENERAL', 5, true, NOW(), NOW(), 1)
ON CONFLICT (name) DO NOTHING;

INSERT INTO categories (name, description, type, display_order, user_selectable, created_at, updated_at, version)
VALUES ('Отпуск', 'Оформление отпуска, перенос отпуска', 'GENERAL', 6, true, NOW(), NOW(), 1)
ON CONFLICT (name) DO NOTHING;

INSERT INTO categories (name, description, type, display_order, user_selectable, created_at, updated_at, version)
VALUES ('Срочные проблемы', 'Срочные проблемы, требующие немедленного решения', 'ESCALATION', 7, false, NOW(), NOW(), 1)
ON CONFLICT (name) DO NOTHING;

INSERT INTO categories (name, description, type, display_order, user_selectable, created_at, updated_at, version)
VALUES ('Системные уведомления', 'Системные сообщения и уведомления', 'SYSTEM', 8, false, NOW(), NOW(), 1)
ON CONFLICT (name) DO NOTHING;

INSERT INTO categories (name, description, type, display_order, user_selectable, created_at, updated_at, version)
VALUES ('Обучение', 'Запросы на обучение, инструктаж', 'GENERAL', 9, true, NOW(), NOW(), 1)
ON CONFLICT (name) DO NOTHING;

INSERT INTO categories (name, description, type, display_order, user_selectable, created_at, updated_at, version)
VALUES ('Запрос техники', 'Заказ нового оборудования или ПО', 'GENERAL', 10, true, NOW(), NOW(), 1)
ON CONFLICT (name) DO NOTHING;

---------------------------------------------------------------------
-- 8. Тикеты (10 тестовых кейсов)
---------------------------------------------------------------------
INSERT INTO tickets (title, description, link_1c, created_by_id, assigned_to_id, support_line_id, status,
                     category_user_id, category_support_id, time_spent_seconds, priority, escalated,
                     created_at, updated_at, version)
VALUES ('Ошибка при закрытии месяца в 1С',
        'При проведении регламентной операции ''Закрытие месяца'' вылетает ошибка 100500',
        '1c:base/processing/CloseMonth', 6, 2, 2, 'OPEN',
        1, 1, 5400, 'URGENT', true,
        NOW() - INTERVAL '1 day', NOW() - INTERVAL '30 minutes', 1),

       ('Не приходит почта на новый ящик',
        'Сотрудник Иванов И.И. не получает письма на новый почтовый ящик ivanov@company.ru',
        NULL, 5, 3, 1, 'PENDING',
        5, 5, 1800, 'MEDIUM', false,
        NOW() - INTERVAL '12 hours', NOW() - INTERVAL '10 hours', 1),

       ('Замена картриджа в принтере бухгалтерии',
        'Закончился тонер в принтере HP в бухгалтерии',
        NULL, 6, 4, 1, 'CLOSED',
        2, 2, 3600, 'LOW', false,
        NOW() - INTERVAL '5 days', NOW() - INTERVAL '3 days', 1),

       ('Ошибка в отчёте ''Зарплата по сотрудникам''',
        'В отчёте по зарплате не учитываются премии за декабрь',
        '1c:base/report/SalaryByEmployee', 5, 1, 3, 'ESCALATED',
        1, 1, 7200, 'HIGH', true,
        NOW() - INTERVAL '18 hours', NOW() - INTERVAL '2 hours', 1),

       ('Оформление отпуска с 20 декабря',
        'Прошу оформить ежегодный оплачиваемый отпуск с 20 по 31 декабря',
        NULL, 6, 8, 3, 'OPEN',
        6, 6, 900, 'LOW', false,
        NOW() - INTERVAL '3 days', NOW() - INTERVAL '1 day', 1),

       ('Не загружается сайт компании',
        'Сайт company.ru не открывается уже 3 часа',
        NULL, 5, 3, 4, 'RESOLVED',
        3, 3, 10800, 'URGENT', true,
        NOW() - INTERVAL '4 hours', NOW() - INTERVAL '30 minutes', 1),

       ('Запрос на повышение зарплаты',
        'Хочу повышение зарплаты на 50%',
        NULL, 6, NULL, 5, 'REJECTED',
        10, 10, 300, 'LOW', false,
        NOW() - INTERVAL '2 days', NOW() - INTERVAL '1 day', 1),

       ('Заказ нового ноутбука для нового сотрудника',
        'Нужно заказать ноутбук Dell Latitude для нового разработчика',
        NULL, 1, 2, 1, 'OPEN',
        10, 10, 2400, 'MEDIUM', false,
        NOW() - INTERVAL '4 days', NOW() - INTERVAL '2 days', 1),

       ('Миграция на новый сервер 1С',
        'Планируется перенос базы 1С на новый сервер. Нужна помощь в настройке',
        '1c:base/migration', 7, 1, 3, 'OPEN',
        1, 1, 28800, 'HIGH', true,
        NOW() - INTERVAL '12 days', NOW() - INTERVAL '4 hours', 1),

       ('Ошибка при выгрузке данных в Excel',
        'При экспорте отчета ''Свод по отделам'' вылетает ошибка формата данных',
        NULL, 5, 2, 1, 'OPEN',
        1, 1, 3600, 'MEDIUM', false,
        NOW() - INTERVAL '6 hours', NOW() - INTERVAL '1 hour', 1);

---------------------------------------------------------------------
-- 9. Обновления: рейтинг и фидбек у закрытых тикетов
---------------------------------------------------------------------
UPDATE tickets
SET rating      = 5,
    feedback    = 'Всё быстро и чётко, спасибо!',
    resolved_at = created_at + INTERVAL '2 days',
    closed_at   = created_at + INTERVAL '3 days'
WHERE title = 'Замена картриджа в принтере бухгалтерии';

UPDATE tickets
SET rating      = 4,
    feedback    = 'Решено, но долго ждал ответа',
    resolved_at = created_at + INTERVAL '1 day',
    closed_at   = created_at + INTERVAL '2 days'
WHERE title = 'Не приходит почта на новый ящик';

---------------------------------------------------------------------
-- Конец миграции
---------------------------------------------------------------------
