package com.talkify.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {

    /**
     * Centralised Clock bean — injected into Application Services and Handlers
     * that need the current time. Using a Spring-managed Clock instead of
     * calling {@code Instant.now()} directly makes time-sensitive logic
     * testable: tests can inject a fixed {@code Clock} to control time.
     */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
