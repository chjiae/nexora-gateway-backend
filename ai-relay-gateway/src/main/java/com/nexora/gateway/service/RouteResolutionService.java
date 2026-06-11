package com.nexora.gateway.service;

import com.nexora.common.enums.ProtocolType;
import com.nexora.common.model.ProviderEndpointSnapshot;
import com.nexora.common.model.ResolvedModelRoute;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Resolves model aliases to upstream routes using Redis snapshot data.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RouteResolutionService {

    private final Map<String, List<ResolvedModelRoute>> routeCache = new ConcurrentHashMap<>();
    private final Map<String, ProviderEndpointSnapshot> endpointCache = new ConcurrentHashMap<>();

    /**
     * Resolve a model alias to a route and endpoint.
     */
    public Mono<ResolvedModelRoute> resolve(String modelAlias, String clientProtocol) {
        List<ResolvedModelRoute> routes = routeCache.get(modelAlias);

        if (routes == null || routes.isEmpty()) {
            return Mono.error(new RouteException("No route found for model: " + modelAlias));
        }

        // Filter by client protocol support and enabled status
        ResolvedModelRoute bestRoute = routes.stream()
            .filter(r -> r.isEnabled())
            .filter(r -> r.getClientProtocolSupport() == null ||
                         r.getClientProtocolSupport().contains(ProtocolType.valueOf(clientProtocol.toUpperCase())))
            .filter(r -> r.getEndpoint() != null && r.getEndpoint().isEnabled())
            .filter(r -> !"UNHEALTHY".equals(r.getEndpoint().getHealthStatus()))
            .sorted((a, b) -> Integer.compare(b.getPriority(), a.getPriority()))
            .findFirst()
            .orElse(null);

        if (bestRoute == null) {
            return Mono.error(new RouteException("No healthy route available for model: " + modelAlias));
        }

        return Mono.just(bestRoute);
    }

    public void updateRoutes(Map<String, List<ResolvedModelRoute>> routes) {
        routeCache.putAll(routes);
    }

    public void updateEndpoints(Map<String, ProviderEndpointSnapshot> endpoints) {
        endpointCache.putAll(endpoints);
    }

    public static class RouteException extends RuntimeException {
        public RouteException(String message) {
            super(message);
        }
    }
}
