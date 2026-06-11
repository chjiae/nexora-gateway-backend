# Nexora Gateway API Reference

> **Last Updated:** 2026-06-11
> **Project:** Nexora Gateway — Multi-tenant AI API Gateway Platform

---

## Table of Contents

1. [Relay Gateway Endpoints](#1-relay-gateway-endpoints)
   - [OpenAI Compatible API](#11-openai-compatible-api)
   - [Anthropic Compatible API](#12-anthropic-compatible-api)
2. [Platform Service Auth Endpoints](#2-platform-service-auth-endpoints)
3. [Platform Service API Key Endpoints](#3-platform-service-api-key-endpoints)
4. [Platform Service Provider Endpoints](#4-platform-service-provider-endpoints)
5. [Platform Service Endpoint Management](#5-platform-service-endpoint-management)
6. [Platform Service Model Route Endpoints](#6-platform-service-model-route-endpoints)
7. [Platform Service Tenant Endpoints](#7-platform-service-tenant-endpoints)
8. [Platform Service Endpoint Testing](#8-platform-service-endpoint-testing)
9. [Common Response Format and Error Codes](#9-common-response-format-and-error-codes)
10. [Authentication and Authorization](#10-authentication-and-authorization)

---

## 1. Relay Gateway Endpoints

The Relay Gateway (`ai-relay-gateway`) exposes AI-model-compatible endpoints for end users and SDKs. It is the hot path: authenticate, route, proxy upstream, and stream responses back.

**Base URL:** `http://<host>:<relay-port>`

All relay endpoints require an **API Key** for authentication. See [Authentication and Authorization](#10-authentication-and-authorization).

### 1.1 OpenAI Compatible API

Endpoints follow the [OpenAI API v1 schema](https://platform.openai.com/docs/api-reference) so standard OpenAI SDKs and tools can connect transparently.

#### `GET /v1/models`

List available models.

**Auth:** `Authorization: Bearer <api-key>`

**Response `200 OK`:**

```json
{
  "object": "list",
  "data": [
    {"id": "gpt-4o", "object": "model", "created": 1700000000, "owned_by": "nexora"},
    {"id": "gpt-4o-mini", "object": "model", "created": 1700000000, "owned_by": "nexora"},
    {"id": "claude-sonnet-4-20250514", "object": "model", "created": 1700000000, "owned_by": "nexora"}
  ]
}
```

**Error Response `500 Internal Server Error`:**

```json
{
  "error": {
    "message": "<error description>",
    "type": "server_error"
  }
}
```

---

#### `POST /v1/chat/completions`

Chat completions endpoint. Supports both **non-streaming** (single JSON response) and **streaming** (SSE, `text/event-stream`).

**Auth:** `Authorization: Bearer <api-key>`
**Content-Type:** `application/json`

**Request Body:**

Standard OpenAI Chat Completions request body. Key fields:

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `model` | string | Yes | Model alias (e.g. `gpt-4o`, `claude-sonnet-4-20250514`) |
| `messages` | array | Yes | Conversation messages |
| `stream` | boolean | No | Enable SSE streaming (default: `false`) |
| `temperature` | number | No | Sampling temperature |
| `max_tokens` | integer | No | Max tokens to generate |
| `tools` | array | No | Tool definitions for function calling |

**Example Request:**

```json
{
  "model": "gpt-4o",
  "messages": [
    {"role": "user", "content": "Hello!"}
  ],
  "stream": false
}
```

**Non-Streaming Response `200 OK`:**

Standard OpenAI Chat Completion response object.

```json
{
  "id": "chatcmpl-xxx",
  "object": "chat.completion",
  "created": 1718000000,
  "model": "gpt-4o",
  "choices": [
    {
      "index": 0,
      "message": {
        "role": "assistant",
        "content": "Hello! How can I help you today?"
      },
      "finish_reason": "stop"
    }
  ],
  "usage": {
    "prompt_tokens": 10,
    "completion_tokens": 9,
    "total_tokens": 19
  }
}
```

**Streaming Response `200 OK`:**

Content-Type: `text/event-stream`. Standard OpenAI SSE chunks ending with `data: [DONE]`.

**Error Response `500 Internal Server Error`:**

```json
{
  "error": {
    "message": "<error description>",
    "type": "server_error"
  }
}
```

---

### 1.2 Anthropic Compatible API

Endpoints follow the [Anthropic Messages API schema](https://docs.anthropic.com/en/api/messages) so Claude Code and other Anthropic-compatible tools can connect.

#### `POST /v1/messages`

Messages API endpoint. Supports both **non-streaming** and **streaming** (SSE).

**Auth (one of the following):**
- `x-api-key: <api-key>` (preferred, Anthropic SDK convention)
- `Authorization: Bearer <api-key>` (fallback)

**Headers:**

| Header | Required | Default | Description |
|--------|----------|---------|-------------|
| `x-api-key` | Yes* | — | API key (Anthropic convention) |
| `Authorization` | Yes* | — | Bearer token (fallback if x-api-key absent) |
| `anthropic-version` | No | `2023-06-01` | Anthropic API version string |
| `Content-Type` | Yes | — | `application/json` |

> *At least one of `x-api-key` or `Authorization` must be present. If both are provided, `x-api-key` takes precedence.

**Request Body:**

Standard Anthropic Messages request body. Key fields:

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `model` | string | Yes | Model alias |
| `messages` | array | Yes | Conversation messages |
| `max_tokens` | integer | Yes | Max tokens to generate |
| `stream` | boolean | No | Enable SSE streaming |
| `system` | string/array | No | System prompt |
| `tools` | array | No | Tool definitions |
| `temperature` | number | No | Sampling temperature |

**Example Request:**

```json
{
  "model": "claude-sonnet-4-20250514",
  "max_tokens": 1024,
  "messages": [
    {"role": "user", "content": "Hello!"}
  ]
}
```

**Non-Streaming Response `200 OK`:**

Standard Anthropic Messages response object.

```json
{
  "id": "msg_xxx",
  "type": "message",
  "role": "assistant",
  "model": "claude-sonnet-4-20250514",
  "content": [
    {"type": "text", "text": "Hello! How can I help?"}
  ],
  "stop_reason": "end_turn",
  "usage": {
    "input_tokens": 10,
    "output_tokens": 8
  }
}
```

**Streaming Response `200 OK`:**

Content-Type: `text/event-stream`. Standard Anthropic SSE event stream.

**Error Response `500 Internal Server Error`:**

```json
{
  "type": "error",
  "error": {
    "type": "server_error",
    "message": "<error description>"
  }
}
```

---

## 2. Platform Service Auth Endpoints

The Platform Service (`ai-platform-service`) hosts all management APIs. These endpoints use **session token** authentication (not API keys).

**Base URL:** `http://<host>:<platform-port>`
**Auth Header:** `Authorization: Bearer <session-token>` (obtained from `/api/auth/login`)

### `POST /api/auth/login`

Authenticate with username and password to obtain a session token.

**Auth:** None (public)

**Request Body:**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `username` | string | Yes | Login username |
| `password` | string | Yes | Login password |

**Request Example:**

```json
{
  "username": "admin",
  "password": "admin123"
}
```

**Response `200 OK`:**

```json
{
  "code": 200,
  "message": "OK",
  "data": {
    "token": "550e8400-e29b-41d4-a716-446655440000",
    "userId": 1,
    "username": "admin",
    "userType": "PLATFORM_ADMIN",
    "ownerType": "PLATFORM",
    "ownerTenantId": null,
    "roles": ["PLATFORM_SUPER_ADMIN", "USER_DEVELOPER"],
    "permissions": ["dashboard", "chat", "playground", "apikey", "providers", "endpoints", "routes", "logs", "billing"]
  }
}
```

**Error Responses:**

| Code | Message | Cause |
|------|---------|-------|
| 401 | Invalid username or password | Bad credentials |
| 403 | User account is not active | Account disabled |

---

### `GET /api/auth/me`

Get the currently authenticated user's profile.

**Auth:** `Authorization: Bearer <session-token>`

**Response `200 OK`:**

```json
{
  "code": 200,
  "message": "OK",
  "data": {
    "userId": 1,
    "username": "admin",
    "email": "admin@nexora.io",
    "phone": null,
    "userType": "PLATFORM_ADMIN",
    "ownerType": "PLATFORM",
    "ownerTenantId": null,
    "avatarUrl": null,
    "status": "ACTIVE",
    "roles": ["PLATFORM_SUPER_ADMIN", "USER_DEVELOPER"],
    "permissions": ["dashboard", "chat", "playground", "apikey", "providers", "endpoints", "routes", "logs", "billing"]
  }
}
```

---

### `GET /api/auth/menus`

Get the dynamic menu tree for the current user based on their roles and user type.

**Auth:** `Authorization: Bearer <session-token>`

**Response `200 OK`:**

```json
{
  "code": 200,
  "message": "OK",
  "data": {
    "menus": [
      {"code": "dashboard", "name": "Overview", "path": "/dashboard", "icon": "dashboard", "children": null},
      {"code": "tenant_mgmt", "name": "Tenants", "path": "/tenants", "icon": "building", "children": null},
      {"code": "provider_mgmt", "name": "Providers", "path": "/providers", "icon": "server", "children": null},
      {"code": "endpoint_mgmt", "name": "Endpoints", "path": "/endpoints", "icon": "plug", "children": null},
      {"code": "route_mgmt", "name": "Model Routes", "path": "/routes", "icon": "route", "children": null},
      {"code": "logs", "name": "Call Logs", "path": "/logs", "icon": "file-text", "children": null},
      {"code": "billing", "name": "Billing", "path": "/billing", "icon": "dollar", "children": null},
      {"code": "apikey_mgmt", "name": "API Keys", "path": "/api-keys", "icon": "key", "children": null},
      {"code": "chat", "name": "Chat", "path": "/chat", "icon": "message", "children": null},
      {"code": "playground", "name": "Playground", "path": "/playground", "icon": "code", "children": null}
    ]
  }
}
```

**Menu visibility by role:**

| Menu Code | Platform Admin | Tenant Admin |
|-----------|:---:|:---:|
| `dashboard` | Yes | Yes |
| `tenant_mgmt` | Yes | No |
| `provider_mgmt` / `tenant_providers` | Yes | Yes |
| `endpoint_mgmt` / `tenant_endpoints` | Yes | Yes |
| `route_mgmt` / `tenant_routes` | Yes | Yes |
| `logs` | Yes | No |
| `billing` | Yes | No |
| `apikey_mgmt` | Yes | Yes |
| `chat` | Yes | Yes |
| `playground` | Yes | Yes |

---

### `POST /api/auth/logout`

Invalidate the current session token.

**Auth:** `Authorization: Bearer <session-token>`

**Response `200 OK`:**

```json
{
  "code": 200,
  "message": "OK",
  "data": null
}
```

---

## 3. Platform Service API Key Endpoints

Manage API keys used to authenticate against the Relay Gateway.

**Base path:** `/api/api-keys`
**Auth:** `Authorization: Bearer <session-token>` (all endpoints)

### `POST /api/api-keys`

Create a new API key. **The raw key is returned only once in this response** and cannot be retrieved later.

**Request Body:**

| Field | Type | Required | Default | Description |
|-------|------|----------|---------|-------------|
| `keyName` | string | No | `"Default Key"` | Display name for the key |

**Request Example:**

```json
{
  "keyName": "Production Key"
}
```

**Response `200 OK`:**

```json
{
  "code": 200,
  "message": "OK",
  "data": {
    "id": 1,
    "rawKey": "sk-nex-abc123def456...",
    "prefix": "sk-nex-abc123",
    "name": "Production Key",
    "warning": "Store this key securely. It will not be shown again."
  }
}
```

**Important:** The `rawKey` field contains the full secret key. This value is never stored in plaintext; only a SHA-256 hash is persisted. Copy and store it immediately.

---

### `GET /api/api-keys`

List API keys for the current user (paginated).

**Query Parameters:**

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `page` | integer | No | `1` | Page number |

**Response `200 OK`:**

```json
{
  "code": 200,
  "message": "OK",
  "data": {
    "records": [
      {
        "id": 1,
        "keyHash": "<sha256-hash>",
        "keyPrefix": "sk-nex-abc123",
        "keyName": "Production Key",
        "ownerUserId": 1,
        "ownerType": "PLATFORM",
        "ownerTenantId": null,
        "status": "ACTIVE",
        "allowedModels": null,
        "rateLimitRpm": null,
        "rateLimitTpm": null,
        "maxQuotaTokens": null,
        "usedQuotaTokens": 0,
        "expireTime": null,
        "lastUsedTime": "2026-06-11T10:30:00",
        "createTime": "2026-06-01T08:00:00",
        "updateTime": "2026-06-01T08:00:00"
      }
    ],
    "total": 1
  }
}
```

> **Note:** `rawKey` is never included in list responses. The `keyPrefix` can be used to identify keys in logs and UIs.

---

### `PUT /api/api-keys/{id}/disable`

Disable an API key. Disabled keys cannot authenticate against the Relay Gateway.

**Path Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| `id` | integer | API Key ID |

**Response `200 OK`:**

```json
{
  "code": 200,
  "message": "OK",
  "data": null
}
```

**Error Response:**

```json
{
  "code": 404,
  "message": "API Key not found or not authorized",
  "data": null
}
```

---

## 4. Platform Service Provider Endpoints

Manage AI providers (e.g., OpenAI, Anthropic, Azure, custom providers).

**Base path:** `/api/providers`
**Auth:** `Authorization: Bearer <session-token>` (all endpoints)

### Provider Entity Fields

| Field | Type | Description |
|-------|------|-------------|
| `id` | integer | Auto-generated primary key |
| `providerName` | string | Internal name (e.g. `openai`, `anthropic`) |
| `providerType` | string | Provider type/category |
| `displayName` | string | Human-readable name |
| `ownerType` | string | `PLATFORM` or `TENANT` |
| `ownerTenantId` | integer | Tenant ID (null for platform-owned) |
| `enabled` | boolean | Whether the provider is active |
| `extraConfig` | string | JSON configuration blob |
| `remark` | string | Notes |
| `createTime` | datetime | Creation timestamp |
| `updateTime` | datetime | Last update timestamp |

---

### `GET /api/providers`

List all enabled providers.

**Response `200 OK`:**

```json
{
  "code": 200,
  "message": "OK",
  "data": [
    {
      "id": 1,
      "providerName": "openai",
      "providerType": "OPENAI",
      "displayName": "OpenAI",
      "ownerType": "PLATFORM",
      "ownerTenantId": null,
      "enabled": true,
      "extraConfig": null,
      "remark": null,
      "createTime": "2026-06-01T08:00:00",
      "updateTime": "2026-06-01T08:00:00"
    }
  ]
}
```

---

### `POST /api/providers`

Create a new provider.

**Request Body:** Full `AiProviderConfig` object (excluding `id`, `createTime`, `updateTime`).

**Request Example:**

```json
{
  "providerName": "anthropic",
  "providerType": "ANTHROPIC",
  "displayName": "Anthropic",
  "ownerType": "PLATFORM",
  "enabled": true
}
```

**Response `200 OK`:**

```json
{
  "code": 200,
  "message": "OK",
  "data": {
    "id": 2,
    "providerName": "anthropic",
    "providerType": "ANTHROPIC",
    "displayName": "Anthropic",
    "ownerType": "PLATFORM",
    "ownerTenantId": null,
    "enabled": true,
    "extraConfig": null,
    "remark": null,
    "createTime": "2026-06-11T12:00:00",
    "updateTime": "2026-06-11T12:00:00"
  }
}
```

---

### `GET /api/providers/{id}`

Get a single provider by ID.

**Response `200 OK`:** Single `AiProviderConfig` object in `data`.

---

### `PUT /api/providers/{id}`

Update an existing provider.

**Path Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| `id` | integer | Provider ID |

**Request Body:** Full `AiProviderConfig` object with updated fields.

**Response `200 OK`:** Updated provider entity in `data`.

---

### `DELETE /api/providers/{id}`

Soft-delete (disable) a provider. The provider is not physically deleted; `enabled` is set to `false`.

**Path Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| `id` | integer | Provider ID |

**Response `200 OK`:**

```json
{
  "code": 200,
  "message": "OK",
  "data": null
}
```

---

## 5. Platform Service Endpoint Management

Manage provider endpoints (connection details for each upstream service).

**Base path:** `/api/endpoints`
**Auth:** `Authorization: Bearer <session-token>` (all endpoints)

### Endpoint Entity Fields

| Field | Type | Description |
|-------|------|-------------|
| `id` | integer | Auto-generated primary key |
| `providerId` | integer | Foreign key to provider |
| `endpointName` | string | Human-readable name |
| `baseUrl` | string | Base URL (e.g. `https://api.openai.com`) |
| `pathPrefix` | string | Optional path prefix |
| `supportedProtocols` | string | Comma-separated protocol list (e.g. `OPENAI,ANTHROPIC`) |
| `defaultProtocol` | string | Default protocol for this endpoint |
| `authType` | string | `API_KEY`, `OAUTH`, etc. |
| `apiKeyEncrypted` | string | Encrypted API key |
| `extraHeaders` | string | JSON map of extra headers |
| `timeoutMs` | integer | Request timeout (ms) |
| `connectTimeoutMs` | integer | Connection timeout (ms) |
| `readTimeoutMs` | integer | Read timeout (ms) |
| `maxRetries` | integer | Max retry attempts |
| `priority` | integer | Routing priority (lower = higher priority) |
| `weight` | integer | Load balancing weight |
| `ownerType` | string | `PLATFORM` or `TENANT` |
| `ownerTenantId` | integer | Tenant ID (null for platform-owned) |
| `enabled` | boolean | Whether the endpoint is active |
| `createTime` | datetime | Creation timestamp |
| `updateTime` | datetime | Last update timestamp |

---

### `GET /api/endpoints`

List endpoints, optionally filtered by provider.

**Query Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `providerId` | integer | No | Filter by provider ID |

**Response `200 OK`:**

```json
{
  "code": 200,
  "message": "OK",
  "data": [
    {
      "id": 1,
      "providerId": 1,
      "endpointName": "OpenAI Default",
      "baseUrl": "https://api.openai.com",
      "pathPrefix": null,
      "supportedProtocols": "OPENAI",
      "defaultProtocol": "OPENAI",
      "authType": "API_KEY",
      "apiKeyEncrypted": "<encrypted>",
      "timeoutMs": 30000,
      "priority": 1,
      "weight": 100,
      "ownerType": "PLATFORM",
      "enabled": true,
      "createTime": "2026-06-01T08:00:00",
      "updateTime": "2026-06-01T08:00:00"
    }
  ]
}
```

---

### `POST /api/endpoints`

Create a new endpoint.

**Request Body:** Full `AiProviderEndpoint` object (excluding `id`, `createTime`, `updateTime`).

**Request Example:**

```json
{
  "providerId": 1,
  "endpointName": "OpenAI EU",
  "baseUrl": "https://api.openai.com",
  "supportedProtocols": "OPENAI",
  "defaultProtocol": "OPENAI",
  "authType": "API_KEY",
  "apiKeyEncrypted": "<encrypted-api-key>",
  "timeoutMs": 30000,
  "priority": 2,
  "weight": 50,
  "ownerType": "PLATFORM",
  "enabled": true
}
```

**Response `200 OK`:** Created endpoint entity in `data`.

---

### `GET /api/endpoints/{id}`

Get a single endpoint by ID.

**Response `200 OK`:** Single `AiProviderEndpoint` object in `data`.

---

### `PUT /api/endpoints/{id}`

Update an existing endpoint.

**Path Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| `id` | integer | Endpoint ID |

**Request Body:** Full `AiProviderEndpoint` object with updated fields.

**Response `200 OK`:** Updated endpoint entity in `data`.

---

### `DELETE /api/endpoints/{id}`

Soft-delete (disable) an endpoint.

**Path Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| `id` | integer | Endpoint ID |

**Response `200 OK`:**

```json
{
  "code": 200,
  "message": "OK",
  "data": null
}
```

---

## 6. Platform Service Model Route Endpoints

Manage model routing rules that map client-facing model aliases to upstream provider endpoints.

**Base path:** `/api/routes`
**Auth:** `Authorization: Bearer <session-token>` (all endpoints)

### Model Route Entity Fields

| Field | Type | Description |
|-------|------|-------------|
| `id` | integer | Auto-generated primary key |
| `modelAlias` | string | Client-facing model name (e.g. `gpt-4o`) |
| `providerId` | integer | Target provider ID |
| `endpointId` | integer | Target endpoint ID |
| `upstreamModel` | string | Actual model name sent upstream (e.g. `gpt-4o-2024-08-06`) |
| `upstreamProtocol` | string | Protocol to use upstream (`OPENAI`, `ANTHROPIC`) |
| `clientProtocolSupport` | string | Supported client protocols |
| `priority` | integer | Routing priority (lower = higher priority) |
| `weight` | integer | Load balancing weight |
| `fallbackGroup` | string | Fallback group name (same group = can fallback) |
| `supportStream` | boolean | Supports streaming |
| `supportTools` | boolean | Supports tool/function calling |
| `supportVision` | boolean | Supports vision/image input |
| `supportReasoning` | boolean | Supports reasoning/thinking |
| `maxInputTokens` | integer | Max input token limit |
| `maxOutputTokens` | integer | Max output token limit |
| `priceConfigId` | integer | Linked price configuration ID |
| `ownerType` | string | `PLATFORM` or `TENANT` |
| `ownerTenantId` | integer | Tenant ID (null for platform-owned) |
| `enabled` | boolean | Whether the route is active |
| `createTime` | datetime | Creation timestamp |
| `updateTime` | datetime | Last update timestamp |

---

### `GET /api/routes`

List all enabled model routes.

**Response `200 OK`:**

```json
{
  "code": 200,
  "message": "OK",
  "data": [
    {
      "id": 1,
      "modelAlias": "gpt-4o",
      "providerId": 1,
      "endpointId": 1,
      "upstreamModel": "gpt-4o",
      "upstreamProtocol": "OPENAI",
      "clientProtocolSupport": "OPENAI",
      "priority": 1,
      "weight": 100,
      "fallbackGroup": "default",
      "supportStream": true,
      "supportTools": true,
      "supportVision": true,
      "supportReasoning": false,
      "maxInputTokens": 128000,
      "maxOutputTokens": 16384,
      "priceConfigId": null,
      "ownerType": "PLATFORM",
      "ownerTenantId": null,
      "enabled": true,
      "createTime": "2026-06-01T08:00:00",
      "updateTime": "2026-06-01T08:00:00"
    }
  ]
}
```

---

### `POST /api/routes`

Create a new model route.

**Request Body:** Full `AiModelRoute` object (excluding `id`, `createTime`, `updateTime`).

**Request Example:**

```json
{
  "modelAlias": "claude-sonnet-4-20250514",
  "providerId": 2,
  "endpointId": 2,
  "upstreamModel": "claude-sonnet-4-20250514",
  "upstreamProtocol": "ANTHROPIC",
  "clientProtocolSupport": "ANTHROPIC,OPENAI",
  "priority": 1,
  "weight": 100,
  "fallbackGroup": "default",
  "supportStream": true,
  "supportTools": true,
  "supportVision": true,
  "supportReasoning": true,
  "maxInputTokens": 200000,
  "maxOutputTokens": 16384,
  "ownerType": "PLATFORM",
  "enabled": true
}
```

**Response `200 OK`:** Created route entity in `data`.

---

### `GET /api/routes/{id}`

Get a single model route by ID.

**Response `200 OK`:** Single `AiModelRoute` object in `data`.

---

### `PUT /api/routes/{id}`

Update an existing model route.

**Path Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| `id` | integer | Route ID |

**Request Body:** Full `AiModelRoute` object with updated fields.

**Response `200 OK`:** Updated route entity in `data`.

---

### `DELETE /api/routes/{id}`

Soft-delete (disable) a model route.

**Path Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| `id` | integer | Route ID |

**Response `200 OK`:**

```json
{
  "code": 200,
  "message": "OK",
  "data": null
}
```

---

## 7. Platform Service Tenant Endpoints

Manage tenants (sub-platform operators / SaaS operators).

**Base path:** `/api/tenants`
**Auth:** `Authorization: Bearer <session-token>` (all endpoints)

### Tenant Entity Fields

| Field | Type | Description |
|-------|------|-------------|
| `id` | integer | Auto-generated primary key |
| `tenantName` | string | Display name |
| `tenantCode` | string | Unique code identifier |
| `status` | string | `ACTIVE` or `DISABLED` |
| `contactName` | string | Primary contact person |
| `contactEmail` | string | Contact email |
| `contactPhone` | string | Contact phone |
| `maxUsers` | integer | Maximum user quota |
| `maxProviders` | integer | Maximum provider quota |
| `extraConfig` | string | JSON configuration blob |
| `remark` | string | Notes |
| `createTime` | datetime | Creation timestamp |
| `updateTime` | datetime | Last update timestamp |

---

### `GET /api/tenants`

List all active tenants.

**Response `200 OK`:**

```json
{
  "code": 200,
  "message": "OK",
  "data": [
    {
      "id": 1,
      "tenantName": "Acme Corp",
      "tenantCode": "acme",
      "status": "ACTIVE",
      "contactName": "John Doe",
      "contactEmail": "john@acme.com",
      "contactPhone": "+1-555-0100",
      "maxUsers": 100,
      "maxProviders": 10,
      "extraConfig": null,
      "remark": null,
      "createTime": "2026-06-01T08:00:00",
      "updateTime": "2026-06-01T08:00:00"
    }
  ]
}
```

---

### `POST /api/tenants`

Create a new tenant. If `status` is not provided, defaults to `ACTIVE`.

**Request Body:** Full `Tenant` object (excluding `id`, `createTime`, `updateTime`).

**Request Example:**

```json
{
  "tenantName": "Beta Inc",
  "tenantCode": "beta",
  "contactName": "Jane Smith",
  "contactEmail": "jane@beta.io",
  "maxUsers": 50,
  "maxProviders": 5
}
```

**Response `200 OK`:** Created tenant entity in `data`.

---

### `GET /api/tenants/{id}`

Get a single tenant by ID.

**Response `200 OK`:** Single `Tenant` object in `data`.

---

### `PUT /api/tenants/{id}`

Update an existing tenant.

**Path Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| `id` | integer | Tenant ID |

**Request Body:** Full `Tenant` object with updated fields.

**Response `200 OK`:** Updated tenant entity in `data`.

---

### `PUT /api/tenants/{id}/disable`

Disable a tenant. Disabled tenants cannot use the platform.

**Path Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| `id` | integer | Tenant ID |

**Response `200 OK`:**

```json
{
  "code": 200,
  "message": "OK",
  "data": null
}
```

---

### `PUT /api/tenants/{id}/enable`

Re-enable a previously disabled tenant.

**Path Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| `id` | integer | Tenant ID |

**Response `200 OK`:**

```json
{
  "code": 200,
  "message": "OK",
  "data": null
}
```

---

## 8. Platform Service Endpoint Testing

Test connectivity and model availability for provider endpoints.

**Base path:** `/api/provider-endpoints`
**Auth:** `Authorization: Bearer <session-token>` (all endpoints)

### `POST /api/provider-endpoints/{id}/test-connectivity`

Test connectivity to an endpoint. Probes the endpoint's `/models` (or equivalent) URL using the configured protocol and authentication.

**Path Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| `id` | integer | Endpoint ID |

**Request Body:**

| Field | Type | Required | Default | Description |
|-------|------|----------|---------|-------------|
| `protocol` | string | No | Endpoint's `defaultProtocol` | Protocol to test (`OPENAI`, `ANTHROPIC`) |

**Request Example:**

```json
{
  "protocol": "OPENAI"
}
```

**Success Response `200 OK`:**

```json
{
  "code": 200,
  "message": "OK",
  "data": {
    "url": "https://api.openai.com/v1/models",
    "statusCode": 200,
    "latencyMs": 245,
    "success": true,
    "body": "{\"object\":\"list\",\"data\":[...]}"
  }
}
```

**Failure Response `200 OK` (still returns data with error info):**

```json
{
  "code": 200,
  "message": "OK",
  "data": {
    "url": "https://api.openai.com/v1/models",
    "statusCode": 0,
    "latencyMs": 5023,
    "success": false,
    "error": "Connection timed out"
  }
}
```

> **Note:** Even when a connectivity test fails, the API returns `code: 200` with `success: false` in the data payload. This allows the frontend to display test results consistently.

**Endpoint Not Found:**

```json
{
  "code": 404,
  "message": "Endpoint not found",
  "data": null
}
```

---

### `POST /api/provider-endpoints/{id}/test-models`

Test model listing for an endpoint. Similar to test-connectivity but returns raw model list data.

**Path Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| `id` | integer | Endpoint ID |

**Request Body:**

| Field | Type | Required | Default | Description |
|-------|------|----------|---------|-------------|
| `protocol` | string | No | Endpoint's `defaultProtocol` | Protocol to test |

**Request Example:**

```json
{
  "protocol": "ANTHROPIC"
}
```

**Success Response `200 OK`:**

```json
{
  "code": 200,
  "message": "OK",
  "data": {
    "url": "https://api.anthropic.com/v1/models",
    "statusCode": 200,
    "latencyMs": 180,
    "success": true,
    "data": "{\"data\":[...]}"
  }
}
```

**Failure Response `200 OK`:**

```json
{
  "code": 200,
  "message": "OK",
  "data": {
    "url": "https://api.anthropic.com/v1/models",
    "statusCode": 0,
    "latencyMs": 3012,
    "success": false,
    "error": "Connection refused"
  }
}
```

---

## 9. Common Response Format and Error Codes

### Platform Service Response Envelope

All `ai-platform-service` endpoints wrap responses in a unified envelope:

```json
{
  "code": 200,
  "message": "OK",
  "data": <payload>
}
```

| Field | Type | Description |
|-------|------|-------------|
| `code` | integer | HTTP-style status code (200 = success) |
| `message` | string | Human-readable status message |
| `data` | any | Response payload (object, array, or null) |

### Standard Error Response

```json
{
  "code": <error-code>,
  "message": "<error-description>",
  "data": null
}
```

### Common Error Codes

| Code | Meaning | Typical Cause |
|------|---------|---------------|
| 200 | Success | — |
| 400 | Bad Request | Missing or invalid request body |
| 401 | Unauthorized | Missing or invalid session token |
| 403 | Forbidden | Insufficient permissions or disabled account |
| 404 | Not Found | Resource does not exist or is not authorized |
| 500 | Internal Server Error | Unexpected server failure |

### Relay Gateway Error Format

The Relay Gateway uses protocol-specific error formats to match the upstream API conventions:

**OpenAI-style error:**

```json
{
  "error": {
    "message": "<description>",
    "type": "server_error"
  }
}
```

**Anthropic-style error:**

```json
{
  "type": "error",
  "error": {
    "type": "server_error",
    "message": "<description>"
  }
}
```

---

## 10. Authentication and Authorization

The Nexora Gateway uses two distinct authentication mechanisms depending on the service.

### 10.1 Relay Gateway: API Key Authentication

End users and SDKs authenticate against the Relay Gateway using an **API Key**.

**Header format:**
- OpenAI endpoints: `Authorization: Bearer <api-key>`
- Anthropic endpoints: `x-api-key: <api-key>` (preferred) or `Authorization: Bearer <api-key>`

**Key format:** API keys are generated by the platform and have the prefix `sk-nex-`.

**How it works:**
1. The raw API key is never stored in plaintext. Only a SHA-256 hash is persisted.
2. Relay Gateway validates the key against a local cache (populated from Redis by platform-service config publish).
3. On each request, the gateway validates:
   - The key exists in cache
   - The key status is `ACTIVE`
   - The associated user and tenant are active
4. If validation fails, a `401` or `500` error is returned with details.

**Managing API keys:** Use the [Platform Service API Key Endpoints](#3-platform-service-api-key-endpoints) to create, list, and disable keys.

### 10.2 Platform Service: Session Token Authentication

Management console users and administrators authenticate against the Platform Service using a **Session Token**.

**Header format:** `Authorization: Bearer <session-token>`

**How it works:**
1. Call `POST /api/auth/login` with username and password to obtain a token.
2. The token is a UUID v4 string stored in an in-memory token store.
3. Include the token in the `Authorization` header for all subsequent API calls.
4. The token is validated on each request; invalid or expired tokens return a `401` error.
5. Call `POST /api/auth/logout` to invalidate the token.

**Token lifetime:** Tokens are stored in memory and are lost on service restart. In production, token storage should be migrated to Redis or a database.

**User types:**

| User Type | Description | Typical Roles |
|-----------|-------------|---------------|
| `PLATFORM_ADMIN` | Platform super admin | `PLATFORM_SUPER_ADMIN`, `USER_DEVELOPER` |
| `TENANT_MEMBER` | Tenant operator/member | `TENANT_ADMIN`, `USER_DEVELOPER` |
| `PLATFORM_CUSTOMER` | Platform direct C-end user | `USER_DEVELOPER` |
| `TENANT_CUSTOMER` | Tenant's C-end user | `USER_DEVELOPER` |

**Owner types:**

| Owner Type | Description |
|------------|-------------|
| `PLATFORM` | Resource owned by the platform |
| `TENANT` | Resource owned by a specific tenant |

### 10.3 Multi-Tenant Isolation

All resources (API keys, providers, endpoints, model routes) carry `ownerType` and `ownerTenantId` fields. The platform enforces:

- Platform admins can manage all resources across all tenants.
- Tenant admins can only manage resources where `ownerTenantId` matches their own tenant.
- C-end users can only manage their own resources (API keys, etc.).

---

## Endpoint Summary

| # | Service | Method | Path | Description |
|---|---------|--------|------|-------------|
| 1 | Relay | GET | `/v1/models` | List available models |
| 2 | Relay | POST | `/v1/chat/completions` | OpenAI chat completions (stream/non-stream) |
| 3 | Relay | POST | `/v1/messages` | Anthropic messages (stream/non-stream) |
| 4 | Platform | POST | `/api/auth/login` | Authenticate, get session token |
| 5 | Platform | GET | `/api/auth/me` | Get current user profile |
| 6 | Platform | GET | `/api/auth/menus` | Get dynamic menu tree |
| 7 | Platform | POST | `/api/auth/logout` | Invalidate session |
| 8 | Platform | POST | `/api/api-keys` | Create API key |
| 9 | Platform | GET | `/api/api-keys` | List API keys (paginated) |
| 10 | Platform | PUT | `/api/api-keys/{id}/disable` | Disable API key |
| 11 | Platform | GET | `/api/providers` | List providers |
| 12 | Platform | POST | `/api/providers` | Create provider |
| 13 | Platform | GET | `/api/providers/{id}` | Get provider |
| 14 | Platform | PUT | `/api/providers/{id}` | Update provider |
| 15 | Platform | DELETE | `/api/providers/{id}` | Disable provider |
| 16 | Platform | GET | `/api/endpoints` | List endpoints (optional filter) |
| 17 | Platform | POST | `/api/endpoints` | Create endpoint |
| 18 | Platform | GET | `/api/endpoints/{id}` | Get endpoint |
| 19 | Platform | PUT | `/api/endpoints/{id}` | Update endpoint |
| 20 | Platform | DELETE | `/api/endpoints/{id}` | Disable endpoint |
| 21 | Platform | GET | `/api/routes` | List model routes |
| 22 | Platform | POST | `/api/routes` | Create model route |
| 23 | Platform | GET | `/api/routes/{id}` | Get model route |
| 24 | Platform | PUT | `/api/routes/{id}` | Update model route |
| 25 | Platform | DELETE | `/api/routes/{id}` | Disable model route |
| 26 | Platform | GET | `/api/tenants` | List tenants |
| 27 | Platform | POST | `/api/tenants` | Create tenant |
| 28 | Platform | GET | `/api/tenants/{id}` | Get tenant |
| 29 | Platform | PUT | `/api/tenants/{id}` | Update tenant |
| 30 | Platform | PUT | `/api/tenants/{id}/disable` | Disable tenant |
| 31 | Platform | PUT | `/api/tenants/{id}/enable` | Enable tenant |
| 32 | Platform | POST | `/api/provider-endpoints/{id}/test-connectivity` | Test endpoint connectivity |
| 33 | Platform | POST | `/api/provider-endpoints/{id}/test-models` | Test endpoint model listing |
