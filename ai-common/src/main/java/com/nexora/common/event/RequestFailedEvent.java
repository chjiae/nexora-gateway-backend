package com.nexora.common.event;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class RequestFailedEvent extends GatewayEvent {
    private String modelAlias;
    private String providerId;
    private String endpointId;
    private String upstreamModel;
    private String errorCode;
    private String errorMessage;
    private int httpStatusCode;
    private long latencyMs;
    private boolean stream;
    private boolean fallbackAttempted;
    private String fallbackResult;

    public RequestFailedEvent(String eventType) {
        super(eventType);
    }
}
