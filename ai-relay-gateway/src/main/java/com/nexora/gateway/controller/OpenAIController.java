package com.nexora.gateway.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexora.common.model.*;
import com.nexora.common.event.*;
import com.nexora.gateway.adapter.OpenAIProtocolAdapter;
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

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * OpenAI Compatible API endpoints.
 * <ul>
 *   <li>GET /v1/models — list available models</li>
 *   <li>POST /v1/chat/completions — chat completions (stream and non-stream)</li>
 * </ul>
 */
@Slf4j
@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
public class OpenAIController {

    private final ApiKeyAuthService authService;
    private final RouteResolutionService routeService;
    private final OpenAIProtocolAdapter openaiAdapter;
    private final UpstreamProxyService proxyService;
    private final EventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    @GetMapping("/models")
    public Mono<Map<String, Object>> listModels(@RequestHeader("Authorization") String authHeader) {
        return validateAuth(authHeader)
            .flatMap(auth -> {
                Map<String, Object> response = new LinkedHashMap<>();
                response.put("object", "list");
                response.put("data", List.of(
                    Map.of("id", "gpt-4o", "object", "model", "created", 1700000000, "owned_by", "nexora"),
                    Map.of("id", "gpt-4o-mini", "object", "model", "created", 1700000000, "owned_by", "nexora"),
                    Map.of("id", "claude-sonnet-4-20250514", "object", "model", "created", 1700000000, "owned_by", "nexora")
                ));
                return Mono.just(response);
            });
    }

    @PostMapping("/chat/completions")
    public Mono<Object> chatCompletions(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody JsonNode body) {

        String requestId = "req_" + UUID.randomUUID().toString().substring(0, 12);
        Instant start = Instant.now();

        return validateAuth(authHeader)
            .flatMap(auth -> {
                // Adapt OpenAI request to internal format
                InternalChatRequest internalRequest = openaiAdapter.toInternal(body, requestId);

                // Resolve route
                return routeService.resolve(internalRequest.getModel(), "OPENAI")
                    .flatMap(route -> {
                        // Publish started event
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

    private Mono<Object> handleNonStream(InternalChatRequest request, ResolvedModelRoute route,
                                          ApiKeyAuthService.AuthResult auth, Instant start) {
        return proxyService.proxyNonStream(request, route)
            .map(response -> {
                long latencyMs = Duration.between(start, Instant.now()).toMillis();

                // Publish completed event
                publishEvent(createCompletedEvent(request.getRequestId(), auth, route, response, latencyMs));

                return openaiAdapter.toOpenAIResponse(response);
            })
            .map(r -> (Object) r);
    }

    private Mono<Object> handleStream(InternalChatRequest request, ResolvedModelRoute route,
                                       ApiKeyAuthService.AuthResult auth, Instant start) {
        Flux<String> stream = proxyService.proxyStream(request, route)
            .doOnComplete(() -> {
                // Publish completed event on stream end
                RequestCompletedEvent event = createCompletedEvent(request.getRequestId(), auth, route, null,
                    Duration.between(start, Instant.now()).toMillis());
                publishEvent(event);
            })
            .doOnError(e -> {
                RequestFailedEvent event = createFailedEvent(request.getRequestId(), auth, route, e,
                    Duration.between(start, Instant.now()).toMillis());
                publishEvent(event);
            });

        return Mono.just(org.springframework.http.ResponseEntity.ok()
            .contentType(MediaType.TEXT_EVENT_STREAM)
            .body(stream));
    }

    private Mono<ApiKeyAuthService.AuthResult> validateAuth(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return Mono.error(new RuntimeException("Missing or invalid Authorization header"));
        }
        String rawKey = authHeader.substring(7);
        return authService.validate(rawKey);
    }

    private void publishEvent(GatewayEvent event) {
        try {
            eventPublisher.publish(event);
        } catch (Exception e) {
            log.warn("Failed to publish event (non-blocking): {}", e.getMessage());
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
        event.setClientProtocol("OPENAI");
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
        event.setFinishReason("stop");
        if (response != null && response.getUsage() != null) {
            event.setInputTokens(response.getUsage().getInputTokens());
            event.setOutputTokens(response.getUsage().getOutputTokens());
            event.setTotalTokens(response.getUsage().getTotalTokens());
        }
        return event;
    }

    private RequestFailedEvent createFailedEvent(String requestId, ApiKeyAuthService.AuthResult auth,
                                                  ResolvedModelRoute route, Throwable error, long latencyMs) {
        RequestFailedEvent event = new RequestFailedEvent("RequestFailedEvent");
        event.setRequestId(requestId);
        event.setTenantId(auth.tenantId() != null ? auth.tenantId().toString() : null);
        event.setUserId(auth.userId().toString());
        event.setApiKeyId(auth.keyHash());
        event.setModelAlias(route.getModelAlias());
        event.setProviderId(route.getProviderId());
        event.setEndpointId(route.getEndpointId());
        event.setErrorCode("UPSTREAM_ERROR");
        event.setErrorMessage(error.getMessage());
        event.setLatencyMs(latencyMs);
        return event;
    }

    private Mono<Object> handleError(Throwable error, String requestId) {
        Map<String, Object> errorResponse = new LinkedHashMap<>();
        Map<String, Object> errorBody = new LinkedHashMap<>();
        errorBody.put("message", error.getMessage());
        errorBody.put("type", "server_error");
        errorResponse.put("error", errorBody);
        return Mono.just(org.springframework.http.ResponseEntity
            .status(500)
            .contentType(MediaType.APPLICATION_JSON)
            .body(errorResponse));
    }
}
