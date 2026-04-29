package com.backend.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
                // ----------------------------------------
                // CORS + CSRF
                // ----------------------------------------
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())

                // ----------------------------------------
                // API error responses
                // ----------------------------------------
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((req, res, authEx) -> {
                            res.setStatus(401);
                            res.setContentType("application/json");
                            res.getWriter().write("{\"error\":\"UNAUTHORIZED\"}");
                        })
                        .accessDeniedHandler((req, res, deniedEx) -> {
                            res.setStatus(403);
                            res.setContentType("application/json");
                            res.getWriter().write("{\"error\":\"FORBIDDEN\"}");
                        })
                )

                // ----------------------------------------
                // Route security
                // ORDER MATTERS
                // ----------------------------------------
                .authorizeHttpRequests(auth -> auth

                        // Public auth routes
                        .requestMatchers("/auth/**").permitAll()

                        // Public utility routes
                        .requestMatchers("/favicon.ico").permitAll()
                        .requestMatchers("/error").permitAll()

                        // Public login routes
                        .requestMatchers(HttpMethod.POST, "/users/oauth-login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/users/demo-login").permitAll()

                        // Protected routes
                        .requestMatchers(HttpMethod.POST, "/users/invite").authenticated()
                        .requestMatchers("/users/me").authenticated()

                        // Everything else protected
                        .anyRequest().authenticated()
                );

        // ----------------------------------------
        // JWT Filter
        // ----------------------------------------
        http.addFilterBefore(
                jwtAuthenticationFilter,
                UsernamePasswordAuthenticationFilter.class
        );

        return http.build();
    }
}