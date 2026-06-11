package com.nexora.common.event;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class RequestStartedEvent extends GatewayEvent {
    private String modelAlias;
    private String clientProtocol;
    private boolean stream;

    public RequestStartedEvent(String eventType) {
        super(eventType);
    }
}
