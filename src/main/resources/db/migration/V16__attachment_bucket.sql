-- Добавление колонки bucket для хранения имени бакета MinIO
ALTER TABLE attachments ADD COLUMN bucket VARCHAR(100);

-- Для существующих записей устанавливаем дефолтный бакет
UPDATE attachments SET bucket = 'chat-attachments' WHERE bucket IS NULL;
