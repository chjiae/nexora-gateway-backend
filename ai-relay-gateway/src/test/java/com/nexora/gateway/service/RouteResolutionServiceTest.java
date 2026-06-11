package com.nexora.gateway.service;

import com.nexora.common.enums.ProtocolType;
import com.nexora.common.model.ProviderEndpointSnapshot;
import com.nexora.common.model.ResolvedModelRoute;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link RouteResolutionService}.
 */
class RouteResolutionServiceTest {

    @Test
    void resolve_shouldReturnHighestPriorityRoute() {
        RouteResolutionService service = new RouteResolutionService();

        ProviderEndpointSnapshot healthyEndpoint = ProviderEndpointSnapshot.builder()
            .endpointId("ep1")
            .enabled(true)
            .healthStatus("HEALTHY")
            .build();

        List<ResolvedModelRoute> routes = List.of(
            ResolvedModelRoute.builder()
                .modelAlias("gpt-4o").enabled(true).priority(50)
                .upstreamProtocol(ProtocolType.OPENAI)
                .clientProtocolSupport(List.of(ProtocolType.OPENAI))
                .endpoint(healthyEndpoint)
                .build(),
            ResolvedModelRoute.builder()
                .modelAlias("gpt-4o").enabled(true).priority(100)
                .upstreamProtocol(ProtocolType.OPENAI)
                .clientProtocolSupport(List.of(ProtocolType.OPENAI))
                .endpoint(healthyEndpoint)
                .build()
        );

        service.updateRoutes(Map.of("gpt-4o", routes));
        ResolvedModelRoute result = service.resolve("gpt-4o", "OPENAI").block();

        assertNotNull(result);
        assertEquals(100, result.getPriority());
    }

    @Test
    void resolve_shouldFilterUnhealthyEndpoints() {
        RouteResolutionService service = new RouteResolutionService();

        List<ResolvedModelRoute> routes = List.of(
            ResolvedModelRoute.builder()
                .modelAlias("gpt-4o").enabled(true).priority(100)
                .upstreamProtocol(ProtocolType.OPENAI)
                .clientProtocolSupport(List.of(ProtocolType.OPENAI))
                .endpoint(ProviderEndpointSnapshot.builder()
                    .endpointId("ep1").enabled(true).healthStatus("UNHEALTHY").build())
                .build()
        );

        service.updateRoutes(Map.of("gpt-4o", routes));

        assertThrows(RouteResolutionService.RouteException.class, () ->
            service.resolve("gpt-4o", "OPENAI").block()
        );
    }

    @Test
    void resolve_shouldFilterByClientProtocol() {
        RouteResolutionService service = new RouteResolutionService();

        ProviderEndpointSnapshot healthyEndpoint = ProviderEndpointSnapshot.builder()
            .endpointId("ep1").enabled(true).healthStatus("HEALTHY").build();

        List<ResolvedModelRoute> routes = List.of(
            ResolvedModelRoute.builder()
                .modelAlias("gpt-4o").enabled(true).priority(100)
                .upstreamProtocol(ProtocolType.OPENAI)
                .clientProtocolSupport(List.of(ProtocolType.OPENAI))
                .endpoint(healthyEndpoint)
                .build()
        );

        service.updateRoutes(Map.of("gpt-4o", routes));

        assertThrows(RouteResolutionService.RouteException.class, () ->
            service.resolve("gpt-4o", "ANTHROPIC").block()
        );
    }

    @Test
    void resolve_unknownModel_shouldThrowException() {
        RouteResolutionService service = new RouteResolutionService();

        assertThrows(RouteResolutionService.RouteException.class, () ->
            service.resolve("nonexistent-model", "OPENAI").block()
        );
    }
}
