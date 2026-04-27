package com.backend.backend.controller;

import com.backend.backend.dto.LoginResponse;
import com.backend.backend.dto.google.GoogleTokenResponse;
import com.backend.backend.dto.google.GoogleUserInfo;
import com.backend.backend.entity.UserEntity;
import com.backend.backend.service.UserService;
import com.backend.backend.service.google.GoogleOAuthService;
import com.backend.backend.service.google.GoogleUserInfoService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpHeaders;

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

    @Value("${app.cookie.secure}")
    private boolean cookieSecure;

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

//    @GetMapping("/google/callback")
//    public void googleCallback(
//            @RequestParam String code,
//            HttpServletResponse response
//    ) throws IOException {
//
//        GoogleTokenResponse token =
//                googleOAuthService.exchangeCodeForToken(code);
//
//        GoogleUserInfo userInfo =
//                googleUserInfoService.fetchUserInfo(token.getAccessToken());
//
//        UserEntity incoming = new UserEntity();
//        incoming.setUserEmail(userInfo.getEmail());
//        incoming.setUserName(userInfo.getName());
//        incoming.setGoogleId(userInfo.getSub());
//        incoming.setUserImage(userInfo.getPicture());
//
//        LoginResponse login =
//                userService.handleOAuthLogin(incoming);
//
//        response.sendRedirect(
//                frontendRedirectUrl + "/auth/callback?token=" + login.token()
//        );
//
//
//    }

    //new call back with cookies
    @GetMapping("/google/callback")
    public void googleCallback(
            @RequestParam String code,
            HttpServletRequest request,
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


        // ✅ Set HTTP-only cookie
        ResponseCookie cookie = ResponseCookie.from("auth_token", login.token())
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/")
                .sameSite("None")
                .maxAge(60 * 60 * 24)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        // ✅ Redirect without exposing token
        response.sendRedirect(frontendRedirectUrl + "/dashboard");
    }

  @PostMapping("/logout")
public void logout(
        HttpServletRequest request,
        HttpServletResponse response
) {


    ResponseCookie cookie = ResponseCookie.from("auth_token", "")
            .httpOnly(true)
            .secure(cookieSecure)
            .path("/")
            .sameSite("None")
            .maxAge(0)
            .build();

    response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
}

}
