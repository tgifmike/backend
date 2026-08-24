package com.backend.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.context.SecurityContextHolder;
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

                        // Browser CORS preflight requests never carry auth.
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // PUBLIC AUTH
                        .requestMatchers("/auth/**").permitAll()
                        .requestMatchers("/favicon.ico", "/error").permitAll()

                        // LOGIN ENDPOINTS
                        .requestMatchers(HttpMethod.POST, "/users/oauth-login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/users/demo-login").permitAll()

                        //EMAIL
                        .requestMatchers("/api/email/**").permitAll()

                        //AWS
                        .requestMatchers("/api/s3/test").permitAll()

                        // Restaurant temperature configuration is manager-only.
                        .requestMatchers(
                                HttpMethod.POST,
                                "/temperature-categories",
                                "/temperature-categories/location/*/defaults"
                        ).hasAuthority("APP_MANAGER")
                        .requestMatchers(
                                HttpMethod.PUT,
                                "/temperature-categories/*"
                        ).hasAuthority("APP_MANAGER")
                        .requestMatchers(
                                HttpMethod.PATCH,
                                "/temperature-categories/*/active"
                        ).hasAuthority("APP_MANAGER")
                        .requestMatchers(
                                HttpMethod.DELETE,
                                "/temperature-categories/*"
                        ).hasAuthority("APP_MANAGER")

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
