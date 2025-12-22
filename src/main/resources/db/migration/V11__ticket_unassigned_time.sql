-- V11: Добавление полей для отслеживания времени без назначения тикета
-- unassigned_since - когда тикет стал без assignedTo
-- total_unassigned_seconds - общее время без назначения в секундах

ALTER TABLE tickets ADD COLUMN IF NOT EXISTS unassigned_since TIMESTAMP;
ALTER TABLE tickets ADD COLUMN IF NOT EXISTS total_unassigned_seconds BIGINT DEFAULT 0;

-- Индекс для поиска тикетов без назначения
CREATE INDEX IF NOT EXISTS idx_ticket_unassigned ON tickets(unassigned_since) WHERE unassigned_since IS NOT NULL;

COMMENT ON COLUMN tickets.unassigned_since IS 'Время когда тикет стал без assignedTo (null если назначен)';
COMMENT ON COLUMN tickets.total_unassigned_seconds IS 'Общее время без назначения в секундах';
