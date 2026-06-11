package com.nexora.platform.controller;

import com.nexora.platform.dto.ApiResponse;
import com.nexora.platform.entity.AiModelRoute;
import com.nexora.platform.mapper.AiModelRouteMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/routes")
@RequiredArgsConstructor
public class ModelRouteController {

    private final AiModelRouteMapper routeMapper;

    @GetMapping
    public ApiResponse<List<AiModelRoute>> list() {
        return ApiResponse.success(routeMapper.findEnabled());
    }

    @PostMapping
    public ApiResponse<AiModelRoute> create(@RequestBody AiModelRoute route) {
        routeMapper.insert(route);
        return ApiResponse.success(route);
    }

    @PutMapping("/{id}")
    public ApiResponse<AiModelRoute> update(@PathVariable Long id, @RequestBody AiModelRoute route) {
        route.setId(id);
        routeMapper.update(route);
        return ApiResponse.success(routeMapper.findById(id));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        AiModelRoute route = routeMapper.findById(id);
        if (route != null) {
            route.setEnabled(false);
            routeMapper.update(route);
        }
        return ApiResponse.success(null);
    }

    @GetMapping("/{id}")
    public ApiResponse<AiModelRoute> get(@PathVariable Long id) {
        return ApiResponse.success(routeMapper.findById(id));
    }
}
