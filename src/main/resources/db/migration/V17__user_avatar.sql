-- V17: Добавление поля аватара для пользователей
ALTER TABLE users ADD COLUMN avatar_url VARCHAR(500);

COMMENT ON COLUMN users.avatar_url IS 'URL аватара пользователя в MinIO';
