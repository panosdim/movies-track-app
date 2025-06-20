package eu.deltasw.common.security.web;

import java.io.IOException;
import java.util.Collections;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.filter.OncePerRequestFilter;

import eu.deltasw.common.security.JwtUtil;
import eu.deltasw.common.util.RequestContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    public JwtAuthenticationFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    @SuppressWarnings({ "java:67109781", "null" }) // Suppress warning for @NonNull
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {
        log.debug("Processing request: {} {}", request.getMethod(), request.getRequestURI());

        // Ensure RequestContextHolder has the current request
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request, response));

        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        log.debug("Authorization header: {}", authHeader);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("Missing or invalid Authorization header");
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            return;
        }

        String token = authHeader.substring(7);
        log.debug("Extracted token: {}", token.substring(0, Math.min(10, token.length())) + "...");

        try {
            boolean valid = jwtUtil.validateToken(token);
            log.debug("Token validation result: {}", valid);

            if (!valid) {
                log.error("JWT verification failed");
                response.setStatus(HttpStatus.UNAUTHORIZED.value());
                return;
            }

            // Add user information to request attributes
            String userId = jwtUtil.extractUserId(token);
            log.debug("Extracted user ID: {}", userId);
            RequestContext.setCurrentUserId(userId);

            // Set up Spring Security authentication
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    userId, null,
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));
            SecurityContextHolder.getContext().setAuthentication(authentication);

            log.debug("JWT validation successful, proceeding with request");
            filterChain.doFilter(request, response);
        } catch (Exception e) {
            log.error("Error validating JWT token", e);
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
        }
    }
}