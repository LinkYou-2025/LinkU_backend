CREATE TABLE IF NOT EXISTS linku_search_histories (
    linku_search_history_id BIGSERIAL PRIMARY KEY,
    user_id                 BIGINT       NOT NULL,
    keyword                 VARCHAR(200) NOT NULL,
    created_at              TIMESTAMP,
    updated_at              TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_search_histories_user_id_created_at
    ON linku_search_histories (user_id, created_at DESC);
