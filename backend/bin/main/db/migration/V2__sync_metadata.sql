CREATE TABLE sync_state (
    table_name VARCHAR(64) PRIMARY KEY,
    last_successful_sync TIMESTAMP WITH TIME ZONE
);

CREATE TABLE sync_run (
    id BIGSERIAL PRIMARY KEY,
    started_at TIMESTAMP WITH TIME ZONE NOT NULL,
    finished_at TIMESTAMP WITH TIME ZONE,
    status VARCHAR(32) NOT NULL,
    error_message TEXT
);

CREATE TABLE sync_run_item (
    id BIGSERIAL PRIMARY KEY,
    run_id BIGINT NOT NULL REFERENCES sync_run (id) ON DELETE CASCADE,
    table_name VARCHAR(64) NOT NULL,
    country VARCHAR(10) NOT NULL,
    object_count INTEGER NOT NULL,
    s3_key VARCHAR(512) NOT NULL
);

CREATE INDEX idx_sync_run_started_at ON sync_run (started_at DESC);

