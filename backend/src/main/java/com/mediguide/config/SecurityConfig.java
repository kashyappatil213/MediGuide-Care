package com.mediguide.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security configuration for MediGuide Care stateless REST API.
 * Configures JWT authentication filter, stateless session policy, CSRF disabling,
 * and endpoint access rules.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomAuthenticationEntryPoint authenticationEntryPoint;
    private final CustomAccessDeniedHandler accessDeniedHandler;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter,
            CustomAuthenticationEntryPoint authenticationEntryPoint,
            CustomAccessDeniedHandler accessDeniedHandler) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Disable CSRF for stateless REST API
                .csrf(AbstractHttpConfigurer::disable)

                // Enable CORS with defaults (integrates with CorsConfig)
                .cors(Customizer.withDefaults())

                // Stateless session management: no HTTP session is created or used
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // Custom JSON handlers for authentication (401) and authorization (403) failures
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                )

                // Endpoint authorization rules
                .authorizeHttpRequests(auth -> auth
                        // Public system health endpoints
                        .requestMatchers("/api/health", "/api/v1/health").permitAll()

                        // Public authentication endpoints
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/register", "/api/v1/auth/login").permitAll()

                        // Public Swagger / OpenAPI documentation endpoints
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/api-docs/**"
                        ).permitAll()

                        // Role-restricted test route: accessible only to DOCTOR role
                        .requestMatchers("/api/v1/test/doctor-only").hasRole("DOCTOR")

                        // All other /api/** endpoints require authentication
                        .requestMatchers("/api/**").authenticated()

                        // Any remaining unmatched requests require authentication
                        .anyRequest().authenticated()
                )

                // Insert the JWT filter before UsernamePasswordAuthenticationFilter
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * UserDetailsService placeholder to deactivate Spring Security's default
     * in-memory user generation (which outputs a random password warning at startup).
     * Authentication in MediGuide Care is exclusively handled via signed JWT tokens.
     */
    @Bean
    public org.springframework.security.core.userdetails.UserDetailsService userDetailsService() {
        return username -> {
            throw new org.springframework.security.core.userdetails.UsernameNotFoundException(
                    "Direct username/password authentication is not supported. Use /api/v1/auth/login to obtain a JWT."
            );
        };
    }
}
