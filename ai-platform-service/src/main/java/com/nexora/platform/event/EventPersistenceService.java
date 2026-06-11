package com.nexora.platform.event;

import com.nexora.common.event.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Persists events received from Redis Pub/Sub to PostgreSQL.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EventPersistenceService {

    private final JdbcTemplate jdbcTemplate;

    @Transactional
    public void persist(String channel, GatewayEvent event) {
        logEventConsumed(event);

        if (event instanceof RequestCompletedEvent completed) {
            persistRequestLog(completed);
            persistBillingRecord(completed);
        } else if (event instanceof RequestFailedEvent failed) {
            persistFailedLog(failed);
        } else if (event instanceof UsageReportedEvent usage) {
            persistUsageRecord(usage);
        }
        // Other event types can be added later
    }

    private void logEventConsumed(GatewayEvent event) {
        jdbcTemplate.update(
            "INSERT INTO ai_event_consume_log (event_id, event_type, channel, consume_status) VALUES (?, ?, ?, ?)",
            event.getEventId(), event.getEventType(), "ai-gateway:event:request", "PROCESSED"
        );
    }

    private void persistRequestLog(RequestCompletedEvent event) {
        jdbcTemplate.update(
            "INSERT INTO ai_request_log (request_id, tenant_id, user_id, api_key_id, " +
            "model_alias, provider_id, endpoint_id, upstream_model, upstream_protocol, " +
            "stream, input_tokens, output_tokens, total_tokens, latency_ms, " +
            "first_token_latency_ms, success, finish_reason) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            event.getRequestId(), parseLong(event.getTenantId()), parseLong(event.getUserId()),
            parseLong(event.getApiKeyId()), event.getModelAlias(),
            parseLong(event.getProviderId()), parseLong(event.getEndpointId()),
            event.getUpstreamModel(), event.getUpstreamProtocol(),
            event.isStream(), event.getInputTokens(), event.getOutputTokens(),
            event.getTotalTokens(), event.getLatencyMs(), event.getFirstTokenLatencyMs(),
            true, event.getFinishReason()
        );
    }

    private void persistBillingRecord(RequestCompletedEvent event) {
        jdbcTemplate.update(
            "INSERT INTO ai_billing_record (event_id, request_id, tenant_id, user_id, api_key_id, " +
            "model_alias, provider_id, endpoint_id, upstream_model, " +
            "input_tokens, output_tokens, total_tokens, " +
            "upstream_cost, platform_charge, tenant_charge, billing_status) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
            "ON CONFLICT (event_id) DO NOTHING",
            event.getEventId(), event.getRequestId(), parseLong(event.getTenantId()),
            parseLong(event.getUserId()), parseLong(event.getApiKeyId()),
            event.getModelAlias(), parseLong(event.getProviderId()),
            parseLong(event.getEndpointId()), event.getUpstreamModel(),
            event.getInputTokens(), event.getOutputTokens(), event.getTotalTokens(),
            event.getUpstreamCost(), event.getPlatformCharge(), event.getTenantCharge(),
            "PENDING"
        );
    }

    private void persistFailedLog(RequestFailedEvent event) {
        jdbcTemplate.update(
            "INSERT INTO ai_request_log (request_id, tenant_id, user_id, api_key_id, " +
            "model_alias, provider_id, endpoint_id, upstream_model, " +
            "stream, latency_ms, success, error_code, error_message) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            event.getRequestId(), parseLong(event.getTenantId()), parseLong(event.getUserId()),
            parseLong(event.getApiKeyId()), event.getModelAlias(),
            parseLong(event.getProviderId()), parseLong(event.getEndpointId()),
            event.getUpstreamModel(), event.isStream(), event.getLatencyMs(),
            false, event.getErrorCode(), event.getErrorMessage()
        );
    }

    private void persistUsageRecord(UsageReportedEvent event) {
        persistBillingRecordFromUsage(event);
    }

    private void persistBillingRecordFromUsage(UsageReportedEvent event) {
        jdbcTemplate.update(
            "INSERT INTO ai_billing_record (event_id, request_id, tenant_id, user_id, api_key_id, " +
            "model_alias, provider_id, endpoint_id, upstream_model, " +
            "input_tokens, output_tokens, total_tokens, " +
            "upstream_cost, platform_charge, tenant_charge, billing_status) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
            "ON CONFLICT (event_id) DO NOTHING",
            event.getEventId(), event.getRequestId(), parseLong(event.getTenantId()),
            parseLong(event.getUserId()), parseLong(event.getApiKeyId()),
            event.getModelAlias(), parseLong(event.getProviderId()),
            parseLong(event.getEndpointId()), event.getUpstreamModel(),
            event.getInputTokens(), event.getOutputTokens(), event.getTotalTokens(),
            event.getUpstreamCost(), event.getPlatformCharge(), event.getTenantCharge(),
            "PENDING"
        );
    }

    private Long parseLong(String value) {
        if (value == null || value.isEmpty()) return null;
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
