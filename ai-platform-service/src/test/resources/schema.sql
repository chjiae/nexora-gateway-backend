CREATE TABLE IF NOT EXISTS ai_api_key (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    key_hash        VARCHAR(128)  NOT NULL UNIQUE,
    key_prefix      VARCHAR(16)   NOT NULL,
    key_name        VARCHAR(128),
    owner_user_id   BIGINT        NOT NULL,
    owner_type      VARCHAR(32)   DEFAULT 'PLATFORM',
    owner_tenant_id BIGINT,
    status          VARCHAR(32)   DEFAULT 'ACTIVE',
    allowed_models  VARCHAR(512),
    rate_limit_rpm  INT           DEFAULT 0,
    rate_limit_tpm  INT           DEFAULT 0,
    max_quota_tokens BIGINT       DEFAULT 0,
    used_quota_tokens BIGINT      DEFAULT 0,
    expire_time     TIMESTAMP,
    last_used_time  TIMESTAMP,
    create_time     TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    update_time     TIMESTAMP     DEFAULT CURRENT_TIMESTAMP
);
