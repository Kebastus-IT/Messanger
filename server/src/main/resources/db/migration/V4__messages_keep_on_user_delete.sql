ALTER TABLE messages
DROP CONSTRAINT fk_messages_sender;

ALTER TABLE messages
    ALTER COLUMN sender_user_id DROP NOT NULL;

ALTER TABLE messages
    ADD CONSTRAINT fk_messages_sender
        FOREIGN KEY (sender_user_id) REFERENCES users(id)
            ON DELETE SET NULL;