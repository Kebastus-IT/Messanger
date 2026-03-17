CREATE TABLE users (
                       id VARCHAR(64) PRIMARY KEY,
                       login VARCHAR(64) NOT NULL UNIQUE,
                       display_name VARCHAR(128) NOT NULL,
                       password_hash VARCHAR(255) NOT NULL
);

CREATE TABLE chats (
                       id VARCHAR(64) PRIMARY KEY,
                       title VARCHAR(128) NOT NULL,
                       type VARCHAR(16) NOT NULL
);

CREATE TABLE chat_members (
                              chat_id VARCHAR(64) NOT NULL,
                              user_id VARCHAR(64) NOT NULL,
                              PRIMARY KEY (chat_id, user_id),
                              CONSTRAINT fk_chat_members_chat
                                  FOREIGN KEY (chat_id) REFERENCES chats(id) ON DELETE CASCADE,
                              CONSTRAINT fk_chat_members_user
                                  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE messages (
                          id BIGSERIAL PRIMARY KEY,
                          chat_id VARCHAR(64) NOT NULL,
                          sender_user_id VARCHAR(64) NOT NULL,
                          text TEXT NOT NULL,
                          created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                          CONSTRAINT fk_messages_chat
                              FOREIGN KEY (chat_id) REFERENCES chats(id) ON DELETE CASCADE,
                          CONSTRAINT fk_messages_sender
                              FOREIGN KEY (sender_user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_chat_members_user_id ON chat_members(user_id);
CREATE INDEX idx_messages_chat_id_created_at ON messages(chat_id, created_at);
CREATE INDEX idx_messages_sender_user_id ON messages(sender_user_id);
