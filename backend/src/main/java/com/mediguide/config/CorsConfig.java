package com.mediguide.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Global CORS configuration to allow cross-origin requests from the React frontend
 * running on local workstations or on the same Wi-Fi local network (RFC 1918 private subnets).
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    private static final List<String> DEFAULT_ORIGIN_PATTERNS = List.of(
            "http://localhost:[*]",
            "http://127.0.0.1:[*]",
            "https://localhost:[*]",
            "https://127.0.0.1:[*]",
            "https://*.vercel.app",
            "https://*.loca.lt",
            "http://192.168.*:[*]",
            "http://10.*:[*]",
            "http://172.16.*:[*]",
            "http://172.17.*:[*]",
            "http://172.18.*:[*]",
            "http://172.19.*:[*]",
            "http://172.2*:[*]",
            "http://172.30.*:[*]",
            "http://172.31.*:[*]"
    );

    @Value("${app.cors.allowed-origins:}")
    private String customOriginsProperty;

    private List<String> resolveOriginPatterns() {
        List<String> patterns = new ArrayList<>(DEFAULT_ORIGIN_PATTERNS);
        if (customOriginsProperty != null && !customOriginsProperty.trim().isEmpty()) {
            Arrays.stream(customOriginsProperty.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .forEach(patterns::add);
        }
        return patterns;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        List<String> originPatterns = resolveOriginPatterns();
        registry.addMapping("/api/**")
                .allowedOriginPatterns(originPatterns.toArray(new String[0]))
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }

    /**
     * Spring Security CORS configuration source bean.
     * Integrates with SecurityFilterChain (.cors(Customizer.withDefaults()))
     * to handle preflight OPTIONS requests and security error responses.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(resolveOriginPatterns());
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}

