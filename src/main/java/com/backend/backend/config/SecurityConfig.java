package com.backend.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.boot.web.servlet.FilterRegistrationBean;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final PinActionTokenFilter pinActionTokenFilter;
    private final DeviceAuthenticationFilter deviceAuthenticationFilter;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter,
            PinActionTokenFilter pinActionTokenFilter,
            DeviceAuthenticationFilter deviceAuthenticationFilter
    ) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.pinActionTokenFilter = pinActionTokenFilter;
        this.deviceAuthenticationFilter = deviceAuthenticationFilter;
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

                        // Device-authenticated routes cannot be reached by OAuth or PIN action tokens.
                        .requestMatchers(HttpMethod.GET, "/ipad/devices/*/pin-verifiers")
                        .hasAuthority("DEVICE_AUTH")
                        .requestMatchers(HttpMethod.POST, "/ipad/devices/*/pin-events/batch")
                        .hasAuthority("DEVICE_AUTH")

                        // Account manager operations are additionally account-scoped in services.
                        .requestMatchers(HttpMethod.POST, "/ipad/devices/enroll")
                        .hasAnyAuthority("APP_MANAGER", "ROLE_ADMIN", "ROLE_SRADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/ipad/devices/*")
                        .hasAnyAuthority("APP_MANAGER", "ROLE_ADMIN", "ROLE_SRADMIN")
                        .requestMatchers(HttpMethod.GET, "/ipad/devices/accounts/*")
                        .hasAnyAuthority("APP_MANAGER", "ROLE_ADMIN", "ROLE_SRADMIN")
                        .requestMatchers("/accounts/*/users/*/pin/**")
                        .hasAnyAuthority("APP_MANAGER", "ROLE_ADMIN", "ROLE_SRADMIN")
                        .requestMatchers(HttpMethod.GET, "/user-access/*/getUsersForAccount")
                        .hasAnyAuthority("APP_MANAGER", "ROLE_ADMIN", "ROLE_SRADMIN")
                        .requestMatchers(HttpMethod.POST, "/user-access/*/accounts/*")
                        .hasAnyAuthority("APP_MANAGER", "ROLE_ADMIN", "ROLE_SRADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/user-access/*/accounts/*")
                        .hasAnyAuthority("APP_MANAGER", "ROLE_ADMIN", "ROLE_SRADMIN")
                        .requestMatchers(HttpMethod.POST, "/user-access-locations/*/locations/*")
                        .hasAnyAuthority("APP_MANAGER", "ROLE_ADMIN", "ROLE_SRADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/user-access-locations/*/locations/*")
                        .hasAnyAuthority("APP_MANAGER", "ROLE_ADMIN", "ROLE_SRADMIN")

                        // Global user administration is never available to ordinary managers/members.
                        .requestMatchers(
                                "/users/all",
                                "/users/history",
                                "/users/delete/*",
                                "/users/update/*",
                                "/users/create",
                                "/users/*/active",
                                "/users/*/accessRole",
                                "/users/*/appRole"
                        ).hasAnyRole("ADMIN", "SRADMIN")

                        // Restricted employee tokens are accepted only for employee line-check actions.
                        .requestMatchers(HttpMethod.POST, "/line-checks/create", "/line-checks/save")
                        .hasAnyAuthority("PIN_LINE_CHECK", "APP_MEMBER", "APP_MANAGER", "APP_CONTRIBUTOR")

                        // Only managers may invite users.
                        .requestMatchers(HttpMethod.POST, "/users/invite")
                        .hasAuthority("APP_MANAGER")

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

                        // Item criterion templates are configured on the website by managers.
                        .requestMatchers(
                                HttpMethod.POST,
                                "/items/*/criteria/**"
                        ).hasAuthority("APP_MANAGER")
                        .requestMatchers(
                                HttpMethod.PUT,
                                "/items/*/criteria/**"
                        ).hasAuthority("APP_MANAGER")
                        .requestMatchers(
                                HttpMethod.PATCH,
                                "/items/*/criteria/**"
                        ).hasAuthority("APP_MANAGER")
                        .requestMatchers(
                                HttpMethod.DELETE,
                                "/items/*/criteria/**"
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
        http.addFilterBefore(pinActionTokenFilter, JwtAuthenticationFilter.class);
        http.addFilterBefore(deviceAuthenticationFilter, PinActionTokenFilter.class);




        return http.build();
    }

    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter> disableJwtContainerRegistration(
            JwtAuthenticationFilter filter
    ) {
        return disabledRegistration(filter);
    }

    @Bean
    public FilterRegistrationBean<PinActionTokenFilter> disablePinTokenContainerRegistration(
            PinActionTokenFilter filter
    ) {
        return disabledRegistration(filter);
    }

    @Bean
    public FilterRegistrationBean<DeviceAuthenticationFilter> disableDeviceContainerRegistration(
            DeviceAuthenticationFilter filter
    ) {
        return disabledRegistration(filter);
    }

    private static <T extends jakarta.servlet.Filter> FilterRegistrationBean<T> disabledRegistration(T filter) {
        FilterRegistrationBean<T> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }
}
