# Redis Pub/Sub Event System

## Architecture Overview

Nexora Gateway uses Redis Pub/Sub as its event bus between the two backend services:

```
relay-gateway                Redis Pub/Sub               platform-service
     |                           |                            |
     |-- publish(event) -------->|                            |
     |   (fire-and-forget)       |                            |
     |                           |-- deliver message -------->|
     |                           |                            |-- deserialize -> GatewayEvent
     |                           |                            |-- persist -> PostgreSQL
```

- **relay-gateway** publishes events to Redis channels asynchronously. Publishing is fire-and-forget -- it must never block the streaming response.
- **platform-service** subscribes to those channels via `RedisMessageListenerContainer` and persists events to PostgreSQL.
- The relay publishes; the platform consumes. They share no database connection and no synchronous coupling.

**Fire-and-forget guarantee**: The `EventPublisher` uses Reactive Redis (`ReactiveRedisTemplate`). The `subscribe()` call is non-blocking. If Redis is unavailable or publishing fails, the error is logged but never thrown to the caller. The relay hot path is never blocked by event publishing.

---

## Channels

Three Redis Pub/Sub channels are defined, and the same constants appear in both services:

| Channel | Purpose | Events Routed Here |
|---|---|---|
| `ai-gateway:event:request` | Request lifecycle events | `RequestStartedEvent`, `FallbackTriggeredEvent`, `RateLimitRejectedEvent` |
| `ai-gateway:event:usage` | Usage tracking events | `UsageReportedEvent` |
| `ai-gateway:event:billing` | Billing events | `RequestCompletedEvent`, `RequestFailedEvent` |

The channel routing logic lives in `EventPublisher.resolveChannel()`:

```java
private String resolveChannel(GatewayEvent event) {
    return switch (event.getEventType()) {
        case "UsageReportedEvent" -> USAGE_CHANNEL;
        case "RequestCompletedEvent", "RequestFailedEvent" -> BILLING_CHANNEL;
        default -> REQUEST_CHANNEL;
    };
}
```

Any event type not explicitly listed falls through to `ai-gateway:event:request`.

---

## Event Type Hierarchy

All events extend the abstract base class `GatewayEvent` (package `com.nexora.common.event`).

### Base Class: `GatewayEvent`

```java
public abstract class GatewayEvent {
    private String eventId;       // UUID, auto-generated in constructor
    private String eventType;     // Simple class name, e.g. "RequestCompletedEvent"
    private String requestId;     // Correlation id for the request lifecycle
    private String tenantId;      // Tenant id (String, parsed to Long on persist)
    private String userId;        // User id (String, parsed to Long on persist)
    private String apiKeyId;     // API Key id (String, parsed to Long on persist)
    private Instant occurredAt;   // Timestamp, auto-set to Instant.now()

    protected GatewayEvent(String eventType) {
        this.eventId = UUID.randomUUID().toString();
        this.eventType = eventType;
        this.occurredAt = Instant.now();
    }
}
```

Jackson polymorphic deserialization is configured via `@JsonTypeInfo` and `@JsonSubTypes` on the base class, so the consumer can deserialize any concrete event type from a single `objectMapper.readValue(body, GatewayEvent.class)` call.

### Concrete Event Types

#### `RequestStartedEvent`

Emitted when a new AI API request begins processing.

| Field | Type | Description |
|---|---|---|
| `modelAlias` | `String` | The model alias requested by the client, e.g. `"gpt-4o"` |
| `clientProtocol` | `String` | Client protocol, e.g. `"OPENAI"` or `"ANTHROPIC"` |
| `stream` | `boolean` | Whether the request is streaming |

Plus all base class fields.

#### `RequestCompletedEvent`

Emitted when an upstream request completes successfully (finish reason arrived, stream ended).

| Field | Type | Description |
|---|---|---|
| `modelAlias` | `String` | Model alias from routing |
| `providerId` | `String` | Upstream provider id |
| `endpointId` | `String` | Upstream endpoint id |
| `upstreamModel` | `String` | Actual model name sent upstream |
| `upstreamProtocol` | `String` | Protocol used upstream, e.g. `"OPENAI"` |
| `inputTokens` | `long` | Prompt tokens |
| `outputTokens` | `long` | Completion tokens |
| `totalTokens` | `long` | Total tokens |
| `cacheCreationInputTokens` | `long` | Cache write tokens (Anthropic prompt caching) |
| `cacheReadInputTokens` | `long` | Cache hit tokens (Anthropic prompt caching) |
| `upstreamCost` | `BigDecimal` | Cost we paid the upstream provider |
| `platformCharge` | `BigDecimal` | Platform's mark-up |
| `tenantCharge` | `BigDecimal` | Amount charged to the tenant |
| `latencyMs` | `long` | Total request latency in milliseconds |
| `firstTokenLatencyMs` | `long` | Time to first token (TTFT) in milliseconds |
| `stream` | `boolean` | Whether the request was streaming |
| `success` | `boolean` | Always `true` for this event |
| `finishReason` | `String` | Upstream finish reason, e.g. `"stop"`, `"length"` |

Plus all base class fields.

#### `RequestFailedEvent`

Emitted when an upstream request fails (network error, upstream error, timeout, etc.).

| Field | Type | Description |
|---|---|---|
| `modelAlias` | `String` | Model alias from routing |
| `providerId` | `String` | Provider that was attempted |
| `endpointId` | `String` | Endpoint that was attempted |
| `upstreamModel` | `String` | Model sent upstream |
| `errorCode` | `String` | Machine-readable error code, e.g. `"UPSTREAM_ERROR"`, `"TIMEOUT"` |
| `errorMessage` | `String` | Human-readable error message |
| `httpStatusCode` | `int` | HTTP status from upstream, e.g. `504`, `502` |
| `latencyMs` | `long` | Time elapsed before failure |
| `stream` | `boolean` | Whether the request was streaming |
| `fallbackAttempted` | `boolean` | Whether fallback was triggered for this request |
| `fallbackResult` | `String` | Result of the fallback, e.g. `"SUCCESS"`, `"FAILED"` |

Plus all base class fields.

#### `UsageReportedEvent`

Emitted for usage tracking. Can be used independently (mid-stream usage snapshots) or in addition to `RequestCompletedEvent`.

| Field | Type | Description |
|---|---|---|
| `modelAlias` | `String` | Model alias |
| `providerId` | `String` | Upstream provider id |
| `endpointId` | `String` | Upstream endpoint id |
| `upstreamModel` | `String` | Actual upstream model name |
| `inputTokens` | `long` | Prompt tokens |
| `outputTokens` | `long` | Completion tokens |
| `totalTokens` | `long` | Total tokens |
| `upstreamCost` | `BigDecimal` | Cost to upstream provider |
| `platformCharge` | `BigDecimal` | Platform charge |
| `tenantCharge` | `BigDecimal` | Tenant charge |
| `latencyMs` | `long` | Latency in milliseconds |

Plus all base class fields.

#### `FallbackTriggeredEvent`

Emitted when the relay falls back from one provider/endpoint to another.

| Field | Type | Description |
|---|---|---|
| `modelAlias` | `String` | Model alias |
| `originalProviderId` | `String` | Provider that failed |
| `originalEndpointId` | `String` | Endpoint that failed |
| `fallbackProviderId` | `String` | Fallback provider used |
| `fallbackEndpointId` | `String` | Fallback endpoint used |
| `fallbackReason` | `String` | Why fallback was triggered |
| `originalErrorCode` | `String` | Error code from the original failure |

Plus all base class fields.

#### `RateLimitRejectedEvent`

Emitted when a request is rejected due to rate limiting or quota exhaustion.

| Field | Type | Description |
|---|---|---|
| `modelAlias` | `String` | Model alias requested |
| `limitType` | `String` | Type of limit, e.g. `"RPM"`, `"TPM"`, `"QUOTA"` |
| `limitKey` | `String` | Key used for the rate limit counter |
| `currentCount` | `long` | Current count at rejection time |
| `limitValue` | `long` | Configured limit value |

Plus all base class fields.

---

## Publisher: `EventPublisher` (relay-gateway)

Location: `ai-relay-gateway/src/main/java/com/nexora/gateway/event/EventPublisher.java`

Key implementation details:

- Uses `ReactiveRedisTemplate<String, Object>` for non-blocking publish.
- Uses Jackson `ObjectMapper` for serialization.
- Serialization failure is caught and logged; the event is silently dropped (does not throw).
- `redisTemplate.convertAndSend(channel, payload).subscribe(...)` returns immediately. The subscriber callbacks (onNext, onError) run asynchronously.
- Publish errors are logged at ERROR level but never propagated.

**Usage pattern in relay code:**

```java
eventPublisher.publish(event); // fire-and-forget, never blocks
// relay continues processing the response immediately
```

---

## Consumer: `EventConsumerService` (platform-service)

Location: `ai-platform-service/src/main/java/com/nexora/platform/event/EventConsumerService.java`

Key implementation details:

- Uses `RedisMessageListenerContainer` (blocking Redis client) for subscription.
- Subscribes to all three channels in a `@PostConstruct` method.
- Each channel gets an inner `EventListener` implementing `MessageListener`.
- On message arrival:
  1. Reads `message.getBody()` as a UTF-8 string.
  2. Deserializes via `objectMapper.readValue(body, GatewayEvent.class)` -- Jackson polymorphism resolves the concrete type.
  3. Delegates to `EventPersistenceService.persist(channelName, event)`.
- Deserialization failures are caught, logged at ERROR level, and the message is skipped (no retry in the current version).

---

## Persistence: `EventPersistenceService` (platform-service)

Location: `ai-platform-service/src/main/java/com/nexora/platform/event/EventPersistenceService.java`

Uses `JdbcTemplate` for direct SQL inserts. All operations in a single method are wrapped in `@Transactional`.

### Persistence Flow

```
persist(channel, event)
  |
  |-- logEventConsumed(event)
  |     INSERT INTO ai_event_consume_log (event_id, event_type, channel, consume_status)
  |     VALUES (?, ?, ?, 'PROCESSED')
  |
  |-- instanceof RequestCompletedEvent?
  |     |-- persistRequestLog(completed)
  |     |     INSERT INTO ai_request_log (...)
  |     |     with success=true, all token/cost/latency fields
  |     |
  |     |-- persistBillingRecord(completed)
  |           INSERT INTO ai_billing_record (...)
  |           ON CONFLICT (event_id) DO NOTHING
  |
  |-- instanceof RequestFailedEvent?
  |     |-- persistFailedLog(failed)
  |           INSERT INTO ai_request_log (...)
  |           with success=false, error_code, error_message
  |
  |-- instanceof UsageReportedEvent?
        |-- persistUsageRecord(usage)
              |-- persistBillingRecordFromUsage(usage)
                    INSERT INTO ai_billing_record (...)
                    ON CONFLICT (event_id) DO NOTHING
```

### Tables Written

| Table | Written By | Notes |
|---|---|---|
| `ai_event_consume_log` | All events | Audit log of every event consumed |
| `ai_request_log` | `RequestCompletedEvent`, `RequestFailedEvent` | Request-level log with success/failure |
| `ai_billing_record` | `RequestCompletedEvent`, `UsageReportedEvent` | Billing record with `ON CONFLICT (event_id) DO NOTHING` |

### ID Type Conversion

The base class stores `tenantId`, `userId`, `apiKeyId`, `providerId`, and `endpointId` as `String`. The persistence service converts them to `Long` via a helper:

```java
private Long parseLong(String value) {
    if (value == null || value.isEmpty()) return null;
    try {
        return Long.parseLong(value);
    } catch (NumberFormatException e) {
        return null;
    }
}
```

NULL values are written to the database when the string is empty or unparseable.

---

## Idempotency

Billing events (`RequestCompletedEvent`, `UsageReportedEvent`) use the `eventId` field as a unique key for idempotency.

The `ai_billing_record` INSERT uses:

```sql
ON CONFLICT (event_id) DO NOTHING
```

This guarantees:
- If the same event is delivered more than once (Redis Pub/Sub at-most-once semantics, network retry, or consumer replay), only the first insert succeeds.
- No duplicate billing records are created.
- The platform charge and tenant charge are recorded exactly once per request.

`ai_event_consume_log` does not have this guard -- every consume attempt is logged (may have duplicates if retried).

`ai_request_log` does not have this guard in the current version. A duplicate delivery would produce duplicate request log rows.

---

## Failure Handling

### Redis Publish Failure

- `EventPublisher.publish()` catches `JsonProcessingException` during serialization and logs at ERROR. No event is sent.
- If `convertAndSend` itself fails (Redis down, network error), the reactive subscriber's error callback logs at ERROR. The caller is never notified.
- The relay continues serving requests normally without Redis.

### Redis Connection Lost

- Spring Data Redis manages the connection lifecycle. The `ReactiveRedisTemplate` (publisher) and `RedisMessageListenerContainer` (consumer) both auto-reconnect when Redis becomes available again.
- During the outage window, events published by the relay are lost. Events are not queued or retried.

### Consumer Deserialization Failure

- If a message body cannot be deserialized to a `GatewayEvent` (malformed JSON, unknown event type, schema mismatch), `EventConsumerService.onMessage()` catches the exception, logs at ERROR, and returns.
- The message is not requeued, not retried, and not written to a dead letter queue in the current version.

### Database Write Failure

- If a `persist*()` method throws (constraint violation, connection failure, etc.), the `@Transactional` method rolls back.
- For `persist()`: if `persistRequestLog()` succeeds but `persistBillingRecord()` fails, both roll back. The event is effectively lost from the platform-service perspective.
- For `RequestFailedEvent` persistence: if the INSERT fails, the whole `persist()` call rolls back, including the `ai_event_consume_log` entry that was already inserted.
- No retry mechanism exists in the current version.

---

## Event Payload Examples

### RequestStartedEvent

```json
{
  "eventId": "550e8400-e29b-41d4-a716-446655440000",
  "eventType": "RequestStartedEvent",
  "requestId": "req_a1b2c3d4",
  "tenantId": "1",
  "userId": "42",
  "apiKeyId": "10",
  "occurredAt": "2026-01-01T00:00:00Z",
  "modelAlias": "gpt-4o",
  "clientProtocol": "OPENAI",
  "stream": true
}
```

### RequestCompletedEvent

```json
{
  "eventId": "550e8400-e29b-41d4-a716-446655440001",
  "eventType": "RequestCompletedEvent",
  "requestId": "req_a1b2c3d4",
  "tenantId": "1",
  "userId": "42",
  "apiKeyId": "10",
  "occurredAt": "2026-01-01T00:00:05Z",
  "modelAlias": "gpt-4o",
  "providerId": "1",
  "endpointId": "3",
  "upstreamModel": "gpt-4o",
  "upstreamProtocol": "OPENAI",
  "inputTokens": 150,
  "outputTokens": 320,
  "totalTokens": 470,
  "cacheCreationInputTokens": 0,
  "cacheReadInputTokens": 0,
  "upstreamCost": 0.0047,
  "platformCharge": 0.00235,
  "tenantCharge": 0.00705,
  "latencyMs": 2100,
  "firstTokenLatencyMs": 450,
  "stream": true,
  "success": true,
  "finishReason": "stop"
}
```

### RequestFailedEvent

```json
{
  "eventId": "550e8400-e29b-41d4-a716-446655440002",
  "eventType": "RequestFailedEvent",
  "requestId": "req_e5f6g7h8",
  "tenantId": "1",
  "userId": "42",
  "apiKeyId": "10",
  "occurredAt": "2026-01-01T00:01:30Z",
  "modelAlias": "gpt-4o",
  "providerId": "1",
  "endpointId": "3",
  "upstreamModel": "gpt-4o",
  "errorCode": "UPSTREAM_ERROR",
  "errorMessage": "Connection timeout after 30000ms",
  "httpStatusCode": 504,
  "latencyMs": 30000,
  "stream": true,
  "fallbackAttempted": true,
  "fallbackResult": "SUCCESS"
}
```

### UsageReportedEvent

```json
{
  "eventId": "550e8400-e29b-41d4-a716-446655440003",
  "eventType": "UsageReportedEvent",
  "requestId": "req_a1b2c3d4",
  "tenantId": "1",
  "userId": "42",
  "apiKeyId": "10",
  "occurredAt": "2026-01-01T00:00:03Z",
  "modelAlias": "gpt-4o",
  "providerId": "1",
  "endpointId": "3",
  "upstreamModel": "gpt-4o",
  "inputTokens": 150,
  "outputTokens": 200,
  "totalTokens": 350,
  "upstreamCost": 0.0035,
  "platformCharge": 0.00175,
  "tenantCharge": 0.00525,
  "latencyMs": 2100
}
```

### FallbackTriggeredEvent

```json
{
  "eventId": "550e8400-e29b-41d4-a716-446655440004",
  "eventType": "FallbackTriggeredEvent",
  "requestId": "req_e5f6g7h8",
  "tenantId": "1",
  "userId": "42",
  "apiKeyId": "10",
  "occurredAt": "2026-01-01T00:01:25Z",
  "modelAlias": "gpt-4o",
  "originalProviderId": "1",
  "originalEndpointId": "3",
  "fallbackProviderId": "2",
  "fallbackEndpointId": "5",
  "fallbackReason": "UPSTREAM_TIMEOUT",
  "originalErrorCode": "TIMEOUT"
}
```

### RateLimitRejectedEvent

```json
{
  "eventId": "550e8400-e29b-41d4-a716-446655440005",
  "eventType": "RateLimitRejectedEvent",
  "requestId": "req_i9j0k1l2",
  "tenantId": "1",
  "userId": "42",
  "apiKeyId": "10",
  "occurredAt": "2026-01-01T00:02:00Z",
  "modelAlias": "gpt-4o",
  "limitType": "RPM",
  "limitKey": "tenant:1:model:gpt-4o",
  "currentCount": 100,
  "limitValue": 100
}
```

---

## Consumer Startup

`EventConsumerService` uses `@PostConstruct` to register listeners:

```java
@PostConstruct
public void subscribe() {
    listenerContainer.addMessageListener(new EventListener(REQUEST_CHANNEL),
            new ChannelTopic(REQUEST_CHANNEL));
    listenerContainer.addMessageListener(new EventListener(USAGE_CHANNEL),
            new ChannelTopic(USAGE_CHANNEL));
    listenerContainer.addMessageListener(new EventListener(BILLING_CHANNEL),
            new ChannelTopic(BILLING_CHANNEL));
    log.info("Subscribed to Redis channels: {}, {}, {}",
            REQUEST_CHANNEL, USAGE_CHANNEL, BILLING_CHANNEL);
}
```

All three listeners are registered before the application is fully ready. If `RedisMessageListenerContainer` is not yet connected, Spring Data Redis will deliver messages once the connection is established.

---

## Current Limitations

These are known limitations of the current (Phase 1) implementation:

1. **At-most-once delivery**: Redis Pub/Sub does not persist messages. If the consumer is down, events published during the outage are lost.
2. **No retry on deserialization failure**: Malformed messages are logged and discarded.
3. **No dead letter queue**: Failed events are not stored for later inspection or replay.
4. **Single consumer**: Redis Pub/Sub delivers each message to all subscribers, but there is no consumer group concept. If multiple platform-service instances subscribe, each instance processes every message (duplicate processing). The `ON CONFLICT (event_id) DO NOTHING` on billing records mitigates this but is not a complete solution.
5. **No backpressure**: If the consumer cannot keep up with the publish rate, messages may pile up in Redis memory buffers.
6. **No message ordering guarantees across channels**: Events on different channels may arrive out of chronological order relative to each other.

---

## Future Extension Points

### Redis Streams

Migrate from Pub/Sub to Redis Streams for at-least-once delivery:

- Each channel becomes a stream (e.g. `ai-gateway:stream:request`).
- Consumer groups enable multiple platform-service instances to process messages in parallel without duplication.
- Pending entries list enables retry of failed messages.
- Message acknowledgment (`XACK`) provides reliable processing guarantees.

### Dead Letter Queue

Failed events (deserialization errors, persistence failures) can be written to a separate Redis stream or list for manual inspection and replay:

```
ai-gateway:dlq:request
ai-gateway:dlq:usage
ai-gateway:dlq:billing
```

### Multiple Consumer Groups

With Redis Streams, multiple consumer groups can process different event categories in parallel, each with independent progress tracking.

### Kafka / Pulsar Migration

For production deployments requiring stronger durability and ordering guarantees, the event bus can be migrated to Apache Kafka or Apache Pulsar:

- Kafka topics with partitioning by `tenantId` for ordered per-tenant processing.
- Pulsar with built-in multi-tenancy alignment to Nexora's tenant model.
- The `GatewayEvent` base class and event types in `ai-common` remain unchanged -- only the transport layer changes.

### Event Replay

Future versions can support replaying events from a persistent store (Redis Streams history, Kafka log compaction) to rebuild billing records or audit trails.
