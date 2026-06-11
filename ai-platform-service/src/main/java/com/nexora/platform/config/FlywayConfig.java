package com.nexora.platform.config;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * Explicit Flyway configuration for Spring Boot 4.x.
 *
 * In Spring Boot 4.x, Flyway auto-configuration was moved to a separate
 * {@code spring-boot-flyway} module. Since that module is not included as a
 * dependency, the auto-configuration classes (FlywayAutoConfiguration,
 * FlywayMigrationInitializer) are absent from the classpath and migrations
 * never run automatically.
 *
 * This configuration creates a Flyway bean and explicitly calls
 * {@code migrate()} on startup, ensuring database migrations are applied
 * before any other database-dependent beans are initialized.
 */
@Configuration
public class FlywayConfig {

    private static final Logger log = LoggerFactory.getLogger(FlywayConfig.class);

    @Value("${spring.flyway.locations:classpath:db/migration}")
    private String[] locations;

    @Value("${spring.flyway.baseline-on-migrate:true}")
    private boolean baselineOnMigrate;

    @Value("${spring.flyway.clean-disabled:true}")
    private boolean cleanDisabled;

    @Bean
    public Flyway flyway(DataSource dataSource) {
        log.info("Starting Flyway migration: locations={}, baselineOnMigrate={}, cleanDisabled={}",
                String.join(",", locations), baselineOnMigrate, cleanDisabled);

        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations(locations)
                .baselineOnMigrate(baselineOnMigrate)
                .cleanDisabled(cleanDisabled)
                .load();

        MigrateResult result = flyway.migrate();

        log.info("Flyway migration completed: success={}, migrationsExecuted={}, schemaVersion={}",
                result.success, result.migrationsExecuted, result.targetSchemaVersion);
        return flyway;
    }
}
