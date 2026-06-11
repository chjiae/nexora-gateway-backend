package com.nexora.platform;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Nexora AI Platform Service — all business logic, tenant management, billing,
 * logging, configuration, health checks, and event consumption.
 */
@SpringBootApplication
@MapperScan("com.nexora.platform.mapper")
public class PlatformServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PlatformServiceApplication.class, args);
    }
}
