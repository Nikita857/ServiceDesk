-- V8: Add ticket status history for time tracking and pending closure workflow

-- История статусов тикета для учёта времени
CREATE TABLE ticket_status_history (
    id BIGSERIAL PRIMARY KEY,
    ticket_id BIGINT NOT NULL REFERENCES tickets(id) ON DELETE CASCADE,
    
    -- Статус и время
    status VARCHAR(30) NOT NULL,
    entered_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    exited_at TIMESTAMP WITH TIME ZONE,
    duration_seconds BIGINT, -- автоматически вычисляется при выходе из статуса
    
    -- Кто сменил статус
    changed_by_id BIGINT REFERENCES users(id),
    
    -- Дополнительная информация
    comment TEXT,
    
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Индексы для быстрого поиска
CREATE INDEX idx_status_history_ticket ON ticket_status_history(ticket_id);
CREATE INDEX idx_status_history_ticket_status ON ticket_status_history(ticket_id, status);
CREATE INDEX idx_status_history_entered ON ticket_status_history(entered_at);

-- Добавляем поле first_response_at для времени первой реакции
ALTER TABLE tickets ADD COLUMN IF NOT EXISTS first_response_at TIMESTAMP WITH TIME ZONE;

-- Добавляем поле для хранения ID запроса на закрытие (если статус PENDING_CLOSURE)
ALTER TABLE tickets ADD COLUMN IF NOT EXISTS closure_requested_by_id BIGINT REFERENCES users(id);
ALTER TABLE tickets ADD COLUMN IF NOT EXISTS closure_requested_at TIMESTAMP WITH TIME ZONE;

COMMENT ON TABLE ticket_status_history IS 'История статусов тикета для учёта времени в каждом статусе';
COMMENT ON COLUMN ticket_status_history.duration_seconds IS 'Время в статусе (секунды), заполняется при выходе из статуса';
COMMENT ON COLUMN tickets.first_response_at IS 'Время первой реакции специалиста на тикет';
COMMENT ON COLUMN tickets.closure_requested_by_id IS 'Кто запросил закрытие тикета (для двухфакторного подтверждения)';
