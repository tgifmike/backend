package com.backend.backend.controller;

import com.backend.backend.dto.LoginResponse;
import com.backend.backend.dto.google.GoogleTokenResponse;
import com.backend.backend.dto.google.GoogleUserInfo;
import com.backend.backend.entity.UserEntity;
import com.backend.backend.service.UserService;
import com.backend.backend.service.google.GoogleOAuthService;
import com.backend.backend.service.google.GoogleUserInfoService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@CrossOrigin(origins = {
        "http://localhost:3000",
        "https://www.themanagerlife.com"
})
@RestController
@RequestMapping("/auth")
public class AuthController {

    @Value("${google.client.id}")
    private String clientId;
    @Value("${google.redirect.uri}")
    private String redirectUri;

    @Value("${frontend.redirect.url}")
    private String frontendRedirectUrl;

    private final GoogleOAuthService googleOAuthService;
    private final GoogleUserInfoService googleUserInfoService;
    private final UserService userService;

    public AuthController(

            GoogleOAuthService googleOAuthService,
            GoogleUserInfoService googleUserInfoService,
            UserService userService
    ) {

        this.googleOAuthService = googleOAuthService;
        this.googleUserInfoService = googleUserInfoService;
        this.userService = userService;
    }


    @GetMapping("/google/login")
    public void redirectToGoogle(HttpServletResponse response) throws IOException {

        String scope = "openid email profile";

        String url =
                "https://accounts.google.com/o/oauth2/v2/auth"
                        + "?client_id=" + clientId
                        + "&redirect_uri=" + URLEncoder.encode(redirectUri, StandardCharsets.UTF_8)
                        + "&response_type=code"
                        + "&scope=" + URLEncoder.encode(scope, StandardCharsets.UTF_8)
                        + "&access_type=offline"
                        + "&prompt=consent";

        response.sendRedirect(url);
    }

    @GetMapping("/google/callback")
    public void googleCallback(
            @RequestParam String code,
            HttpServletResponse response
    ) throws IOException {

        GoogleTokenResponse token =
                googleOAuthService.exchangeCodeForToken(code);

        GoogleUserInfo userInfo =
                googleUserInfoService.fetchUserInfo(token.getAccessToken());

        UserEntity incoming = new UserEntity();
        incoming.setUserEmail(userInfo.getEmail());
        incoming.setUserName(userInfo.getName());
        incoming.setGoogleId(userInfo.getSub());
        incoming.setUserImage(userInfo.getPicture());

        LoginResponse login =
                userService.handleOAuthLogin(incoming);

        response.sendRedirect(
                frontendRedirectUrl + "/auth/callback?token=" + login.token()
        );
    }

    @GetMapping("/ping")
    public String ping() {
        return "alive";
    }
}
