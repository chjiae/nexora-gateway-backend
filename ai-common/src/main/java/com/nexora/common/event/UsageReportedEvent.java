package com.nexora.common.event;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class UsageReportedEvent extends GatewayEvent {
    private String modelAlias;
    private String providerId;
    private String endpointId;
    private String upstreamModel;
    private long inputTokens;
    private long outputTokens;
    private long totalTokens;
    private BigDecimal upstreamCost;
    private BigDecimal platformCharge;
    private BigDecimal tenantCharge;
    private long latencyMs;

    public UsageReportedEvent(String eventType) {
        super(eventType);
    }
}
