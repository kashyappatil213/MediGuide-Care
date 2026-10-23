package com.mediguide.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Spring configuration providing a default system UTC/zone Clock bean.
 * Allows clean test substitution with fixed clocks for deterministic timestamp validation.
 */
@Configuration
public class DateTimeConfig {

    @Bean
    @ConditionalOnMissingBean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
