package com.backend.backend.controller;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.backend.backend.dto.LoginResponse;
import com.backend.backend.dto.apple.AppleTokenResponse;
import com.backend.backend.dto.google.GoogleTokenResponse;
import com.backend.backend.dto.google.GoogleUserInfo;
import com.backend.backend.entity.UserEntity;
import com.backend.backend.service.UserService;
import com.backend.backend.service.apple.AppleOAuthService;
import com.backend.backend.service.google.GoogleOAuthService;
import com.backend.backend.service.google.GoogleUserInfoService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;


import java.io.IOException;
import java.net.URLEncoder;


import static java.nio.charset.StandardCharsets.UTF_8;

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

    @Value("${apple.client.id}")
    private String appleClientId;

    @Value("${apple.redirect.uri}")
    private String appleRedirectUri;

    @Value("${frontend.redirect.url}")
    private String frontendRedirectUrl;

    @Value("${app.cookie.secure}")
    private boolean cookieSecure;

    private final GoogleOAuthService googleOAuthService;
    private final GoogleUserInfoService googleUserInfoService;
    private final UserService userService;
    private final AppleOAuthService appleOAuthService;

    public AuthController(

            GoogleOAuthService googleOAuthService,
            GoogleUserInfoService googleUserInfoService,
            UserService userService,
            AppleOAuthService appleOAuthService

    ) {

        this.googleOAuthService = googleOAuthService;
        this.googleUserInfoService = googleUserInfoService;
        this.userService = userService;
        this.appleOAuthService = appleOAuthService;
    }


    @GetMapping("/google/login")
    public void redirectToGoogle(HttpServletResponse response) throws IOException {

        String scope = "openid email profile";

        String url =
                "https://accounts.google.com/o/oauth2/v2/auth"
                        + "?client_id=" + clientId
                        + "&redirect_uri=" + URLEncoder.encode(redirectUri, UTF_8)
                        + "&response_type=code"
                        + "&scope=" + URLEncoder.encode(scope, UTF_8)
                        + "&access_type=offline"
                        + "&prompt=consent";

        response.sendRedirect(url);
    }



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
        ResponseCookie cookie = ResponseCookie.from("accessToken", login.token())
                .httpOnly(true)
                .secure(true)
                .sameSite("None")
                .path("/")
//                .domain("api.themanagerlife.com")
                .maxAge(60 * 60 * 24)
                .build();

        response.setHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        // ✅ Redirect without exposing token
        response.sendRedirect(frontendRedirectUrl + "/dashboard");
    }

    @GetMapping("/apple/login")
    public void redirectToApple(HttpServletResponse response) throws IOException {

        String url =
                "https://appleid.apple.com/auth/authorize"
                        + "?client_id=" + appleClientId
                        + "&redirect_uri=" + URLEncoder.encode(appleRedirectUri, UTF_8)
                        + "&response_type=code"
                        + "&response_mode=form_post"
                        + "&scope=name email";

        response.sendRedirect(url);
        System.out.println("APPLE TOKEN REDIRECT URI = " + appleRedirectUri);
    }

    @PostMapping("/apple/callback")
    public void appleCallback(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {

        System.out.println("🍎 APPLE CALLBACK CONTROLLER HIT");
        System.out.println("METHOD = " + request.getMethod());
        System.out.println("CONTENT TYPE = " + request.getContentType());

        try {

            // Apple sends form POST params
            String code = request.getParameter("code");
            String state = request.getParameter("state");
            String userParam = request.getParameter("user");

            System.out.println("APPLE CODE PRESENT = " + (code != null));
            System.out.println("APPLE STATE = " + state);
            System.out.println("APPLE USER PARAM = " + userParam);

            if (code == null || code.isBlank()) {
                System.out.println("❌ Apple callback missing code");
                response.sendError(
                        HttpServletResponse.SC_BAD_REQUEST,
                        "Missing Apple authorization code"
                );
                return;
            }

            // Exchange code for tokens
            AppleTokenResponse tokenResponse =
                    appleOAuthService.exchangeCodeForToken(code);

            if (tokenResponse == null) {
                System.out.println("❌ Apple token response null");
                response.sendError(
                        HttpServletResponse.SC_UNAUTHORIZED,
                        "Apple token exchange failed"
                );
                return;
            }

            String idToken = tokenResponse.getIdToken();

            if (idToken == null || idToken.isBlank()) {
                System.out.println("❌ Apple ID token missing");
                response.sendError(
                        HttpServletResponse.SC_UNAUTHORIZED,
                        "Missing Apple ID token"
                );
                return;
            }

            // Decode identity token
            DecodedJWT jwt = JWT.decode(idToken);

            String appleId = jwt.getSubject();
            String email = jwt.getClaim("email").asString();

            if (appleId == null || appleId.isBlank()) {
                System.out.println("❌ Apple subject missing");
                response.sendError(
                        HttpServletResponse.SC_UNAUTHORIZED,
                        "Invalid Apple identity token"
                );
                return;
            }

            // Apple often only sends email first login
            if (email == null || email.isBlank()) {
                email = appleId + "@apple.local";
            }

            System.out.println("APPLE USER ID = " + appleId);
            System.out.println("APPLE EMAIL = " + email);

            // Create / login user
            UserEntity user = new UserEntity();
            user.setAppleId(appleId);
            user.setUserEmail(email);

            LoginResponse login = userService.handleOAuthLogin(user);

            // Auth cookie
            ResponseCookie cookie = ResponseCookie.from("accessToken", login.token())
                    .httpOnly(true)
                    .secure(true)
                    .sameSite("None")
                    .path("/")
//                    .domain("api.themanagerlife.com")
                    .maxAge(60 * 60 * 24)
                    .build();

            response.setHeader(HttpHeaders.SET_COOKIE, cookie.toString());

            System.out.println("✅ Apple login success");
            System.out.println("REDIRECTING TO = " + frontendRedirectUrl + "/dashboard");

            response.sendRedirect(frontendRedirectUrl + "/dashboard");

        } catch (Exception e) {

            e.printStackTrace();

            response.sendError(
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Apple login failed"
            );
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse response) {

        ResponseCookie cookie = ResponseCookie.from("accessToken", "")
                .httpOnly(true)
                .secure(true)
                .sameSite("None")
                .path("/")
//                .domain("api.themanagerlife.com")
                .maxAge(0)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .build();
    }

}
