CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE INDEX idx_users_login_trgm ON users USING GIN (LOWER(login) gin_trgm_ops);


