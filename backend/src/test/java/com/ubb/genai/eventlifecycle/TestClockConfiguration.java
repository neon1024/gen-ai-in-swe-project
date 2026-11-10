package com.ubb.genai.eventlifecycle;

import java.time.Instant;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/** Replaces the production clock with a {@link MutableClock} starting at 2027-01-01T00:00:00Z. */
@TestConfiguration
public class TestClockConfiguration {

    @Bean
    @Primary
    MutableClock testClock() {
        return new MutableClock(Instant.parse("2027-01-01T00:00:00Z"));
    }
}
