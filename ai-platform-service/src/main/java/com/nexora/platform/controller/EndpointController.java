package com.nexora.platform.controller;

import com.nexora.platform.dto.ApiResponse;
import com.nexora.platform.entity.AiProviderEndpoint;
import com.nexora.platform.mapper.AiProviderEndpointMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/endpoints")
@RequiredArgsConstructor
public class EndpointController {

    private final AiProviderEndpointMapper endpointMapper;

    @GetMapping
    public ApiResponse<List<AiProviderEndpoint>> list(@RequestParam(required = false) Long providerId) {
        if (providerId != null) {
            return ApiResponse.success(endpointMapper.findByProvider(providerId));
        }
        return ApiResponse.success(endpointMapper.findEnabled());
    }

    @PostMapping
    public ApiResponse<AiProviderEndpoint> create(@RequestBody AiProviderEndpoint endpoint) {
        endpointMapper.insert(endpoint);
        return ApiResponse.success(endpoint);
    }

    @PutMapping("/{id}")
    public ApiResponse<AiProviderEndpoint> update(@PathVariable Long id, @RequestBody AiProviderEndpoint endpoint) {
        endpoint.setId(id);
        endpointMapper.update(endpoint);
        return ApiResponse.success(endpointMapper.findById(id));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        AiProviderEndpoint ep = endpointMapper.findById(id);
        if (ep != null) {
            ep.setEnabled(false);
            endpointMapper.update(ep);
        }
        return ApiResponse.success(null);
    }

    @GetMapping("/{id}")
    public ApiResponse<AiProviderEndpoint> get(@PathVariable Long id) {
        return ApiResponse.success(endpointMapper.findById(id));
    }
}
