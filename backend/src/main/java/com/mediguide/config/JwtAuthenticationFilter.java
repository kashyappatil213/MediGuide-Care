package com.mediguide.config;

import com.mediguide.model.Role;
import com.mediguide.service.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Filter responsible for validating incoming JWT Bearer tokens and establishing
 * the authenticated security context for the request.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(BEARER_PREFIX.length()).trim();

        if (token.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            Claims claims = jwtService.parseAndValidateToken(token);

            String email = claims.getSubject();
            Number userIdNum = claims.get("userId", Number.class);
            Long userId = userIdNum != null ? userIdNum.longValue() : null;
            String roleName = claims.get("role", String.class);

            if (email != null && roleName != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                Role role = Role.valueOf(roleName);
                UserPrincipal principal = new UserPrincipal(userId, email, role);

                // Standard Spring Security authority format: ROLE_PATIENT, ROLE_DOCTOR, ROLE_ADMIN
                SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + role.name());

                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        List.of(authority)
                );
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (JwtException | IllegalArgumentException ex) {
            // Invalid, expired, or malformed token: clear context to ensure request remains unauthenticated.
            // Spring Security's AuthenticationEntryPoint will handle unauthorized requests to protected endpoints.
            SecurityContextHolder.clearContext();
            log.debug("JWT authentication skipped due to invalid token: {}", ex.getMessage());
        }

        filterChain.doFilter(request, response);
    }
}
