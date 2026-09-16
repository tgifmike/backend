package com.backend.backend.controller;

import com.backend.backend.dto.google.GoogleTokenResponse;
import com.backend.backend.dto.google.GoogleUserInfo;
import com.backend.backend.exception.OAuthUserNotRegisteredException;
import com.backend.backend.service.UserService;
import com.backend.backend.service.google.GoogleOAuthService;
import com.backend.backend.service.google.GoogleUserInfoService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.lang.reflect.Proxy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerTest {

    @Test
    void googleCallbackRedirectsUnregisteredUserToUnauthorizedPage() throws Exception {
        GoogleTokenResponse token = new GoogleTokenResponse();
        token.setAccessToken("google-access-token");

        GoogleUserInfo userInfo = GoogleUserInfo.builder()
                .sub("google-user-id")
                .email("not-registered@example.com")
                .name("Not Registered")
                .build();

        GoogleOAuthService googleOAuthService = new GoogleOAuthService() {
            @Override
            public GoogleTokenResponse exchangeCodeForToken(String code) {
                return token;
            }
        };
        GoogleUserInfoService googleUserInfoService = new GoogleUserInfoService() {
            @Override
            public GoogleUserInfo fetchUserInfo(String accessToken) {
                return userInfo;
            }
        };
        UserService userService = (UserService) Proxy.newProxyInstance(
                UserService.class.getClassLoader(),
                new Class<?>[]{UserService.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("handleOAuthLogin")) {
                        throw new OAuthUserNotRegisteredException();
                    }
                    throw new UnsupportedOperationException(method.getName());
                }
        );

        AuthController controller = new AuthController(
                googleOAuthService,
                googleUserInfoService,
                userService,
                null,
                null
        );
        ReflectionTestUtils.setField(
                controller,
                "frontendRedirectUrl",
                "https://frontend.example"
        );

        MockHttpServletResponse response = new MockHttpServletResponse();

        controller.googleCallback(
                "oauth-code",
                null,
                null,
                new MockHttpServletRequest(),
                response
        );

        assertThat(response.getRedirectedUrl())
                .isEqualTo("https://frontend.example/unauthorized");
        assertThat(response.getHeader("Set-Cookie")).isNull();
    }

    @Test
    void googleCallbackRedirectsCancelledLoginToLoginPage() throws Exception {
        AuthController controller = controllerWithFailingOAuthServices();
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        mockMvc.perform(get("/auth/google/callback")
                        .param("error", "access_denied")
                        .param("state", "oauth-state"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl(
                        "https://frontend.example/login?authError=cancelled"
                ));
    }

    @Test
    void googleCallbackRedirectsInvalidOAuthResponseToLoginPage() throws Exception {
        AuthController controller = controllerWithFailingOAuthServices();
        MockHttpServletResponse response = new MockHttpServletResponse();

        controller.googleCallback(
                null,
                null,
                null,
                new MockHttpServletRequest(),
                response
        );

        assertThat(response.getStatus()).isEqualTo(302);
        assertThat(response.getRedirectedUrl())
                .isEqualTo("https://frontend.example/login?authError=oauth_failed");
    }

    private AuthController controllerWithFailingOAuthServices() {
        GoogleOAuthService googleOAuthService = new GoogleOAuthService() {
            @Override
            public GoogleTokenResponse exchangeCodeForToken(String code) {
                throw new AssertionError("OAuth token exchange must not be called");
            }
        };
        GoogleUserInfoService googleUserInfoService = new GoogleUserInfoService() {
            @Override
            public GoogleUserInfo fetchUserInfo(String accessToken) {
                throw new AssertionError("Google user info must not be fetched");
            }
        };

        AuthController controller = new AuthController(
                googleOAuthService,
                googleUserInfoService,
                null,
                null,
                null
        );
        ReflectionTestUtils.setField(
                controller,
                "frontendRedirectUrl",
                "https://frontend.example"
        );
        return controller;
    }
}
