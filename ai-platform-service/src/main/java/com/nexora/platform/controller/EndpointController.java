package com.nexora.platform.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
        LambdaQueryWrapper<AiProviderEndpoint> wrapper = new LambdaQueryWrapper<>();
        if (providerId != null) {
            wrapper.eq(AiProviderEndpoint::getProviderId, providerId);
        }
        wrapper.eq(AiProviderEndpoint::getEnabled, true);
        return ApiResponse.success(endpointMapper.selectList(wrapper));
    }

    @PostMapping
    public ApiResponse<AiProviderEndpoint> create(@RequestBody AiProviderEndpoint endpoint) {
        endpointMapper.insert(endpoint);
        return ApiResponse.success(endpoint);
    }

    @PutMapping("/{id}")
    public ApiResponse<AiProviderEndpoint> update(@PathVariable Long id, @RequestBody AiProviderEndpoint endpoint) {
        endpoint.setId(id);
        endpointMapper.updateById(endpoint);
        return ApiResponse.success(endpointMapper.selectById(id));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        AiProviderEndpoint ep = endpointMapper.selectById(id);
        if (ep != null) {
            ep.setEnabled(false);
            endpointMapper.updateById(ep);
        }
        return ApiResponse.success(null);
    }

    @GetMapping("/{id}")
    public ApiResponse<AiProviderEndpoint> get(@PathVariable Long id) {
        return ApiResponse.success(endpointMapper.selectById(id));
    }
}
