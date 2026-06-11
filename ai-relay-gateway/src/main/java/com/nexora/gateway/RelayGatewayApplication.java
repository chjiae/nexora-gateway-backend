package com.nexora.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Nexora AI Relay Gateway — hot-path relay service.
 * No database queries, no synchronous logging, no complex business logic.
 * Uses WebFlux + Reactor Netty for high-concurrency SSE streaming.
 */
@SpringBootApplication
public class RelayGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(RelayGatewayApplication.class, args);
    }
}
