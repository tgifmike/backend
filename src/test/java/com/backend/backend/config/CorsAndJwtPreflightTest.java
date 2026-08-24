package com.backend.backend.config;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

class CorsAndJwtPreflightTest {

    @Test
    void optionsRequestPassesThroughJwtFilterWithoutAToken() throws Exception {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(new JwtConfig());
        MockHttpServletRequest request = new MockHttpServletRequest(
                "OPTIONS",
                "/temperature-categories"
        );
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean continued = new AtomicBoolean(false);
        FilterChain filterChain = (req, res) -> continued.set(true);

        filter.doFilter(request, response, filterChain);

        assertThat(continued).isTrue();
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void corsAllowsFrontendPreflightHeadersAndMethods() {
        CorsConfigurationSource source = new CorsConfig().corsConfigurationSource();
        MockHttpServletRequest request = new MockHttpServletRequest(
                "OPTIONS",
                "/temperature-categories"
        );

        CorsConfiguration configuration = source.getCorsConfiguration(request);

        assertThat(configuration).isNotNull();
        assertThat(configuration.getAllowedOrigins())
                .contains("http://localhost:3000");
        assertThat(configuration.getAllowedMethods())
                .containsExactly("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");
        assertThat(configuration.getAllowedHeaders())
                .containsExactly("Content-Type", "Authorization", "X-User-Id");
        assertThat(configuration.getAllowCredentials()).isTrue();
    }
}
