package com.nexora.gateway.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexora.common.event.GatewayEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Publishes relay events to Redis Pub/Sub channels.
 * Publishing is fire-and-forget — must not block the streaming response.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EventPublisher {

    private final ReactiveRedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String REQUEST_CHANNEL = "ai-gateway:event:request";
    private static final String USAGE_CHANNEL = "ai-gateway:event:usage";
    private static final String BILLING_CHANNEL = "ai-gateway:event:billing";

    /**
     * Publish an event to the appropriate Redis channel.
     * Returns immediately — does not block the caller.
     */
    public void publish(GatewayEvent event) {
        String channel = resolveChannel(event);
        String payload;
        try {
            payload = objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize event: {}", e.getMessage());
            return;
        }

        redisTemplate.convertAndSend(channel, payload)
            .subscribe(
                count -> log.debug("Published event: type={}, id={}, channel={}",
                    event.getEventType(), event.getEventId(), channel),
                error -> log.error("Failed to publish event: id={}, error={}",
                    event.getEventId(), error.getMessage())
            );
    }

    private String resolveChannel(GatewayEvent event) {
        return switch (event.getEventType()) {
            case "UsageReportedEvent" -> USAGE_CHANNEL;
            case "RequestCompletedEvent", "RequestFailedEvent" -> BILLING_CHANNEL;
            default -> REQUEST_CHANNEL;
        };
    }
}
