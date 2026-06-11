package com.nexora.gateway.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexora.common.model.*;
import com.nexora.common.event.*;
import com.nexora.gateway.adapter.AnthropicProtocolAdapter;
import com.nexora.gateway.event.EventPublisher;
import com.nexora.gateway.service.ApiKeyAuthService;
import com.nexora.gateway.service.RouteResolutionService;
import com.nexora.gateway.service.UpstreamProxyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * Anthropic Compatible API endpoints (Claude Code compatible).
 * <ul>
 *   <li>POST /v1/messages — Messages API (stream and non-stream)</li>
 * </ul>
 */
@Slf4j
@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
public class AnthropicController {

    private final ApiKeyAuthService authService;
    private final RouteResolutionService routeService;
    private final AnthropicProtocolAdapter anthropicAdapter;
    private final UpstreamProxyService proxyService;
    private final EventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    @PostMapping("/messages")
    public Mono<Object> messages(
            @RequestHeader(value = "x-api-key", required = false) String apiKeyHeader,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestHeader(value = "anthropic-version", defaultValue = "2023-06-01") String version,
            @RequestBody JsonNode body) {

        String requestId = "req_" + UUID.randomUUID().toString().substring(0, 12);
        Instant start = Instant.now();
        String rawKey = extractKey(apiKeyHeader, authHeader);

        return authService.validate(rawKey)
            .flatMap(auth -> {
                InternalChatRequest internalRequest = anthropicAdapter.toInternal(body, requestId);

                return routeService.resolve(internalRequest.getModel(), "ANTHROPIC")
                    .flatMap(route -> {
                        publishEvent(createStartedEvent(requestId, auth, internalRequest));

                        if (internalRequest.isStream()) {
                            return handleStream(internalRequest, route, auth, start);
                        } else {
                            return handleNonStream(internalRequest, route, auth, start);
                        }
                    })
                    .onErrorResume(e -> handleError(e, requestId));
            });
    }

    private String extractKey(String apiKeyHeader, String authHeader) {
        if (apiKeyHeader != null && !apiKeyHeader.isEmpty()) {
            return apiKeyHeader;
        }
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        throw new RuntimeException("Missing x-api-key or Authorization header");
    }

    private Mono<Object> handleNonStream(InternalChatRequest request, ResolvedModelRoute route,
                                          ApiKeyAuthService.AuthResult auth, Instant start) {
        return proxyService.proxyNonStream(request, route)
            .map(response -> {
                long latencyMs = Duration.between(start, Instant.now()).toMillis();
                publishEvent(createCompletedEvent(request.getRequestId(), auth, route, response, latencyMs));
                return anthropicAdapter.toAnthropicResponse(response);
            })
            .map(r -> (Object) r);
    }

    private Mono<Object> handleStream(InternalChatRequest request, ResolvedModelRoute route,
                                       ApiKeyAuthService.AuthResult auth, Instant start) {
        Flux<String> stream = proxyService.proxyStream(request, route)
            .doOnComplete(() -> {
                publishEvent(createCompletedEvent(request.getRequestId(), auth, route, null,
                    Duration.between(start, Instant.now()).toMillis()));
            });

        return Mono.just(org.springframework.http.ResponseEntity.ok()
            .contentType(MediaType.TEXT_EVENT_STREAM)
            .body(stream));
    }

    private void publishEvent(GatewayEvent event) {
        try {
            eventPublisher.publish(event);
        } catch (Exception e) {
            log.warn("Failed to publish event: {}", e.getMessage());
        }
    }

    private RequestStartedEvent createStartedEvent(String requestId, ApiKeyAuthService.AuthResult auth,
                                                    InternalChatRequest request) {
        RequestStartedEvent event = new RequestStartedEvent("RequestStartedEvent");
        event.setRequestId(requestId);
        event.setTenantId(auth.tenantId() != null ? auth.tenantId().toString() : null);
        event.setUserId(auth.userId().toString());
        event.setApiKeyId(auth.keyHash());
        event.setModelAlias(request.getModel());
        event.setClientProtocol("ANTHROPIC");
        event.setStream(request.isStream());
        return event;
    }

    private RequestCompletedEvent createCompletedEvent(String requestId, ApiKeyAuthService.AuthResult auth,
                                                         ResolvedModelRoute route, InternalChatResponse response,
                                                         long latencyMs) {
        RequestCompletedEvent event = new RequestCompletedEvent("RequestCompletedEvent");
        event.setRequestId(requestId);
        event.setTenantId(auth.tenantId() != null ? auth.tenantId().toString() : null);
        event.setUserId(auth.userId().toString());
        event.setApiKeyId(auth.keyHash());
        event.setModelAlias(route.getModelAlias());
        event.setProviderId(route.getProviderId());
        event.setEndpointId(route.getEndpointId());
        event.setUpstreamModel(route.getUpstreamModel());
        event.setUpstreamProtocol(route.getUpstreamProtocol().name());
        event.setLatencyMs(latencyMs);
        event.setStream(false);
        event.setSuccess(true);
        event.setFinishReason("end_turn");
        if (response != null && response.getUsage() != null) {
            event.setInputTokens(response.getUsage().getInputTokens());
            event.setOutputTokens(response.getUsage().getOutputTokens());
            event.setTotalTokens(response.getUsage().getTotalTokens());
        }
        return event;
    }

    private Mono<Object> handleError(Throwable error, String requestId) {
        Map<String, Object> errorResponse = new LinkedHashMap<>();
        errorResponse.put("type", "error");
        Map<String, Object> errorBody = new LinkedHashMap<>();
        errorBody.put("type", "server_error");
        errorBody.put("message", error.getMessage());
        errorResponse.put("error", errorBody);
        return Mono.just(org.springframework.http.ResponseEntity
            .status(500)
            .contentType(MediaType.APPLICATION_JSON)
            .body(errorResponse));
    }
}
