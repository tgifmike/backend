package com.backend.backend.config;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.springframework.test.util.ReflectionTestUtils;

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
                .containsExactly(
                        "Content-Type", "Authorization", "X-User-Id",
                        "X-Device-Id", "X-Correlation-Id"
                );
        assertThat(configuration.getAllowCredentials()).isTrue();
    }

    @Test
    void jwtFilterRequiresIssuerAndAudience() throws Exception {
        String secret = "oauth-jwt-secret-with-at-least-32-characters";
        JwtConfig config = jwtConfig(secret);
        String wrongAudience = JWT.create()
                .withSubject(UUID.randomUUID().toString())
                .withIssuer("the-manager-life")
                .withAudience("wrong-audience")
                .withExpiresAt(new Date(System.currentTimeMillis() + 60_000))
                .sign(Algorithm.HMAC256(secret));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/users/me");
        request.addHeader("Authorization", "Bearer " + wrongAudience);
        MockHttpServletResponse response = new MockHttpServletResponse();

        new JwtAuthenticationFilter(config).doFilter(request, response, (req, res) -> {
            throw new AssertionError("invalid token must not continue");
        });

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("INVALID_TOKEN");
    }

    @Test
    void jwtFilterDoesNotConvertDownstreamErrorsIntoAuthenticationErrors() {
        String secret = "oauth-jwt-secret-with-at-least-32-characters";
        JwtConfig config = jwtConfig(secret);
        String token = JWT.create()
                .withSubject(UUID.randomUUID().toString())
                .withIssuer("the-manager-life")
                .withAudience("the-manager-life-api")
                .withClaim("accessRole", "USER")
                .withClaim("appRole", "MEMBER")
                .withExpiresAt(new Date(System.currentTimeMillis() + 60_000))
                .sign(Algorithm.HMAC256(secret));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/users/me");
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(config);

        assertThatThrownBy(() -> filter.doFilter(request, response, (req, res) -> {
            throw new ServletException("controller failure");
        })).isInstanceOf(ServletException.class).hasMessage("controller failure");
        assertThat(response.getStatus()).isEqualTo(200);
    }

    private static JwtConfig jwtConfig(String secret) {
        JwtConfig config = new JwtConfig();
        ReflectionTestUtils.setField(config, "secret", secret);
        ReflectionTestUtils.setField(config, "issuer", "the-manager-life");
        ReflectionTestUtils.setField(config, "audience", "the-manager-life-api");
        return config;
    }
}
