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

import java.lang.reflect.Proxy;

import static org.assertj.core.api.Assertions.assertThat;

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
                new MockHttpServletRequest(),
                response
        );

        assertThat(response.getRedirectedUrl())
                .isEqualTo("https://frontend.example/unauthorized");
        assertThat(response.getHeader("Set-Cookie")).isNull();
    }
}
