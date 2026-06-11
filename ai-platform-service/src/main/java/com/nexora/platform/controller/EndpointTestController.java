package com.nexora.platform.controller;

import com.nexora.common.enums.ProtocolType;
import com.nexora.common.enums.UpstreamOperation;
import com.nexora.common.util.UrlBuilder;
import com.nexora.platform.dto.ApiResponse;
import com.nexora.platform.entity.AiProviderEndpoint;
import com.nexora.platform.mapper.AiProviderEndpointMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/provider-endpoints")
@RequiredArgsConstructor
public class EndpointTestController {

    private final AiProviderEndpointMapper endpointMapper;
    private final RestTemplate restTemplate = new RestTemplate();

    @PostMapping("/{id}/test-connectivity")
    public ApiResponse<Map<String, Object>> testConnectivity(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        AiProviderEndpoint endpoint = endpointMapper.selectById(id);
        if (endpoint == null) {
            return ApiResponse.error(404, "Endpoint not found");
        }

        String protocol = body.getOrDefault("protocol", endpoint.getDefaultProtocol());
        ProtocolType protocolType = ProtocolType.valueOf(protocol.toUpperCase());

        String url = UrlBuilder.build(endpoint.getBaseUrl(), endpoint.getPathPrefix(),
                protocolType, UpstreamOperation.MODELS);

        Instant start = Instant.now();
        try {
            HttpHeaders headers = new HttpHeaders();
            addAuthHeader(headers, endpoint);
            ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.GET, new HttpEntity<>(headers), String.class);

            long latencyMs = Duration.between(start, Instant.now()).toMillis();
            return ApiResponse.success(Map.of(
                "url", url,
                "statusCode", response.getStatusCode().value(),
                "latencyMs", latencyMs,
                "success", response.getStatusCode().is2xxSuccessful(),
                "body", response.getBody()
            ));
        } catch (Exception e) {
            long latencyMs = Duration.between(start, Instant.now()).toMillis();
            return ApiResponse.success(Map.of(
                "url", url,
                "statusCode", 0,
                "latencyMs", latencyMs,
                "success", false,
                "error", e.getMessage()
            ));
        }
    }

    @PostMapping("/{id}/test-models")
    public ApiResponse<Map<String, Object>> testModels(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        AiProviderEndpoint endpoint = endpointMapper.selectById(id);
        if (endpoint == null) {
            return ApiResponse.error(404, "Endpoint not found");
        }

        String protocol = body.getOrDefault("protocol", endpoint.getDefaultProtocol());
        ProtocolType protocolType = ProtocolType.valueOf(protocol.toUpperCase());

        String url = UrlBuilder.build(endpoint.getBaseUrl(), endpoint.getPathPrefix(),
                protocolType, UpstreamOperation.MODELS);

        Instant start = Instant.now();
        try {
            HttpHeaders headers = new HttpHeaders();
            addAuthHeader(headers, endpoint);
            ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.GET, new HttpEntity<>(headers), String.class);

            long latencyMs = Duration.between(start, Instant.now()).toMillis();
            return ApiResponse.success(Map.of(
                "url", url,
                "statusCode", response.getStatusCode().value(),
                "latencyMs", latencyMs,
                "success", response.getStatusCode().is2xxSuccessful(),
                "data", response.getBody()
            ));
        } catch (Exception e) {
            long latencyMs = Duration.between(start, Instant.now()).toMillis();
            return ApiResponse.success(Map.of(
                "url", url,
                "statusCode", 0,
                "latencyMs", latencyMs,
                "success", false,
                "error", e.getMessage()
            ));
        }
    }

    private void addAuthHeader(HttpHeaders headers, AiProviderEndpoint endpoint) {
        if ("API_KEY".equals(endpoint.getAuthType()) && endpoint.getApiKeyEncrypted() != null
                && !endpoint.getApiKeyEncrypted().startsWith("<")) {
            headers.set("Authorization", "Bearer " + endpoint.getApiKeyEncrypted());
        }
    }
}
