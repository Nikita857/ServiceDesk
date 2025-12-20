CREATE SEQUENCE IF NOT EXISTS user_activity_log_id_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE user_activity_logs
(
    id         BIGSERIAL PRIMARY KEY,

    user_id    BIGINT                   NOT NULL,

    event_type VARCHAR(30)              NOT NULL,

    event_time TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT fk_user_activity_logs_user
        FOREIGN KEY (user_id)
            REFERENCES users (id)
            ON DELETE CASCADE
);
CREATE INDEX idx_user_activity_user
    ON user_activity_logs (user_id);

CREATE INDEX idx_user_activity_event_time
    ON user_activity_logs (event_time);

