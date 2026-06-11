package com.nexora.platform.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexora.common.event.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

/**
 * Redis Pub/Sub event consumer — subscribes to relay-gateway events and persists them.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EventConsumerService {

    private final RedisMessageListenerContainer listenerContainer;
    private final ObjectMapper objectMapper;
    private final EventPersistenceService persistenceService;

    private static final String REQUEST_CHANNEL = "ai-gateway:event:request";
    private static final String USAGE_CHANNEL = "ai-gateway:event:usage";
    private static final String BILLING_CHANNEL = "ai-gateway:event:billing";

    @PostConstruct
    public void subscribe() {
        listenerContainer.addMessageListener(new EventListener(REQUEST_CHANNEL),
                new ChannelTopic(REQUEST_CHANNEL));
        listenerContainer.addMessageListener(new EventListener(USAGE_CHANNEL),
                new ChannelTopic(USAGE_CHANNEL));
        listenerContainer.addMessageListener(new EventListener(BILLING_CHANNEL),
                new ChannelTopic(BILLING_CHANNEL));
        log.info("Subscribed to Redis channels: {}, {}, {}", REQUEST_CHANNEL, USAGE_CHANNEL, BILLING_CHANNEL);
    }

    private class EventListener implements MessageListener {
        private final String channel;

        EventListener(String channel) {
            this.channel = channel;
        }

        @Override
        public void onMessage(Message message, byte[] pattern) {
            try {
                String body = new String(message.getBody());
                String channelName = new String(message.getChannel());

                GatewayEvent event = objectMapper.readValue(body, GatewayEvent.class);
                log.debug("Received event: type={}, id={}, channel={}",
                        event.getEventType(), event.getEventId(), channelName);

                persistenceService.persist(channelName, event);
            } catch (Exception e) {
                log.error("Failed to process Redis message: {}", e.getMessage(), e);
            }
        }
    }
}
