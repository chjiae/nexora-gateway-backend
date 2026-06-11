-- ============================================================
-- Nexora Gateway V1 Schema
-- Multi-tenant AI API Gateway Platform
-- ============================================================

-- ============================================================
-- 1. User / Tenant / Permission
-- ============================================================

CREATE TABLE tenant (
    id              BIGSERIAL PRIMARY KEY,
    tenant_name     VARCHAR(128)  NOT NULL,
    tenant_code     VARCHAR(64)   NOT NULL UNIQUE,
    status          VARCHAR(32)   NOT NULL DEFAULT 'ACTIVE',
    contact_name    VARCHAR(128),
    contact_email   VARCHAR(256),
    contact_phone   VARCHAR(32),
    max_users       INT           DEFAULT 100,
    max_providers   INT           DEFAULT 10,
    extra_config    JSONB,
    remark          TEXT,
    create_time     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_tenant_code ON tenant(tenant_code);
CREATE INDEX idx_tenant_status ON tenant(status);

CREATE TABLE sys_user (
    id              BIGSERIAL PRIMARY KEY,
    username        VARCHAR(128)  NOT NULL,
    email           VARCHAR(256),
    phone           VARCHAR(32),
    password_hash   VARCHAR(256)  NOT NULL,
    user_type       VARCHAR(32)   NOT NULL,
    owner_type      VARCHAR(32)   NOT NULL DEFAULT 'PLATFORM',
    owner_tenant_id BIGINT,
    avatar_url      VARCHAR(512),
    status          VARCHAR(32)   NOT NULL DEFAULT 'ACTIVE',
    extra_info      JSONB,
    last_login_time TIMESTAMP,
    create_time     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_sys_user_username ON sys_user(username);
CREATE INDEX idx_sys_user_owner ON sys_user(owner_type, owner_tenant_id);
CREATE INDEX idx_sys_user_status ON sys_user(status);

CREATE TABLE sys_role (
    id              BIGSERIAL PRIMARY KEY,
    role_code       VARCHAR(64)   NOT NULL UNIQUE,
    role_name       VARCHAR(128)  NOT NULL,
    role_type       VARCHAR(32)   NOT NULL,
    owner_tenant_id BIGINT,
    description     TEXT,
    create_time     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_sys_role_tenant ON sys_role(owner_tenant_id);

CREATE TABLE sys_permission (
    id              BIGSERIAL PRIMARY KEY,
    perm_code       VARCHAR(128)  NOT NULL UNIQUE,
    perm_name       VARCHAR(128)  NOT NULL,
    perm_type       VARCHAR(32)   NOT NULL DEFAULT 'MENU',
    parent_id       BIGINT        DEFAULT 0,
    path            VARCHAR(256),
    create_time     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE sys_user_role (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT        NOT NULL,
    role_id         BIGINT        NOT NULL,
    create_time     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX uk_user_role ON sys_user_role(user_id, role_id);

CREATE TABLE sys_role_permission (
    id              BIGSERIAL PRIMARY KEY,
    role_id         BIGINT        NOT NULL,
    permission_id   BIGINT        NOT NULL,
    create_time     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX uk_role_perm ON sys_role_permission(role_id, permission_id);

-- ============================================================
-- 2. API Key
-- ============================================================

CREATE TABLE ai_api_key (
    id              BIGSERIAL PRIMARY KEY,
    key_hash        VARCHAR(128)  NOT NULL UNIQUE,
    key_prefix      VARCHAR(16)   NOT NULL,
    key_name        VARCHAR(128),
    owner_user_id   BIGINT        NOT NULL,
    owner_type      VARCHAR(32)   NOT NULL DEFAULT 'PLATFORM',
    owner_tenant_id BIGINT,
    status          VARCHAR(32)   NOT NULL DEFAULT 'ACTIVE',
    allowed_models  JSONB,
    rate_limit_rpm  INT           DEFAULT 0,
    rate_limit_tpm  INT           DEFAULT 0,
    max_quota_tokens BIGINT       DEFAULT 0,
    used_quota_tokens BIGINT      DEFAULT 0,
    expire_time     TIMESTAMP,
    last_used_time  TIMESTAMP,
    create_time     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_api_key_owner ON ai_api_key(owner_user_id);
CREATE INDEX idx_api_key_prefix ON ai_api_key(key_prefix);
CREATE INDEX idx_api_key_status ON ai_api_key(status);

-- ============================================================
-- 3. Provider / Endpoint / Route / Price
-- ============================================================

CREATE TABLE ai_provider_config (
    id              BIGSERIAL PRIMARY KEY,
    provider_name   VARCHAR(128)  NOT NULL,
    provider_type   VARCHAR(32)   NOT NULL,
    display_name    VARCHAR(256),
    owner_type      VARCHAR(32)   NOT NULL DEFAULT 'PLATFORM',
    owner_tenant_id BIGINT,
    enabled         BOOLEAN       NOT NULL DEFAULT TRUE,
    extra_config    JSONB,
    remark          TEXT,
    create_time     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_provider_owner ON ai_provider_config(owner_type, owner_tenant_id);
CREATE INDEX idx_provider_enabled ON ai_provider_config(enabled);

CREATE TABLE ai_provider_endpoint (
    id                  BIGSERIAL PRIMARY KEY,
    provider_id         BIGINT        NOT NULL,
    endpoint_name       VARCHAR(128)  NOT NULL,
    base_url            VARCHAR(512)  NOT NULL,
    path_prefix         VARCHAR(128),
    supported_protocols JSONB         NOT NULL DEFAULT '[]',
    default_protocol    VARCHAR(32),
    auth_type           VARCHAR(32)   NOT NULL DEFAULT 'API_KEY',
    api_key_encrypted   TEXT,
    extra_headers       JSONB,
    timeout_ms          INT           DEFAULT 60000,
    connect_timeout_ms  INT           DEFAULT 10000,
    read_timeout_ms     INT           DEFAULT 60000,
    max_retries         INT           DEFAULT 2,
    priority            INT           DEFAULT 100,
    weight              INT           DEFAULT 100,
    owner_type          VARCHAR(32)   NOT NULL DEFAULT 'PLATFORM',
    owner_tenant_id     BIGINT,
    enabled             BOOLEAN       NOT NULL DEFAULT TRUE,
    create_time         TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time         TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_endpoint_provider ON ai_provider_endpoint(provider_id);
CREATE INDEX idx_endpoint_owner ON ai_provider_endpoint(owner_type, owner_tenant_id);
CREATE INDEX idx_endpoint_enabled ON ai_provider_endpoint(enabled);

CREATE TABLE ai_model_route (
    id                      BIGSERIAL PRIMARY KEY,
    model_alias             VARCHAR(128)  NOT NULL,
    provider_id             BIGINT        NOT NULL,
    endpoint_id             BIGINT        NOT NULL,
    upstream_model          VARCHAR(128)  NOT NULL,
    upstream_protocol       VARCHAR(32)   NOT NULL,
    client_protocol_support JSONB         DEFAULT '["OPENAI"]',
    priority                INT           DEFAULT 100,
    weight                  INT           DEFAULT 100,
    fallback_group          VARCHAR(64),
    support_stream          BOOLEAN       DEFAULT TRUE,
    support_tools           BOOLEAN       DEFAULT FALSE,
    support_vision          BOOLEAN       DEFAULT FALSE,
    support_reasoning       BOOLEAN       DEFAULT FALSE,
    max_input_tokens        INT           DEFAULT 128000,
    max_output_tokens       INT           DEFAULT 16384,
    price_config_id         BIGINT,
    owner_type              VARCHAR(32)   NOT NULL DEFAULT 'PLATFORM',
    owner_tenant_id         BIGINT,
    enabled                 BOOLEAN       NOT NULL DEFAULT TRUE,
    create_time             TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time             TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_route_alias ON ai_model_route(model_alias);
CREATE INDEX idx_route_provider ON ai_model_route(provider_id, endpoint_id);
CREATE INDEX idx_route_owner ON ai_model_route(owner_type, owner_tenant_id);
CREATE INDEX idx_route_enabled ON ai_model_route(enabled);

CREATE TABLE ai_model_price (
    id                  BIGSERIAL PRIMARY KEY,
    model_alias         VARCHAR(128)  NOT NULL,
    input_price_per_1k  NUMERIC(12,8) NOT NULL DEFAULT 0,
    output_price_per_1k NUMERIC(12,8) NOT NULL DEFAULT 0,
    cache_read_price_per_1k NUMERIC(12,8) DEFAULT 0,
    currency            VARCHAR(8)    DEFAULT 'USD',
    owner_type          VARCHAR(32)   NOT NULL DEFAULT 'PLATFORM',
    owner_tenant_id     BIGINT,
    create_time         TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time         TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_price_alias ON ai_model_price(model_alias);
CREATE INDEX idx_price_owner ON ai_model_price(owner_type, owner_tenant_id);

CREATE TABLE ai_endpoint_health_state (
    id                  BIGSERIAL PRIMARY KEY,
    endpoint_id         BIGINT        NOT NULL UNIQUE,
    health_status       VARCHAR(32)   NOT NULL DEFAULT 'HEALTHY',
    consecutive_failures INT          DEFAULT 0,
    total_requests      BIGINT        DEFAULT 0,
    total_failures      BIGINT        DEFAULT 0,
    last_check_time     TIMESTAMP,
    last_success_time   TIMESTAMP,
    last_failure_time   TIMESTAMP,
    cooldown_until      TIMESTAMP,
    extra_info          JSONB,
    create_time         TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time         TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_health_status ON ai_endpoint_health_state(health_status);

-- ============================================================
-- 4. Logs / Billing / Events
-- ============================================================

CREATE TABLE ai_request_log (
    id                  BIGSERIAL PRIMARY KEY,
    request_id          VARCHAR(64)   NOT NULL,
    tenant_id           BIGINT,
    user_id             BIGINT,
    api_key_id          BIGINT,
    model_alias         VARCHAR(128),
    provider_id         BIGINT,
    endpoint_id         BIGINT,
    upstream_model      VARCHAR(128),
    upstream_protocol   VARCHAR(32),
    client_protocol     VARCHAR(32),
    stream              BOOLEAN       DEFAULT FALSE,
    input_tokens        BIGINT        DEFAULT 0,
    output_tokens       BIGINT        DEFAULT 0,
    total_tokens        BIGINT        DEFAULT 0,
    latency_ms          BIGINT        DEFAULT 0,
    first_token_latency_ms BIGINT     DEFAULT 0,
    success             BOOLEAN       NOT NULL,
    error_code          VARCHAR(64),
    error_message       TEXT,
    http_status_code    INT,
    finish_reason       VARCHAR(32),
    created_at          TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_req_log_request ON ai_request_log(request_id);
CREATE INDEX idx_req_log_tenant ON ai_request_log(tenant_id);
CREATE INDEX idx_req_log_user ON ai_request_log(user_id);
CREATE INDEX idx_req_log_time ON ai_request_log(created_at);

CREATE TABLE ai_billing_record (
    id                  BIGSERIAL PRIMARY KEY,
    event_id            VARCHAR(64)   NOT NULL UNIQUE,
    request_id          VARCHAR(64)   NOT NULL,
    tenant_id           BIGINT,
    user_id             BIGINT,
    api_key_id          BIGINT,
    model_alias         VARCHAR(128),
    provider_id         BIGINT,
    endpoint_id         BIGINT,
    upstream_model      VARCHAR(128),
    input_tokens        BIGINT        DEFAULT 0,
    output_tokens       BIGINT        DEFAULT 0,
    total_tokens        BIGINT        DEFAULT 0,
    upstream_cost       NUMERIC(12,8) DEFAULT 0,
    platform_charge     NUMERIC(12,8) DEFAULT 0,
    tenant_charge       NUMERIC(12,8) DEFAULT 0,
    platform_profit     NUMERIC(12,8) DEFAULT 0,
    tenant_profit       NUMERIC(12,8) DEFAULT 0,
    billing_status      VARCHAR(32)   DEFAULT 'PENDING',
    billed_at           TIMESTAMP,
    create_time         TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_billing_req ON ai_billing_record(request_id);
CREATE INDEX idx_billing_tenant ON ai_billing_record(tenant_id);
CREATE INDEX idx_billing_user ON ai_billing_record(user_id);
CREATE INDEX idx_billing_time ON ai_billing_record(create_time);

CREATE TABLE ai_event_consume_log (
    id                  BIGSERIAL PRIMARY KEY,
    event_id            VARCHAR(64)   NOT NULL,
    event_type          VARCHAR(64)   NOT NULL,
    channel             VARCHAR(128),
    consume_status      VARCHAR(32)   NOT NULL DEFAULT 'RECEIVED',
    retry_count         INT           DEFAULT 0,
    payload             JSONB,
    error_message       TEXT,
    consumed_at         TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_event_log_id ON ai_event_consume_log(event_id);
CREATE INDEX idx_event_log_status ON ai_event_consume_log(consume_status);

-- ============================================================
-- Seed Data: Platform Super Admin
-- ============================================================

INSERT INTO tenant (id, tenant_name, tenant_code, status)
VALUES (1, 'Platform Default', 'PLATFORM', 'ACTIVE');

-- Default roles
INSERT INTO sys_role (id, role_code, role_name, role_type) VALUES
(1, 'PLATFORM_SUPER_ADMIN', 'Platform Super Admin', 'PLATFORM'),
(2, 'PLATFORM_ADMIN', 'Platform Admin', 'PLATFORM'),
(3, 'TENANT_OWNER', 'Tenant Owner', 'TENANT'),
(4, 'TENANT_ADMIN', 'Tenant Admin', 'TENANT'),
(5, 'USER_DEVELOPER', 'User Developer', 'USER');

-- Default admin user (password: admin123 — bcrypt hashed)
INSERT INTO sys_user (id, username, email, password_hash, user_type, owner_type, status)
VALUES (1, 'admin', 'admin@nexora.io',
        '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
        'PLATFORM_ADMIN', 'PLATFORM', 'ACTIVE');

-- Assign super admin role
INSERT INTO sys_user_role (user_id, role_id) VALUES (1, 1);

-- Basic permissions
INSERT INTO sys_permission (id, perm_code, perm_name, perm_type, path) VALUES
(1, 'dashboard', 'Dashboard', 'MENU', '/dashboard'),
(2, 'tenant_mgmt', 'Tenant Management', 'MENU', '/tenants'),
(3, 'provider_mgmt', 'Provider Management', 'MENU', '/providers'),
(4, 'endpoint_mgmt', 'Endpoint Management', 'MENU', '/endpoints'),
(5, 'route_mgmt', 'Model Route Management', 'MENU', '/routes'),
(6, 'apikey_mgmt', 'API Key Management', 'MENU', '/api-keys'),
(7, 'chat', 'Chat', 'MENU', '/chat'),
(8, 'playground', 'Playground', 'MENU', '/playground'),
(9, 'logs', 'Call Logs', 'MENU', '/logs'),
(10, 'billing', 'Billing', 'MENU', '/billing');

-- Assign all permissions to super admin role
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 1, id FROM sys_permission;

-- Demo provider and endpoint (OpenAI-compatible, uses placeholder API key)
INSERT INTO ai_provider_config (id, provider_name, provider_type, owner_type, enabled)
VALUES (1, 'OpenAI', 'OPENAI', 'PLATFORM', TRUE);

INSERT INTO ai_provider_endpoint (id, provider_id, endpoint_name, base_url,
    supported_protocols, default_protocol, auth_type, api_key_encrypted, owner_type, enabled)
VALUES (1, 1, 'openai-main', 'https://api.openai.com/v1',
    '["OPENAI"]', 'OPENAI', 'API_KEY', '<ENCRYPTED_OPENAI_KEY>', 'PLATFORM', TRUE);
