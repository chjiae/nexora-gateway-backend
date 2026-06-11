package com.nexora.common.event;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class RequestCompletedEvent extends GatewayEvent {
    private String modelAlias;
    private String providerId;
    private String endpointId;
    private String upstreamModel;
    private String upstreamProtocol;
    private long inputTokens;
    private long outputTokens;
    private long totalTokens;
    private long cacheCreationInputTokens;
    private long cacheReadInputTokens;
    private BigDecimal upstreamCost;
    private BigDecimal platformCharge;
    private BigDecimal tenantCharge;
    private long latencyMs;
    private long firstTokenLatencyMs;
    private boolean stream;
    private boolean success;
    private String finishReason;

    public RequestCompletedEvent(String eventType) {
        super(eventType);
    }
}
