package com.mediguide.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Configuration bean providing BCrypt password encoding across the application.
 */
@Configuration
public class PasswordConfig {

    /**
     * BCryptPasswordEncoder bean.
     * Uses the BCrypt adaptive hashing function with salt to hash passwords safely.
     *
     * @return PasswordEncoder instance
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
