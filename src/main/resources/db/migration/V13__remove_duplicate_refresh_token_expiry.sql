-- V13: Удаление дублирующего поля refresh_token_expiry_date из users
-- Дата истечения уже хранится в таблице refresh_tokens

ALTER TABLE users DROP COLUMN IF EXISTS refresh_token_expiry_date;
