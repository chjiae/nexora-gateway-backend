package com.nexora.common.event;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Base gateway event — all relay events extend this.
 */
@Data
@NoArgsConstructor
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "eventType")
@JsonSubTypes({
    @JsonSubTypes.Type(value = RequestCompletedEvent.class, name = "RequestCompletedEvent"),
    @JsonSubTypes.Type(value = RequestFailedEvent.class, name = "RequestFailedEvent"),
    @JsonSubTypes.Type(value = UsageReportedEvent.class, name = "UsageReportedEvent"),
    @JsonSubTypes.Type(value = RequestStartedEvent.class, name = "RequestStartedEvent"),
    @JsonSubTypes.Type(value = FallbackTriggeredEvent.class, name = "FallbackTriggeredEvent"),
    @JsonSubTypes.Type(value = RateLimitRejectedEvent.class, name = "RateLimitRejectedEvent")
})
public abstract class GatewayEvent {
    private String eventId;
    private String eventType;
    private String requestId;
    private String tenantId;
    private String userId;
    private String apiKeyId;
    private Instant occurredAt;

    protected GatewayEvent(String eventType) {
        this.eventId = UUID.randomUUID().toString();
        this.eventType = eventType;
        this.occurredAt = Instant.now();
    }
}
