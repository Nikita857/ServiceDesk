ALTER TABLE support_lines
    ADD COLUMN telegram_chat_id BIGINT;

COMMENT ON COLUMN support_lines.telegram_chat_id IS 'ID чата/канала Telegram для уведомлений этой линии';

ALTER TABLE support_lines_aud
    ADD COLUMN telegram_chat_id BIGINT;

COMMENT ON COLUMN support_lines_aud.telegram_chat_id IS 'ID чата/канала Telegram для уведомлений этой линии';
