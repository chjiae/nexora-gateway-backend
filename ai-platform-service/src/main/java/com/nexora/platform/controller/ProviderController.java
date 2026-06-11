package com.nexora.platform.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nexora.platform.dto.ApiResponse;
import com.nexora.platform.entity.AiProviderConfig;
import com.nexora.platform.mapper.AiProviderConfigMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/providers")
@RequiredArgsConstructor
public class ProviderController {

    private final AiProviderConfigMapper providerMapper;

    @GetMapping
    public ApiResponse<List<AiProviderConfig>> list() {
        return ApiResponse.success(providerMapper.selectList(
            new LambdaQueryWrapper<AiProviderConfig>().eq(AiProviderConfig::getEnabled, true)
        ));
    }

    @PostMapping
    public ApiResponse<AiProviderConfig> create(@RequestBody AiProviderConfig config) {
        providerMapper.insert(config);
        return ApiResponse.success(config);
    }

    @PutMapping("/{id}")
    public ApiResponse<AiProviderConfig> update(@PathVariable Long id, @RequestBody AiProviderConfig config) {
        config.setId(id);
        providerMapper.updateById(config);
        return ApiResponse.success(providerMapper.selectById(id));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        AiProviderConfig config = providerMapper.selectById(id);
        if (config != null) {
            config.setEnabled(false);
            providerMapper.updateById(config);
        }
        return ApiResponse.success(null);
    }

    @GetMapping("/{id}")
    public ApiResponse<AiProviderConfig> get(@PathVariable Long id) {
        return ApiResponse.success(providerMapper.selectById(id));
    }
}
