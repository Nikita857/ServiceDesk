-- V9: Add rated_at field and improve rating tracking

-- Добавляем поле rated_at для отслеживания времени оценки
ALTER TABLE tickets ADD COLUMN IF NOT EXISTS rated_at TIMESTAMP WITH TIME ZONE;

COMMENT ON COLUMN tickets.rated_at IS 'Время когда пользователь поставил оценку качества обслуживания';
COMMENT ON COLUMN tickets.rating IS 'Оценка качества обслуживания от 1 до 5';
COMMENT ON COLUMN tickets.feedback IS 'Текстовый отзыв пользователя';

-- Индекс для поиска тикетов с оценками
CREATE INDEX IF NOT EXISTS idx_ticket_rating ON tickets (rating) WHERE rating IS NOT NULL;
