-- V10: Add missing audit columns for tickets_aud table

-- Добавляем недостающие колонки в audit-таблицу tickets_aud
ALTER TABLE tickets_aud ADD COLUMN IF NOT EXISTS first_response_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE tickets_aud ADD COLUMN IF NOT EXISTS closure_requested_by_id BIGINT;
ALTER TABLE tickets_aud ADD COLUMN IF NOT EXISTS closure_requested_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE tickets_aud ADD COLUMN IF NOT EXISTS rated_at TIMESTAMP WITH TIME ZONE;
