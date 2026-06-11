package com.nexora.common.event;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class RateLimitRejectedEvent extends GatewayEvent {
    private String modelAlias;
    private String limitType;
    private String limitKey;
    private long currentCount;
    private long limitValue;

    public RateLimitRejectedEvent(String eventType) {
        super(eventType);
    }
}
