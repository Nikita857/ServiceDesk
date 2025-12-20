CREATE TABLE user_activity_status
(
    user_id    BIGINT PRIMARY KEY,

    status     VARCHAR(50)              NOT NULL,

    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT fk_user_activity_status_user
        FOREIGN KEY (user_id)
            REFERENCES users (id)
            ON DELETE CASCADE
);
