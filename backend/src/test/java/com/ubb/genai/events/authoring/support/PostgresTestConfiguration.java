package com.ubb.genai.events.authoring.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.PropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;

@TestConfiguration(proxyBeanMethods = false)
@PropertySource("classpath:event-authoring.properties")
public class PostgresTestConfiguration {
    @Bean
    @ServiceConnection
    PostgreSQLContainer postgres() {
        return new PostgreSQLContainer("postgres:17.6-alpine")
                .withDatabaseName("event_authoring_test")
                .withUsername("authoring_test")
                .withPassword("authoring_test");
    }
}
