# Nexora Gateway Architecture

## 1. System Overview

Nexora Gateway is a multi-tenant operational AI API Gateway platform. It allows platform owners and tenant operators to monetize AI API services.

The backend consists of exactly **two runnable services** and **one shared library module**:

```
                    ┌──────────────────────────────┐
                    │        External Clients       │
                    │  (OpenAI SDK / Anthropic SDK  │
                    │   Claude Code / Cursor / etc.)│
                    └──────────────┬───────────────┘
                                   │ HTTPS
                                   ▼
                    ┌──────────────────────────────┐
                    │     ai-relay-gateway          │
                    │   (Spring WebFlux + Netty)    │
                    │                              │
                    │  • API Key Auth (Redis)       │
                    │  • Route Resolution (Redis)   │
                    │  • Protocol Adaptation         │
                    │  • SSE Streaming Proxy         │
                    │  • Redis Pub/Sub Publish       │
                    └──────┬──────────┬────────────┘
                           │          │
                    Redis  │    Redis Pub/Sub
                    Cache  │          │
                           │          ▼
                    ┌──────┴───────────────────────┐
                    │     ai-platform-service       │
                    │   (Spring MVC + MyBatis)      │
                    │                              │
                    │  • User / Tenant / Role CRUD  │
                    │  • API Key CRUD               │
                    │  • Provider / Endpoint / Route│
                    │  • Endpoint Testing            │
                    │  • Redis Pub/Sub Subscribe     │
                    │  • Event Persistence → PG      │
                    │  • Config Publish → Redis      │
                    └──────────────┬───────────────┘
                                   │
                                   ▼
                    ┌──────────────────────────────┐
                    │         PostgreSQL            │
                    │   (15 tables, Flyway)         │
                    └──────────────────────────────┘

          ┌─────────────────────────────────────┐
          │            ai-common                │
          │  (Shared Library — NOT a service)   │
          │                                     │
          │  • Enums (ProtocolType, etc.)       │
          │  • Internal Models (Request/Resp)   │
          │  • Events (GatewayEvent subtypes)   │
          │  • UrlBuilder                       │
          │  • Utility classes                  │
          └─────────────────────────────────────┘
```

**Key architectural principles:**

1. **ai-relay-gateway** is the hot-path relay. It handles API key authentication, model route resolution, protocol adaptation, upstream proxying, and SSE streaming. It publishes events to Redis Pub/Sub asynchronously. It must never query PostgreSQL, use MyBatis, or perform synchronous logging on the hot path. All hot-path data comes from Redis snapshots and in-memory caches.

2. **ai-platform-service** is the business service. It handles all CRUD operations, user/tenant management, endpoint testing, Redis Pub/Sub event consumption, and PostgreSQL persistence. It publishes configuration snapshots to Redis for the relay gateway to consume.

3. **ai-common** is a shared library module (JAR only). It contains enums, internal protocol models, event classes, the UrlBuilder, and common utilities. It has no Spring Boot starter, no controllers, no database config, and no independent deployment. Both services depend on it as a library.

**Data flow summary:**

```
Client Request
  → relay-gateway (API Key auth via Redis snapshot)
  → ProtocolAdapter (OpenAI/Anthropic → InternalChatRequest)
  → RouteResolutionService (model alias → ResolvedModelRoute from Redis)
  → UrlBuilder (baseUrl + pathPrefix + protocol + operation → final URL)
  → UpstreamProxyService (WebClient → upstream provider)
  → ProtocolAdapter (InternalChatResponse → client protocol response)
  → EventPublisher (async Redis Pub/Sub, fire-and-forget)
  → platform-service EventConsumerService (Redis subscription)
  → EventPersistenceService (PostgreSQL INSERT)
```

---

## 2. Module Boundaries

### 2.1 ai-common — Shared Library

**Package:** `com.nexora.common`

**Role:** A shared dependency JAR used by both `ai-relay-gateway` and `ai-platform-service`. It is not a deployable service.

**Contains:**

| Category | Key Types |
|---|---|
| **Enums** | `ProtocolType` (OPENAI, ANTHROPIC, RESPONSES, EMBEDDINGS, GEMINI), `ProviderType`, `UpstreamOperation` (MODELS, CHAT_COMPLETIONS, MESSAGES, RESPONSES, EMBEDDINGS) |
| **Internal Models** | `InternalChatRequest`, `InternalChatMessage`, `InternalChatResponse`, `InternalChatChunk`, `InternalUsage`, `InternalToolDefinition`, `InternalToolCall`, `InternalToolResult`, `InternalModelInfo`, `InternalError` |
| **Snapshot Models** | `ProviderEndpointSnapshot`, `ResolvedModelRoute` |
| **Events** | `GatewayEvent` (abstract base), `RequestStartedEvent`, `RequestCompletedEvent`, `RequestFailedEvent`, `UsageReportedEvent`, `FallbackTriggeredEvent`, `RateLimitRejectedEvent` |
| **Utilities** | `UrlBuilder`, serialization configs, common constants |

**Strictly forbidden in ai-common:**
- Spring Boot application class / `@SpringBootApplication`
- `@RestController` / `@Controller` classes
- Web routes or request mappings
- Database connection configuration
- Redis connection configuration
- MyBatis mappers or Spring Data repositories
- Business service classes with `@Service`
- Dockerfile or independent deployment configuration
- Scheduled tasks (`@Scheduled`)
- Any independently startable code

### 2.2 ai-relay-gateway — Hot-Path Relay

**Package:** `com.nexora.gateway`

**Runtime:** Spring WebFlux + Reactor Netty

**Responsibilities:**

| Component | Class | Description |
|---|---|---|
| **Controllers** | `OpenAIController` | `GET /v1/models`, `POST /v1/chat/completions` (stream + non-stream) |
| | `AnthropicController` | `POST /v1/messages` (stream + non-stream, accepts `x-api-key` or `Authorization` header) |
| **Auth** | `ApiKeyAuthService` | SHA-256 hash lookup against in-memory/Redis cache. Returns `AuthResult(userId, tenantId, status, allowedModels)`. No database calls. |
| **Routing** | `RouteResolutionService` | Resolves model alias to `ResolvedModelRoute` using `ConcurrentHashMap` cache (fed by Redis). Filters by client protocol support, enabled status, and endpoint health (excludes UNHEALTHY). Sorts by priority. |
| **Protocol Adapters** | `OpenAIProtocolAdapter` | OpenAI API JSON ↔ `InternalChatRequest` / `InternalChatResponse` |
| | `AnthropicProtocolAdapter` | Anthropic Messages API JSON ↔ `InternalChatRequest` / `InternalChatResponse` |
| **Proxy** | `UpstreamProxyService` | Uses `WebClient` (Reactor Netty) to call upstream providers. Builds protocol-appropriate upstream request bodies. Supports non-streaming (Mono) and streaming (Flux SSE). |
| **Events** | `EventPublisher` | Publishes `GatewayEvent` subtypes to Redis Pub/Sub channels asynchronously. Fire-and-forget; failure does not block the response. |

**Channel routing:**
- `RequestStartedEvent`, `FallbackTriggeredEvent`, `RateLimitRejectedEvent` → `ai-gateway:event:request`
- `UsageReportedEvent` → `ai-gateway:event:usage`
- `RequestCompletedEvent`, `RequestFailedEvent` → `ai-gateway:event:billing`

**Strictly forbidden in relay-gateway:**
- PostgreSQL queries on the hot path
- MyBatis / JDBC on the hot path
- Synchronous log/billing writes during requests
- Complex billing calculations
- Report generation or aggregation
- Background management APIs
- Any blocking I/O that would stall the event loop

### 2.3 ai-platform-service — Business Service

**Package:** `com.nexora.platform`

**Runtime:** Spring MVC + MyBatis + PostgreSQL

**Responsibilities:**

| Category | Controllers / Services | Description |
|---|---|---|
| **Auth** | `AuthController`, `AuthService` | Login, logout, current user info, menus, permissions |
| **Tenants** | `TenantController` | CRUD, enable/disable, status management |
| **API Keys** | `ApiKeyController`, `ApiKeyService` | Create (returns full key once), list (prefix only), disable. SHA-256 hash storage. |
| **Providers** | `ProviderController` | CRUD for `AiProviderConfig`, owner-scoped queries |
| **Endpoints** | `EndpointController` | CRUD for `AiProviderEndpoint`, protocol configuration, auth setup |
| **Routes** | `ModelRouteController` | CRUD for `AiModelRoute`, upstream protocol, fallback config |
| **Endpoint Testing** | `EndpointTestController` | `test-connectivity`, `test-models`, `test-chat`, `test-stream`, `save-as-route`. Backend injects encrypted keys; never exposes them to frontend. |
| **Event Consumer** | `EventConsumerService` | Subscribes to 3 Redis channels via `RedisMessageListenerContainer`. Dispatches to `EventPersistenceService`. |
| **Persistence** | `EventPersistenceService` | `@Transactional` inserts into `ai_request_log`, `ai_billing_record`, `ai_event_consume_log`. Uses `ON CONFLICT (event_id) DO NOTHING` for billing idempotency. |
| **Config Publish** | Config publish endpoint | Writes route/endpoint/key snapshots to Redis for relay-gateway consumption |
| **Migrations** | Flyway `V1__init_schema.sql` | 15 tables, seed data, indexes |

---

## 3. Request Lifecycle

### Full Request Path

```
1. CLIENT
   OpenAI SDK:  POST /v1/chat/completions  with Bearer sk-xxx
   Anthropic SDK / Claude Code: POST /v1/messages  with x-api-key or Authorization

2. RELAY CONTROLLER (OpenAIController / AnthropicController)
   ├── Extract API key from header
   ├── Generate requestId = "req_" + UUID (12 chars)
   └── Record start timestamp

3. API KEY AUTHENTICATION (ApiKeyAuthService)
   ├── SHA-256 hash the raw key
   ├── Lookup in ConcurrentHashMap<String, AuthResult> cache
   │   (Cache populated from Redis snapshots by config publish)
   ├── Check key status == "ACTIVE"
   └── Return AuthResult(userId, tenantId, keyPrefix, status, allowedModels)

4. PROTOCOL ADAPTATION — REQUEST (OpenAIProtocolAdapter / AnthropicProtocolAdapter)
   ├── Parse client-specific JSON body
   ├── Normalize to InternalChatRequest (model-agnostic format)
   ├── Convert messages, tools/tool_use, system prompts
   └── Set clientProtocol field

5. ROUTE RESOLUTION (RouteResolutionService)
   ├── Lookup model alias in route cache (Map<String, List<ResolvedModelRoute>>)
   ├── Filter: enabled == true
   ├── Filter: clientProtocolSupport contains the client's protocol
   ├── Filter: endpoint.enabled == true
   ├── Filter: endpoint.healthStatus != "UNHEALTHY"
   ├── Sort by priority (highest first)
   └── Return ResolvedModelRoute (with embedded ProviderEndpointSnapshot)

6. URL CONSTRUCTION (UrlBuilder)
   ├── Input: baseUrl, pathPrefix, upstreamProtocol, upstreamOperation
   ├── Strip trailing slashes from baseUrl
   ├── Normalize pathPrefix (strip leading/trailing slashes)
   ├── Append protocol version ("v1" for OPENAI/ANTHROPIC, "v1beta" for GEMINI)
   ├── Avoid /v1/v1 duplication (check if baseUrl already ends with /v1)
   ├── Append operation path (chat/completions, messages, models, etc.)
   └── Output: final upstream URL

7. UPSTREAM REQUEST (UpstreamProxyService)
   ├── Build upstream-format request body based on upstreamProtocol
   │   ├── OpenAI upstream: {model, messages, stream, temperature, max_tokens}
   │   └── Anthropic upstream: {model, max_tokens, messages, stream, system}
   ├── Set Authorization header from endpoint's encrypted API key
   ├── Apply extraHeaders from endpoint config
   ├── Non-stream: WebClient.post() → Mono<InternalChatResponse>
   └── Stream: WebClient.post() → Flux<String> (SSE text/event-stream)

8. PROTOCOL ADAPTATION — RESPONSE
   ├── OpenAI response: {id, object, created, model, choices[{message, finish_reason}], usage}
   ├── Anthropic response: {id, type, role, model, content[{type:"text", text}, ...], stop_reason, usage}
   └── Tool calls: tool_calls (OpenAI) ↔ tool_use (Anthropic)

9. EVENT PUBLISHING (EventPublisher) — FIRE-AND-FORGET
   ├── RequestStartedEvent published before upstream call
   ├── RequestCompletedEvent published on success (non-stream: after response; stream: doOnComplete)
   ├── RequestFailedEvent published on error (doOnError)
   ├── Serialize to JSON, resolve channel
   ├── redisTemplate.convertAndSend(channel, payload).subscribe() — async, non-blocking
   └── Publish failure logged but never propagated to client

10. CLIENT RESPONSE
    ├── Non-stream: JSON response body
    └── Stream: ResponseEntity with Content-Type: text/event-stream, Flux<String> body

11. EVENT CONSUMPTION (platform-service, async)
    ├── EventConsumerService subscribes to 3 Redis channels at @PostConstruct
    ├── RedisMessageListenerContainer dispatches to EventListener.onMessage()
    ├── Deserialize JSON to GatewayEvent (polymorphic)
    └── EventPersistenceService.persist():
        ├── INSERT ai_event_consume_log (event_id, event_type, channel, consume_status)
        ├── RequestCompletedEvent → INSERT ai_request_log + INSERT ai_billing_record
        ├── RequestFailedEvent → INSERT ai_request_log (success=false)
        └── UsageReportedEvent → INSERT ai_billing_record (ON CONFLICT DO NOTHING)
```

### Cross-Protocol Routing Example

```
Client: OpenAI SDK → POST /v1/chat/completions {model: "claude-sonnet", messages: [...]}

1. OpenAIController receives OpenAI-format request
2. OpenAIProtocolAdapter.toInternal() → InternalChatRequest (clientProtocol=OPENAI)
3. RouteResolutionService.resolve("claude-sonnet", "OPENAI")
   → Returns ResolvedModelRoute with upstreamProtocol=ANTHROPIC, endpoint=openrouter-main
4. UrlBuilder.build(baseUrl, prefix, ANTHROPIC, MESSAGES) → https://openrouter.ai/api/v1/messages
5. UpstreamProxyService converts InternalChatRequest to Anthropic-format body
6. Server sends Anthropic-format request to upstream
7. Receives Anthropic-format response → InternalChatResponse
8. OpenAIProtocolAdapter.toOpenAIResponse() → OpenAI-format response to client
```

---

## 4. Provider / Endpoint / Protocol Design

### 4.1 Decoupling Principle

Three entities are intentionally decoupled:

- **Provider** is a vendor identity (e.g., OpenAI, Anthropic, DeepSeek, OpenRouter). It does **not** dictate protocol.
- **Endpoint** is a connection point with a `baseUrl`, authentication, and a list of `supportedProtocols`. It does **not** dictate which protocol is used for a specific request.
- **Protocol** for upstream communication is determined by `ModelRoute.upstreamProtocol`, which is configured per route. This allows a single endpoint to serve requests using different protocols depending on the route.

### 4.2 Key Fields

**AiProviderEndpoint:**

| Field | Type | Description |
|---|---|---|
| `baseUrl` | VARCHAR(512) | e.g. `https://api.openai.com/v1`, `https://api.anthropic.com` |
| `supportedProtocols` | JSONB | Array of protocols this endpoint physically supports, e.g. `["OPENAI"]`, `["OPENAI","ANTHROPIC"]` |
| `defaultProtocol` | VARCHAR(32) | Default protocol when route does not specify |
| `pathPrefix` | VARCHAR(128) | Optional path prefix, e.g. `anthropic` for `https://api.deepseek.com/anthropic` |
| `apiKeyEncrypted` | TEXT | Encrypted upstream API key |
| `authType` | VARCHAR(32) | `API_KEY`, `BEARER`, etc. |
| `extraHeaders` | JSONB | Additional headers to inject into upstream requests |

**AiModelRoute:**

| Field | Type | Description |
|---|---|---|
| `modelAlias` | VARCHAR(128) | Client-facing model name, e.g. `gpt-4o`, `claude-sonnet` |
| `upstreamModel` | VARCHAR(128) | Actual model name to send upstream, e.g. `anthropic/claude-sonnet-4-20250514` |
| `upstreamProtocol` | VARCHAR(32) | Protocol to use for upstream request: `OPENAI`, `ANTHROPIC`, etc. |
| `clientProtocolSupport` | JSONB | Which client protocols can access this route, e.g. `["OPENAI","ANTHROPIC"]` |
| `fallbackGroup` | VARCHAR(64) | Group name for fallback routing |
| `priority` | INT | Higher = preferred |
| `weight` | INT | For weighted load distribution |

### 4.3 Three Endpoint Patterns

**Pattern A: One Provider, Multiple Endpoints, Different Protocols**

Some vendors expose separate endpoints for different protocol formats.

```
Provider: deepseek

Endpoint A (deepseek-openai):
  baseUrl = https://api.deepseek.com
  supportedProtocols = ["OPENAI"]
  defaultProtocol = OPENAI

Endpoint B (deepseek-anthropic):
  baseUrl = https://api.deepseek.com/anthropic
  supportedProtocols = ["ANTHROPIC"]
  defaultProtocol = ANTHROPIC
```

**Pattern B: One Provider, One Endpoint, Multiple Protocols**

Aggregator gateways (OpenRouter, LiteLLM, One API) often support multiple protocols on a single base URL.

```
Provider: openrouter

Endpoint (openrouter-main):
  baseUrl = https://openrouter.ai/api
  supportedProtocols = ["OPENAI", "ANTHROPIC"]
  defaultProtocol = OPENAI
```

**Pattern C: One Provider, One Endpoint, Single Protocol**

Official first-party APIs typically support only their native protocol.

```
Provider: openai
Endpoint (openai-main):
  baseUrl = https://api.openai.com/v1
  supportedProtocols = ["OPENAI"]

Provider: anthropic
Endpoint (anthropic-main):
  baseUrl = https://api.anthropic.com
  supportedProtocols = ["ANTHROPIC"]
```

### 4.4 Cross-Protocol Routing

The system supports all cross-protocol combinations:

| Client Protocol | Upstream Protocol | Example Use Case |
|---|---|---|
| OpenAI | OpenAI | Standard OpenAI → OpenAI pass-through |
| OpenAI | Anthropic | Use OpenRouter to access Claude models via OpenAI SDK |
| Anthropic | Anthropic | Standard Anthropic → Anthropic pass-through, Claude Code |
| Anthropic | OpenAI | Anthropic client calling GPT-4o via an aggregator gateway |

The `ProtocolAdapter` layer handles all conversion. The separation of `clientProtocolSupport` (what the client can use) from `upstreamProtocol` (what the route sends upstream) enables this flexibility.

### 4.5 Anti-Patterns Explicitly Avoided

- Provider type does NOT imply protocol. An `OPENAI`-type provider can have endpoints that support Anthropic protocol.
- Endpoints are NOT limited to a single protocol. `supportedProtocols` is an array.
- Client protocol does NOT dictate upstream protocol. The route decides.
- Protocol choice does NOT live in the endpoint. It lives in `ModelRoute.upstreamProtocol`.

---

## 5. Event Flow

### 5.1 Redis Pub/Sub Channels

| Channel | Events | Purpose |
|---|---|---|
| `ai-gateway:event:request` | `RequestStartedEvent`, `FallbackTriggeredEvent`, `RateLimitRejectedEvent` | Operational visibility: request lifecycle, fallback decisions, rate limit actions |
| `ai-gateway:event:usage` | `UsageReportedEvent` | Token usage reporting for metering and analytics |
| `ai-gateway:event:billing` | `RequestCompletedEvent`, `RequestFailedEvent` | Billing records and request log persistence |

### 5.2 Event Types

All events extend the abstract `GatewayEvent` base class:

```java
public abstract class GatewayEvent {
    String eventId;      // UUID, unique per event instance
    String eventType;    // Discriminator for JSON polymorphic deserialization
    String requestId;    // Correlates events from the same client request
    String tenantId;     // Tenant context
    String userId;       // Authenticated user
    String apiKeyId;     // API key hash used for the request
    Instant occurredAt;  // Event timestamp
}
```

**Concrete event subtypes:**

| Event | Additional Fields |
|---|---|
| `RequestStartedEvent` | `modelAlias`, `clientProtocol`, `stream` |
| `RequestCompletedEvent` | `modelAlias`, `providerId`, `endpointId`, `upstreamModel`, `upstreamProtocol`, `inputTokens`, `outputTokens`, `totalTokens`, `upstreamCost`, `platformCharge`, `tenantCharge`, `latencyMs`, `firstTokenLatencyMs`, `stream`, `success`, `finishReason` |
| `RequestFailedEvent` | `modelAlias`, `providerId`, `endpointId`, `upstreamModel`, `errorCode`, `errorMessage`, `httpStatusCode`, `latencyMs`, `stream`, `fallbackAttempted`, `fallbackResult` |
| `UsageReportedEvent` | `modelAlias`, `providerId`, `endpointId`, `upstreamModel`, `inputTokens`, `outputTokens`, `totalTokens`, `upstreamCost`, `platformCharge`, `tenantCharge`, `latencyMs` |
| `FallbackTriggeredEvent` | `modelAlias`, `originalProviderId`, `originalEndpointId`, `fallbackProviderId`, `fallbackEndpointId`, `fallbackReason`, `originalErrorCode` |
| `RateLimitRejectedEvent` | `modelAlias`, `limitType`, `limitKey`, `currentCount`, `limitValue` |

### 5.3 Publishing (relay-gateway)

The `EventPublisher` service in `ai-relay-gateway`:

1. Resolves the target Redis channel based on `event.getEventType()`
2. Serializes the event to JSON using Jackson (polymorphic via `@JsonTypeInfo` / `@JsonSubTypes`)
3. Calls `ReactiveRedisTemplate.convertAndSend(channel, payload).subscribe()` -- fire-and-forget
4. Serialization failures are logged and silently dropped (event is lost but relay continues)
5. Redis publish failures are logged via the error callback on `.subscribe()` but never propagate to the client

This design ensures that Redis unavailability does not cause relay outages.

### 5.4 Consumption (platform-service)

The `EventConsumerService` in `ai-platform-service`:

1. On `@PostConstruct`, subscribes to all 3 channels via `RedisMessageListenerContainer`
2. Each incoming message is deserialized to `GatewayEvent` (Jackson polymorphic)
3. Dispatched to `EventPersistenceService.persist(channel, event)`

The `EventPersistenceService`:

1. Inserts a record into `ai_event_consume_log` for every event (audit trail)
2. Routes by event type:
   - `RequestCompletedEvent` → `INSERT ai_request_log` + `INSERT ai_billing_record`
   - `RequestFailedEvent` → `INSERT ai_request_log` (with `success=false`)
   - `UsageReportedEvent` → `INSERT ai_billing_record`

### 5.5 Idempotency

Billing events (`RequestCompletedEvent`, `UsageReportedEvent`) use the `eventId` as a unique key:

```sql
INSERT INTO ai_billing_record (event_id, ...)
VALUES (?, ...)
ON CONFLICT (event_id) DO NOTHING
```

The `event_id` column has a UNIQUE constraint. The `requestId` is also indexed for correlation but is not the idempotency key (multiple billing events may be generated per request).

---

## 6. Database Design

### 6.1 Overview

PostgreSQL with Flyway migrations (located in `ai-platform-service/src/main/resources/db/migration/`). The initial migration `V1__init_schema.sql` creates 15 tables organized into 4 groups.

All tables follow these conventions:
- Primary key: `id BIGSERIAL`
- All tables have `create_time` and `update_time` (where applicable)
- Tenant-scoped tables have `owner_tenant_id`
- User-owned resources have `owner_user_id`
- Monetary values use `NUMERIC(12,8)`
- JSON configuration uses `JSONB`
- Common query columns are indexed

### 6.2 Table Groups

**Group 1: User, Tenant, and Permission (6 tables)**

| Table | Key Columns | Notes |
|---|---|---|
| `tenant` | `tenant_name`, `tenant_code` (UNIQUE), `status`, `max_users`, `max_providers` | Tenant operators; platform default tenant has `tenant_code='PLATFORM'` |
| `sys_user` | `username`, `email`, `password_hash` (bcrypt), `user_type`, `owner_type`, `owner_tenant_id`, `status` | Unified user model. `user_type`: `PLATFORM_ADMIN`, `PLATFORM_CUSTOMER`, `TENANT_MEMBER`, `TENANT_CUSTOMER` |
| `sys_role` | `role_code` (UNIQUE), `role_name`, `role_type`, `owner_tenant_id` | `PLATFORM_SUPER_ADMIN`, `PLATFORM_ADMIN`, `TENANT_OWNER`, `TENANT_ADMIN`, `USER_DEVELOPER` |
| `sys_permission` | `perm_code` (UNIQUE), `perm_name`, `perm_type`, `parent_id`, `path` | Menu and action permissions |
| `sys_user_role` | `user_id`, `role_id` (unique composite) | Many-to-many user-role mapping |
| `sys_role_permission` | `role_id`, `permission_id` (unique composite) | Many-to-many role-permission mapping |

**Group 2: API Key (1 table)**

| Table | Key Columns | Notes |
|---|---|---|
| `ai_api_key` | `key_hash` (UNIQUE, SHA-256), `key_prefix` (first 8 chars), `key_name`, `owner_user_id`, `owner_type`, `owner_tenant_id`, `status`, `allowed_models` (JSONB), `rate_limit_rpm`, `rate_limit_tpm`, `max_quota_tokens`, `used_quota_tokens`, `expire_time`, `last_used_time` | Raw API key never stored. Full key returned only at creation. |

**Group 3: Provider, Endpoint, Route, Price, Health (5 tables)**

| Table | Key Columns | Notes |
|---|---|---|
| `ai_provider_config` | `provider_name`, `provider_type`, `display_name`, `owner_type`, `owner_tenant_id`, `enabled` | Vendor identity. Platform or tenant-scoped. |
| `ai_provider_endpoint` | `provider_id` (FK), `endpoint_name`, `base_url`, `path_prefix`, `supported_protocols` (JSONB), `default_protocol`, `auth_type`, `api_key_encrypted`, `extra_headers` (JSONB), `timeout_ms`, `connect_timeout_ms`, `read_timeout_ms`, `max_retries`, `priority`, `weight`, `owner_type`, `owner_tenant_id`, `enabled` | Connection points with supported protocols array. API key encrypted at rest. |
| `ai_model_route` | `model_alias`, `provider_id`, `endpoint_id`, `upstream_model`, `upstream_protocol`, `client_protocol_support` (JSONB), `priority`, `weight`, `fallback_group`, `support_stream`, `support_tools`, `support_vision`, `support_reasoning`, `max_input_tokens`, `max_output_tokens`, `price_config_id`, `owner_type`, `owner_tenant_id`, `enabled` | Routes model aliases to upstream endpoints with specific protocols. |
| `ai_model_price` | `model_alias`, `input_price_per_1k` (NUMERIC), `output_price_per_1k` (NUMERIC), `cache_read_price_per_1k`, `currency`, `owner_type`, `owner_tenant_id` | Pricing configuration for double-tier billing. |
| `ai_endpoint_health_state` | `endpoint_id` (UNIQUE), `health_status`, `consecutive_failures`, `total_requests`, `total_failures`, `last_check_time`, `last_success_time`, `last_failure_time`, `cooldown_until` | Health status for circuit-breaking. Statuses: `HEALTHY`, `DEGRADED`, `UNHEALTHY`, `RECOVERING`, `DISABLED`. |

**Group 4: Logs, Billing, Events (3 tables)**

| Table | Key Columns | Notes |
|---|---|---|
| `ai_request_log` | `request_id`, `tenant_id`, `user_id`, `api_key_id`, `model_alias`, `provider_id`, `endpoint_id`, `upstream_model`, `upstream_protocol`, `stream`, `input_tokens`, `output_tokens`, `total_tokens`, `latency_ms`, `first_token_latency_ms`, `success`, `error_code`, `error_message`, `finish_reason`, `created_at` | Every relay request logged. Indexed on `request_id`, `tenant_id`, `user_id`, `created_at`. |
| `ai_billing_record` | `event_id` (UNIQUE), `request_id`, `tenant_id`, `user_id`, `api_key_id`, `model_alias`, `provider_id`, `endpoint_id`, `upstream_model`, `input_tokens`, `output_tokens`, `total_tokens`, `upstream_cost`, `platform_charge`, `tenant_charge`, `platform_profit`, `tenant_profit`, `billing_status` | Idempotent via `ON CONFLICT (event_id) DO NOTHING`. Statuses: `PENDING`, `BILLED`, `SETTLED`. |
| `ai_event_consume_log` | `event_id`, `event_type`, `channel`, `consume_status`, `retry_count`, `payload` (JSONB), `error_message`, `consumed_at` | Audit trail for all consumed events. |

### 6.3 Seed Data

The migration seeds:
- A platform default tenant (`tenant_code=PLATFORM`)
- Five default roles: `PLATFORM_SUPER_ADMIN`, `PLATFORM_ADMIN`, `TENANT_OWNER`, `TENANT_ADMIN`, `USER_DEVELOPER`
- A default admin user (`admin` / `admin123` bcrypt-hashed)
- Super admin role assignment
- Ten basic permissions (dashboard, tenant_mgmt, provider_mgmt, endpoint_mgmt, route_mgmt, apikey_mgmt, chat, playground, logs, billing)
- A demo OpenAI provider and endpoint (with placeholder encrypted key)

---

## 7. Security

### 7.1 API Key Storage

API keys are never stored in plaintext:

```
User creates key → raw key generated (e.g., sk-a1b2c3d4e5f6...)
key_hash = SHA-256(raw_key)
key_prefix = raw_key.substring(0, 8) // for display only

Stored: key_hash (full SHA-256 hex), key_prefix (first 8 chars)
Returned to user: full raw key (ONLY at creation time)
```

The `ai_api_key` table has a UNIQUE constraint on `key_hash`. The `key_prefix` is indexed for list display and troubleshooting.

### 7.2 Relay Authentication

`ApiKeyAuthService` on the hot path:
1. Hashes the incoming raw key with SHA-256
2. Looks up the hash in an in-memory `ConcurrentHashMap` (populated from Redis snapshots)
3. Returns `AuthResult` containing `userId`, `tenantId`, `status`, `allowedModels`
4. Rejects if status is not `ACTIVE`
5. No database query is performed

### 7.3 Provider API Key Encryption

Upstream provider API keys are stored encrypted in the `ai_provider_endpoint.api_key_encrypted` column. The encryption mechanism is abstracted behind the endpoint configuration. Keys are decrypted only when building upstream requests in `UpstreamProxyService`. The frontend never receives upstream API keys -- the `EndpointTestController` reads and injects them server-side and returns only sanitized headers.

### 7.4 Tenant Data Isolation

All multi-tenant queries enforce tenant isolation:
- Platform-scoped resources: `owner_type = 'PLATFORM'`
- Tenant-scoped resources: `owner_type = 'TENANT' AND owner_tenant_id = ?` (from authenticated context)
- The API layer extracts tenant context from the authenticated user session and applies it to all queries
- Cross-tenant access is prevented at the query level, not just the UI level

### 7.5 User Resource Isolation

User-owned resources enforce user isolation:
- Resources like API keys are scoped to `owner_user_id`
- A user can only view and manage their own API keys
- Platform and tenant admins have elevated access controlled by role permissions

### 7.6 Additional Security Measures

- Passwords are bcrypt-hashed in `sys_user.password_hash`
- Authentication tokens are managed server-side; the frontend never receives raw upstream keys
- Sensitive headers are sanitized before being returned in endpoint test responses
- Configuration changes are logged via operation log events
- Rate limiting and quota enforcement are applied per API key at the relay gateway level
