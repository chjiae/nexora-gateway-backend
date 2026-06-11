package com.nexora.platform.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nexora.platform.dto.ApiResponse;
import com.nexora.platform.entity.Tenant;
import com.nexora.platform.mapper.TenantMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tenants")
@RequiredArgsConstructor
public class TenantController {

    private final TenantMapper tenantMapper;

    @GetMapping
    public ApiResponse<List<Tenant>> list() {
        return ApiResponse.success(tenantMapper.selectList(
            new LambdaQueryWrapper<Tenant>().eq(Tenant::getStatus, "ACTIVE")
        ));
    }

    @PostMapping
    public ApiResponse<Tenant> create(@RequestBody Tenant tenant) {
        tenantMapper.insert(tenant);
        return ApiResponse.success(tenant);
    }

    @PutMapping("/{id}")
    public ApiResponse<Tenant> update(@PathVariable Long id, @RequestBody Tenant tenant) {
        tenant.setId(id);
        tenantMapper.updateById(tenant);
        return ApiResponse.success(tenantMapper.selectById(id));
    }

    @PutMapping("/{id}/disable")
    public ApiResponse<Void> disable(@PathVariable Long id) {
        Tenant tenant = tenantMapper.selectById(id);
        if (tenant != null) {
            tenant.setStatus("DISABLED");
            tenantMapper.updateById(tenant);
        }
        return ApiResponse.success(null);
    }

    @PutMapping("/{id}/enable")
    public ApiResponse<Void> enable(@PathVariable Long id) {
        Tenant tenant = tenantMapper.selectById(id);
        if (tenant != null) {
            tenant.setStatus("ACTIVE");
            tenantMapper.updateById(tenant);
        }
        return ApiResponse.success(null);
    }

    @GetMapping("/{id}")
    public ApiResponse<Tenant> get(@PathVariable Long id) {
        return ApiResponse.success(tenantMapper.selectById(id));
    }
}
