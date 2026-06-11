package com.nexora.common.event;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class FallbackTriggeredEvent extends GatewayEvent {
    private String modelAlias;
    private String originalProviderId;
    private String originalEndpointId;
    private String fallbackProviderId;
    private String fallbackEndpointId;
    private String fallbackReason;
    private String originalErrorCode;

    public FallbackTriggeredEvent(String eventType) {
        super(eventType);
    }
}
