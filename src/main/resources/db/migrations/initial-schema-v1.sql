-- Тестовые данные для базы данных ServiceDesk
-- Основные таблицы (аудит-таблицы не заполняются)

-- Очистка существующих данных (кроме аудит-таблиц)
DO $$
    BEGIN
        IF EXISTS (SELECT FROM pg_tables WHERE schemaname = 'public' AND tablename = 'tickets') THEN
            DELETE FROM public.messages;
            DELETE FROM public.attachments;
            DELETE FROM public.assignments;
            DELETE FROM public.time_entries;
            DELETE FROM public.tickets;
        END IF;

        IF EXISTS (SELECT FROM pg_tables WHERE schemaname = 'public' AND tablename = 'wiki_article_tags') THEN
            DELETE FROM public.wiki_article_tags;
            DELETE FROM public.wiki_articles;
        END IF;

        IF EXISTS (SELECT FROM pg_tables WHERE schemaname = 'public' AND tablename = 'support_line_specialists') THEN
            DELETE FROM public.support_line_specialists;
            DELETE FROM public.support_lines;
        END IF;

        IF EXISTS (SELECT FROM pg_tables WHERE schemaname = 'public' AND tablename = 'user_roles') THEN
            DELETE FROM public.user_roles;
            DELETE FROM public.users;
        END IF;

        IF EXISTS (SELECT FROM pg_tables WHERE schemaname = 'public' AND tablename = 'categories') THEN
            DELETE FROM public.categories;
        END IF;

        IF EXISTS (SELECT FROM pg_tables WHERE schemaname = 'public' AND tablename = 'refresh_tokens') THEN
            DELETE FROM public.refresh_tokens;
        END IF;
    END $$;

-- Сброс последовательностей (тоже безопасно)
SELECT setval('users_id_seq', 1, false)        FROM pg_tables WHERE tablename = 'users';
SELECT setval('support_lines_id_seq', 1, false) FROM pg_tables WHERE tablename = 'support_lines';
SELECT setval('categories_id_seq', 1, false)    FROM pg_tables WHERE tablename = 'categories';
SELECT setval('tickets_id_seq', 1, false)       FROM pg_tables WHERE tablename = 'tickets';
SELECT setval('assignments_id_seq', 1, false)   FROM pg_tables WHERE tablename = 'assignments';
SELECT setval('messages_id_seq', 1, false)      FROM pg_tables WHERE tablename = 'messages';
SELECT setval('attachments_id_seq', 1, false)   FROM pg_tables WHERE tablename = 'attachments';
SELECT setval('time_entries_id_seq', 1, false)  FROM pg_tables WHERE tablename = 'time_entries';
SELECT setval('wiki_articles_id_seq', 1, false) FROM pg_tables WHERE tablename = 'wiki_articles';

-- 1. Пользователи
INSERT INTO public.users (username, fio, email, domain_account, password,
                          active, specialist, telegram_id, created_at, updated_at, version)
VALUES ('ivanovii', 'Иванов Иван Иванович', 'ivanov@company.com', 'COMPANY\ivanovii', '$2a$10$LAgMe0.ZVH80GaQkdKLzFOhAP0GT7X5jpx.QS6BrGaqBaa/YQ45/K', true, true,
        123456789, NOW(), NOW(), 1),
       ('petrovap', 'Петрова Анна Сергеевна', 'petrova@company.com', 'COMPANY\petrovap', '$2a$10$LAgMe0.ZVH80GaQkdKLzFOhAP0GT7X5jpx.QS6BrGaqBaa/YQ45/K', true, true,
        987654321, NOW(), NOW(), 1),
       ('sidorovms', 'Сидоров Михаил Владимирович', 'sidorov@company.com', 'COMPANY\sidorovms', '$2a$10$LAgMe0.ZVH80GaQkdKLzFOhAP0GT7X5jpx.QS6BrGaqBaa/YQ45/K', true,
        true, 555555555, NOW(), NOW(), 1),
       ('smirnovaek', 'Смирнова Екатерина Андреевна', 'smirnova@company.com', 'COMPANY\smirnovaek', '$2a$10$LAgMe0.ZVH80GaQkdKLzFOhAP0GT7X5jpx.QS6BrGaqBaa/YQ45/K',
        true, true, 111222333, NOW(), NOW(), 1),
       ('kuznetsovda', 'Кузнецов Дмитрий Александрович', 'kuznetsov@company.com', 'COMPANY\kuznetsovda',
        '$2a$10$LAgMe0.ZVH80GaQkdKLzFOhAP0GT7X5jpx.QS6BrGaqBaa/YQ45/K', true, false, NULL, NOW(), NOW(), 1),
       ('nikolaevai', 'Николаева Инна Петровна', 'nikolaeva@company.com', 'COMPANY\nikolaevai', '$2a$10$LAgMe0.ZVH80GaQkdKLzFOhAP0GT7X5jpx.QS6BrGaqBaa/YQ45/K', true,
        false, NULL, NOW(), NOW(), 1),
       ('admin', 'Администратор Системы', 'admin@company.com', 'COMPANY\admin', '$2a$10$LAgMe0.ZVH80GaQkdKLzFOhAP0GT7X5jpx.QS6BrGaqBaa/YQ45/K', true, true,
        999999999, NOW(), NOW(), 1),
       ('support1', 'Специалист поддержки 1', 'support1@company.com', 'COMPANY\support1', '$2a$10$LAgMe0.ZVH80GaQkdKLzFOhAP0GT7X5jpx.QS6BrGaqBaa/YQ45/K', true, true,
        888888888, NOW(), NOW(), 1);

-- 2. Роли пользователей
INSERT INTO public.user_roles (user_id, role)
VALUES (1, 'ROLE_SPECIALIST'),
       (1, 'ROLE_USER'),
       (2, 'ROLE_SPECIALIST'),
       (2, 'ROLE_TEAM_LEAD'),
       (3, 'ROLE_SPECIALIST'),
       (4, 'ROLE_SPECIALIST'),
       (5, 'ROLE_USER'),
       (6, 'ROLE_USER'),
       (7, 'ROLE_ADMIN'),
       (7, 'ROLE_SPECIALIST'),
       (8, 'ROLE_SPECIALIST');

-- 3. Линии поддержки
INSERT INTO public.support_lines (name, description, assignment_mode, sla_minutes,
                                  display_order, last_assigned_index, created_at, updated_at, version)
VALUES ('Техническая поддержка', 'Решение технических вопросов по ПО и оборудованию', 'ROUND_ROBIN', 240, 1, 0, NOW(),
        NOW(), 1),
       ('Бухгалтерия и финансы', 'Вопросы по зарплате, отчетности и финансам', 'FIRST_AVAILABLE', 480, 2, 0, NOW(),
        NOW(), 1),
       ('Кадровая служба', 'Вопросы по кадрам, отпускам, больничным', 'LEAST_LOADED', 360, 3, 0, NOW(), NOW(), 1),
       ('ИТ-инфраструктура', 'Сервера, сети, телефония', 'DIRECT', 120, 4, 0, NOW(), NOW(), 1),
       ('Общие вопросы', 'Общие организационные вопросы', 'ROUND_ROBIN', 1440, 5, 0, NOW(), NOW(), 1);

-- 4. Связь специалистов с линиями поддержки
INSERT INTO public.support_line_specialists (line_id, user_id)
VALUES (1, 1),
       (1, 2),
       (1, 3), -- Техподдержка
       (2, 2),
       (2, 4), -- Бухгалтерия
       (3, 1),
       (3, 4), -- Кадры
       (4, 3),
       (4, 7), -- ИТ-инфраструктура
       (5, 1),
       (5, 2),
       (5, 3),
       (5, 4),
       (5, 8);
-- Общие вопросы

-- 5. Категории заявок
INSERT INTO public.categories (name, description, type, display_order,
                               user_selectable, created_at, updated_at, version)
VALUES ('Проблемы с ПО', 'Проблемы с установкой, запуском или работой программного обеспечения', 'GENERAL', 1, true,
        NOW(), NOW(), 1),
       ('Оборудование', 'Неисправности компьютеров, принтеров, сканеров и другого оборудования', 'GENERAL', 2, true,
        NOW(), NOW(), 1),
       ('Сеть и интернет', 'Проблемы с подключением к сети, интернету, Wi-Fi', 'GENERAL', 3, true, NOW(), NOW(), 1),
       ('Учетная запись', 'Проблемы с входом в систему, сброс пароля', 'GENERAL', 4, true, NOW(), NOW(), 1),
       ('Зарплата', 'Вопросы по начислению зарплаты, премиям', 'GENERAL', 5, true, NOW(), NOW(), 1),
       ('Отпуск', 'Оформление отпуска, перенос отпуска', 'GENERAL', 6, true, NOW(), NOW(), 1),
       ('Срочные проблемы', 'Срочные проблемы, требующие немедленного решения', 'ESCALATION', 7, false, NOW(), NOW(),
        1),
       ('Системные уведомления', 'Системные сообщения и уведомления', 'SYSTEM', 8, false, NOW(), NOW(), 1),
       ('Обучение', 'Запросы на обучение, инструктаж', 'GENERAL', 9, true, NOW(), NOW(), 1),
       ('Запрос техники', 'Заказ нового оборудования или ПО', 'GENERAL', 10, true, NOW(), NOW(), 1);

-- 6. Заявки (тикеты)
INSERT INTO public.tickets (title, description, priority, status,
                            created_by_id, category_user_id, category_support_id,
                            support_line_id, created_at, updated_at, version,
                            escalated, rating, time_spent_seconds)
VALUES ('Не запускается Excel',
        'При попытке запустить Microsoft Excel выдает ошибку "Приложение не может быть запущено". Перезагрузка компьютера не помогает.',
        'MEDIUM', 'OPEN', 5, 1, 1, 1, NOW() - INTERVAL '5 days', NOW() - INTERVAL '1 day', 1,
        false, NULL, 1800),
       ('Не печатает принтер в отделе бухгалтерии',
        'Принтер HP LaserJet 4000 в бухгалтерии не печатает. Горит ошибка "Замятие бумаги", хотя бумага не замята.',
        'HIGH', 'RESOLVED', 6, 2, 2, 1, NOW() - INTERVAL '3 days', NOW() - INTERVAL '12 hours', 1,
        false, 5, 3600),
       ('Задержка зарплаты',
        'Зарплата за октябрь до сих пор не поступила на карту. Все коллеги уже получили.',
        'HIGH', 'PENDING', 5, 5, 5, 2, NOW() - INTERVAL '2 days', NOW(), 1,
        false, NULL, 900),
       ('Нет доступа к сетевому диску',
        'После переустановки системы потерял доступ к сетевому диску Z:. Нужны права доступа.',
        'MEDIUM', 'NEW', 5, 3, 3, 4, NOW() - INTERVAL '1 day', NOW() - INTERVAL '1 day', 1,
        false, NULL, 1200),
       ('Оформление отпуска с 15.11',
        'Прошу оформить ежегодный оплачиваемый отпуск с 15 ноября на 14 календарных дней.',
        'LOW', 'OPEN', 6, 6, 6, 3, NOW() - INTERVAL '4 days', NOW() - INTERVAL '2 days', 1,
        false, NULL, 600),
       ('Срочно! Не работает сервер 1С',
        'Сервер 1С предприятия недоступен. Весь отдел не может работать. Требуется срочное решение!',
        'URGENT', 'ESCALATED', 5, 7, 7, 4, NOW() - INTERVAL '6 hours', NOW() - INTERVAL '1 hour', 1,
        true, NULL, 7200),
       ('Заказать новый монитор',
        'Текущий монитор мигает и искажает цвета. Прошу заказать замену - монитор Dell 24".',
        'LOW', 'RESOLVED', 6, 10, 10, 1, NOW() - INTERVAL '7 days', NOW() - INTERVAL '5 days', 1,
        false, 4, 1800),
       ('Сброс пароля доменной учетной записи',
        'Забыл пароль от учетной записи в домене. Требуется сброс пароля.',
        'MEDIUM', 'CLOSED', 5, 4, 4, 4, NOW() - INTERVAL '10 days', NOW() - INTERVAL '8 days', 1,
        false, 5, 1500),
       ('Обучение работе с новой CRM',
        'Требуется обучение работе с новой системой CRM для отдела продаж.',
        'LOW', 'OPEN', 6, 9, 9, 5, NOW() - INTERVAL '3 days', NOW() - INTERVAL '1 day', 1,
        false, NULL, 2400),
       ('Медленный интернет',
        'Очень медленная скорость интернета в течение всего дня. Скорость не превышает 1 Мбит/с.',
        'MEDIUM', 'NEW', 5, 3, 3, 4, NOW() - INTERVAL '8 hours', NOW(), 1,
        false, NULL, NULL);

-- 7. Назначения
INSERT INTO public.assignments (ticket_id, from_line_id, to_line_id, from_user_id, to_user_id,
                                status, mode, created_at, accepted_at, version)
VALUES (1, NULL, 1, NULL, 1, 'ACCEPTED', 'ROUND_ROBIN', NOW() - INTERVAL '5 days', NOW() - INTERVAL '4 days', 1),
       (2, NULL, 1, NULL, 2, 'ACCEPTED', 'FIRST_AVAILABLE', NOW() - INTERVAL '3 days', NOW() - INTERVAL '2 days', 1),
       (3, NULL, 2, NULL, 4, 'PENDING', 'FIRST_AVAILABLE', NOW() - INTERVAL '2 days', NULL, 1),
       (4, NULL, 4, NULL, 3, 'ACCEPTED', 'DIRECT', NOW() - INTERVAL '1 day', NOW() - INTERVAL '20 hours', 1),
       (5, NULL, 3, NULL, 1, 'ACCEPTED', 'LEAST_LOADED', NOW() - INTERVAL '4 days', NOW() - INTERVAL '3 days', 1),
       (6, 4, 4, 3, 7, 'ACCEPTED', 'DIRECT', NOW() - INTERVAL '6 hours', NOW() - INTERVAL '5 hours', 1),
       (7, NULL, 1, NULL, 2, 'ACCEPTED', 'ROUND_ROBIN', NOW() - INTERVAL '7 days', NOW() - INTERVAL '6 days', 1),
       (8, NULL, 4, NULL, 3, 'ACCEPTED', 'DIRECT', NOW() - INTERVAL '10 days', NOW() - INTERVAL '9 days', 1),
       (9, NULL, 5, NULL, 8, 'ACCEPTED', 'ROUND_ROBIN', NOW() - INTERVAL '3 days', NOW() - INTERVAL '2 days', 1),
       (10, NULL, 4, NULL, 7, 'PENDING', 'LEAST_LOADED', NOW() - INTERVAL '8 hours', NULL, 1);

-- Обновляем назначенных специалистов в тикетах
UPDATE public.tickets
SET assigned_to_id = 1
WHERE id = 1;
UPDATE public.tickets
SET assigned_to_id = 2
WHERE id = 2;
UPDATE public.tickets
SET assigned_to_id = 4
WHERE id = 3;
UPDATE public.tickets
SET assigned_to_id = 3
WHERE id = 4;
UPDATE public.tickets
SET assigned_to_id = 1
WHERE id = 5;
UPDATE public.tickets
SET assigned_to_id = 7
WHERE id = 6;
UPDATE public.tickets
SET assigned_to_id = 2
WHERE id = 7;
UPDATE public.tickets
SET assigned_to_id = 3
WHERE id = 8;
UPDATE public.tickets
SET assigned_to_id = 8
WHERE id = 9;
UPDATE public.tickets
SET assigned_to_id = 7
WHERE id = 10;

-- Обновляем даты разрешения для закрытых тикетов
UPDATE public.tickets
SET resolved_at = NOW() - INTERVAL '1 day', closed_at = NOW() - INTERVAL '1 day'
WHERE id = 2;
UPDATE public.tickets
SET resolved_at = NOW() - INTERVAL '6 days', closed_at = NOW() - INTERVAL '5 days'
WHERE id = 7;
UPDATE public.tickets
SET resolved_at = NOW() - INTERVAL '9 days', closed_at = NOW() - INTERVAL '8 days'
WHERE id = 8;

-- 8. Сообщения в тикетах
INSERT INTO public.messages (ticket_id, sender_id, sender_type, content,
                             is_internal, created_at, updated_at, version,
                             read_by_specialist_at, read_by_user_at)
VALUES (1, 5, 'USER', 'Здравствуйте! У меня не запускается Excel. Что можно сделать?', false, NOW() - INTERVAL '5 days',
        NOW() - INTERVAL '5 days', 1, NOW() - INTERVAL '4 days', NOW() - INTERVAL '4 days'),
       (1, 1, 'SPECIALIST', 'Иван, попробуйте выполнить восстановление Office через панель управления.', false,
        NOW() - INTERVAL '4 days 2 hours', NOW() - INTERVAL '4 days 2 hours', 1, NOW() - INTERVAL '4 days',
        NOW() - INTERVAL '4 days'),
       (1, 5, 'USER', 'Попробовал - не помогло. Ошибка все та же.', false, NOW() - INTERVAL '4 days 1 hour',
        NOW() - INTERVAL '4 days 1 hour', 1, NOW() - INTERVAL '3 days', NOW() - INTERVAL '4 days'),

       (2, 6, 'USER', 'Принтер в бухгалтерии не печатает, выдает ошибку замятия.', false, NOW() - INTERVAL '3 days',
        NOW() - INTERVAL '3 days', 1, NOW() - INTERVAL '2 days', NOW() - INTERVAL '2 days'),
       (2, 2, 'SPECIALIST',
        'Анна, проверьте, нет ли действительно маленького кусочка бумаги внутри. Иногда его не видно.', false,
        NOW() - INTERVAL '2 days 3 hours', NOW() - INTERVAL '2 days 3 hours', 1, NOW() - INTERVAL '2 days',
        NOW() - INTERVAL '2 days'),
       (2, 6, 'USER', 'Нашли и убрали кусочек бумаги. Теперь работает, спасибо!', false, NOW() - INTERVAL '1 day',
        NOW() - INTERVAL '1 day', 1, NOW() - INTERVAL '1 day', NOW() - INTERVAL '1 day'),

       (3, 5, 'USER', 'Добрый день, зарплата еще не пришла. Все коллеги получили.', false, NOW() - INTERVAL '2 days',
        NOW() - INTERVAL '2 days', 1, NOW() - INTERVAL '1 day', NOW() - INTERVAL '1 day'),
       (3, 4, 'SPECIALIST', 'Дмитрий, проверим с банком. Возможно задержка по их вине.', true, NOW() - INTERVAL '1 day',
        NOW() - INTERVAL '1 day', 1, NOW() - INTERVAL '1 day', NULL),

       (6, 7, 'ADMIN', 'Сервер 1С перезагружен. Проверяйте доступность.', false, NOW() - INTERVAL '4 hours',
        NOW() - INTERVAL '4 hours', 1, NOW() - INTERVAL '3 hours', NOW() - INTERVAL '3 hours'),
       (6, 5, 'USER', 'Доступ появился, спасибо за оперативность!', false, NOW() - INTERVAL '3 hours',
        NOW() - INTERVAL '3 hours', 1, NOW() - INTERVAL '2 hours', NOW() - INTERVAL '2 hours'),

       (8, 3, 'SPECIALIST', 'Пароль сброшен. Новый пароль: Temp12345. Смените при первом входе.', false,
        NOW() - INTERVAL '9 days', NOW() - INTERVAL '9 days', 1, NOW() - INTERVAL '8 days', NOW() - INTERVAL '8 days'),

       (9, 8, 'SPECIALIST', 'Запланировали обучение на следующую среду в 14:00 в переговорной №3.', false,
        NOW() - INTERVAL '2 days', NOW() - INTERVAL '2 days', 1, NOW() - INTERVAL '1 day', NOW() - INTERVAL '1 day');

-- 9. Вложения
INSERT INTO public.attachments (ticket_id, message_id, uploaded_by_id, filename,
                                url, type, mime_type, file_size_bytes,
                                created_at, version)
VALUES (1, NULL, 5, 'excel_error.png', 'https://storage.company.com/attachments/1/excel_error.png', 'SCREENSHOT',
        'image/png', 102400, NOW() - INTERVAL '5 days', 1),
       (2, NULL, 6, 'printer_error.jpg', 'https://storage.company.com/attachments/2/printer_error.jpg', 'PHOTO',
        'image/jpeg', 204800, NOW() - INTERVAL '3 days', 1),
       (6, 9, 7, 'server_logs.txt', 'https://storage.company.com/attachments/6/server_logs.txt', 'DOCUMENT',
        'text/plain', 51200, NOW() - INTERVAL '4 hours', 1),
       (7, NULL, 6, 'monitor_spec.pdf', 'https://storage.company.com/attachments/7/monitor_spec.pdf', 'DOCUMENT',
        'application/pdf', 1536000, NOW() - INTERVAL '7 days', 1);

-- 10. Учет времени
INSERT INTO public.time_entries (ticket_id, specialist_id, duration_seconds,
                                 activity_type, billable, note, entry_date,
                                 work_date, created_at, updated_at, version)
VALUES (1, 1, 1800, 'WORK', true, 'Диагностика проблемы с Excel, удаленное подключение', NOW() - INTERVAL '4 days',
        (NOW() - INTERVAL '4 days'):: date, NOW() - INTERVAL '4 days', NOW() - INTERVAL '4 days', 1),
       (2, 2, 3600, 'WORK', true, 'Ремонт принтера, замена ролика подачи бумаги', NOW() - INTERVAL '2 days',
        (NOW() - INTERVAL '2 days'):: date, NOW() - INTERVAL '2 days', NOW() - INTERVAL '2 days', 1),
       (6, 7, 7200, 'WORK', true, 'Экстренное восстановление сервера 1С', NOW() - INTERVAL '5 hours',
        (NOW() - INTERVAL '5 hours'):: date, NOW() - INTERVAL '5 hours', NOW() - INTERVAL '5 hours', 1),
       (8, 3, 1500, 'SUPPORT', true, 'Сброс пароля, инструктаж пользователя', NOW() - INTERVAL '9 days',
        (NOW() - INTERVAL '9 days'):: date, NOW() - INTERVAL '9 days', NOW() - INTERVAL '9 days', 1),
       (7, 2, 1800, 'DOCUMENTATION', true, 'Оформление заявки на закупку монитора', NOW() - INTERVAL '6 days',
        (NOW() - INTERVAL '6 days'):: date, NOW() - INTERVAL '6 days', NOW() - INTERVAL '6 days', 1),
       (9, 8, 2400, 'TRAINING', true, 'Подготовка материалов для обучения CRM', NOW() - INTERVAL '2 days',
        (NOW() - INTERVAL '2 days'):: date, NOW() - INTERVAL '2 days', NOW() - INTERVAL '2 days', 1),
       (4, 3, 1200, 'WORK', true, 'Настройка доступа к сетевым дискам', NOW() - INTERVAL '20 hours',
        (NOW() - INTERVAL '20 hours'):: date, NOW() - INTERVAL '20 hours', NOW() - INTERVAL '20 hours', 1);

-- 11. Refresh токены (примеры)
INSERT INTO public.refresh_tokens (user_id, token, expiry_date)
VALUES (1, 'refresh_token_ivanov', NOW() + INTERVAL '30 days'),
       (2, 'refresh_token_petrova', NOW() + INTERVAL '30 days'),
       (7, 'refresh_token_admin', NOW() + INTERVAL '7 days');

-- 12. Wiki статьи
INSERT INTO public.wiki_articles (title, slug, excerpt, content,
                                  category_id, created_by_id, updated_by_id,
                                  view_count, like_count, tags,
                                  created_at, updated_at, version)
VALUES ('Как сбросить пароль доменной учетной записи',
        'kak-sbrosit-parol-domennoj-uchetnoj-zapisi',
        'Инструкция по сбросу пароля учетной записи в домене компании',
        '1. Обратитесь в службу ИТ-поддержки\n2. Предоставьте ваши данные\n3. Получите временный пароль\n4. Смените пароль при первом входе',
        4, 3, 3,
        156, 12, 'пароль,сброс,учетная запись',
        NOW() - INTERVAL '30 days', NOW() - INTERVAL '5 days', 1),
       ('Решение проблем с печатью',
        'reshenie-problem-s-pechatyu',
        'Частые проблемы с принтерами и их решение',
        '1. Проверьте подключение принтера\n2. Проверьте наличие бумаги\n3. Очистите очередь печати\n4. Перезагрузите принтер',
        2, 2, 1,
        89, 8, 'принтер,печать,проблемы',
        NOW() - INTERVAL '25 days', NOW() - INTERVAL '10 days', 1),
       ('Порядок оформления отпуска',
        'poryadok-oformleniya-otpuska',
        'Пошаговая инструкция по оформлению ежегодного отпуска',
        '1. Напишите заявление на отпуск\n2. Согласуйте с руководителем\n3. Отправьте заявление в кадры\n4. Дождитесь приказа',
        6, 1, 4,
        203, 25, 'отпуск,оформление,кадры',
        NOW() - INTERVAL '45 days', NOW() - INTERVAL '15 days', 1),
       ('Настройка доступа к сетевым дискам',
        'nastrojka-dostupa-k-setevym-diskam',
        'Инструкция по настройке доступа к сетевым ресурсам',
        '1. Войдите в систему под своей учетной записью\n2. Откройте "Этот компьютер"\n3. Нажмите "Подключить сетевой диск"\n4. Введите путь к ресурсу',
        3, 3, 7,
        112, 15, 'сеть,диск,доступ',
        NOW() - INTERVAL '20 days', NOW() - INTERVAL '3 days', 1);

-- 13. Теги для wiki статей (отдельная таблица)
INSERT INTO public.wiki_article_tags (article_id, tag)
VALUES (1, 'пароль'),
       (1, 'сброс'),
       (1, 'учетная запись'),
       (2, 'принтер'),
       (2, 'печать'),
       (2, 'проблемы'),
       (3, 'отпуск'),
       (3, 'оформление'),
       (3, 'кадры'),
       (4, 'сеть'),
       (4, 'диск'),
       (4, 'доступ');

-- Обновляем SLA дедлайны для тикетов на основе линий поддержки
UPDATE public.tickets t
SET sla_deadline = t.created_at + (sl.sla_minutes || ' minutes')::interval
FROM public.support_lines sl
WHERE t.support_line_id = sl.id;

COMMIT;

-- Проверка количества записей
SELECT 'users' as table_name,
       COUNT(*) as count
FROM public.users
UNION ALL
SELECT 'support_lines', COUNT(*)
FROM public.support_lines
UNION ALL
SELECT 'categories', COUNT(*)
FROM public.categories
UNION ALL
SELECT 'tickets', COUNT(*)
FROM public.tickets
UNION ALL
SELECT 'assignments', COUNT(*)
FROM public.assignments
UNION ALL
SELECT 'messages', COUNT(*)
FROM public.messages
UNION ALL
SELECT 'attachments', COUNT(*)
FROM public.attachments
UNION ALL
SELECT 'time_entries', COUNT(*)
FROM public.time_entries
UNION ALL
SELECT 'wiki_articles', COUNT(*)
FROM public.wiki_articles
UNION ALL
SELECT 'user_roles', COUNT(*)
FROM public.user_roles
UNION ALL
SELECT 'support_line_specialists', COUNT(*)
FROM public.support_line_specialists
UNION ALL
SELECT 'wiki_article_tags', COUNT(*)
FROM public.wiki_article_tags
ORDER BY table_name;