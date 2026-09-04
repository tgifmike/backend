package com.backend.backend.controller;

import com.backend.backend.config.AppleIdTokenVerifier;
import com.backend.backend.dto.LoginResponse;
import com.backend.backend.dto.apple.AppleTokenResponse;
import com.backend.backend.dto.google.GoogleTokenResponse;
import com.backend.backend.dto.google.GoogleUserInfo;
import com.backend.backend.entity.UserEntity;
import com.backend.backend.exception.OAuthUserNotRegisteredException;
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
    private final AppleIdTokenVerifier appleIdTokenVerifier;

    public AuthController(

            GoogleOAuthService googleOAuthService,
            GoogleUserInfoService googleUserInfoService,
            UserService userService,
            AppleOAuthService appleOAuthService,
            AppleIdTokenVerifier appleIdTokenVerifier

    ) {

        this.googleOAuthService = googleOAuthService;
        this.googleUserInfoService = googleUserInfoService;
        this.userService = userService;
        this.appleOAuthService = appleOAuthService;
        this.appleIdTokenVerifier = appleIdTokenVerifier;
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

        LoginResponse login;

        try {
            login = userService.handleOAuthLogin(incoming);
        } catch (OAuthUserNotRegisteredException ex) {
            response.sendRedirect(frontendRedirectUrl + "/unauthorized");
            return;
        }


        ResponseCookie cookie = createAccessTokenCookie(
                login.token(),
                60 * 60 * 24,
                request
        );

        response.setHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        // ✅ Redirect without exposing token
        String destination = frontendRedirectUrl + "/dashboard"
                + (login.firstLogin() ? "?welcome=1" : "");

        response.sendRedirect(destination);
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
    }

    @PostMapping("/apple/callback")
    public void appleCallback(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {

        try {

            // Apple sends form POST params
            String code = request.getParameter("code");

            if (code == null || code.isBlank()) {
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
                response.sendError(
                        HttpServletResponse.SC_UNAUTHORIZED,
                        "Apple token exchange failed"
                );
                return;
            }

            String idToken = tokenResponse.getIdToken();

            if (idToken == null || idToken.isBlank()) {
                response.sendError(
                        HttpServletResponse.SC_UNAUTHORIZED,
                        "Missing Apple ID token"
                );
                return;
            }

            var claims = appleIdTokenVerifier.verify(idToken);
            String appleId = claims.getSubject();
            String email = (String) claims.getClaim("email");

            if (appleId == null || appleId.isBlank()) {
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

            // Create / login user
            UserEntity user = new UserEntity();
            user.setAppleId(appleId);
            user.setUserEmail(email);

            LoginResponse login = userService.handleOAuthLogin(user);

            ResponseCookie cookie = createAccessTokenCookie(
                    login.token(),
                    60 * 60 * 24,
                    request
            );

            response.setHeader(HttpHeaders.SET_COOKIE, cookie.toString());

            String destination = frontendRedirectUrl + "/dashboard"
                    + (login.firstLogin() ? "?welcome=1" : "");

            response.sendRedirect(destination);

        } catch (OAuthUserNotRegisteredException e) {

            response.sendRedirect(frontendRedirectUrl + "/unauthorized");

        } catch (Exception e) {

            response.sendError(
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Apple login failed"
            );
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            HttpServletRequest request,
            HttpServletResponse response
    ) {

        ResponseCookie cookie = createAccessTokenCookie("", 0, request);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .build();
    }

    private ResponseCookie createAccessTokenCookie(
            String value,
            long maxAge,
            HttpServletRequest request
    ) {

        boolean localRequest = isLocalRequest(request);
        boolean secure = localRequest ? cookieSecure : true;

        ResponseCookie.ResponseCookieBuilder cookie =
                ResponseCookie.from("accessToken", value)
                        .httpOnly(true)
                        .secure(secure)
                        .sameSite(secure ? "None" : "Lax")
                        .path("/")
                        .maxAge(maxAge);

        // A localhost response cannot set a cookie for the production domain.
        if (!localRequest) {
            cookie.domain(".themanagerlife.com");
        }

        return cookie.build();
    }

    private boolean isLocalRequest(HttpServletRequest request) {

        String host = request.getServerName();

        return "localhost".equalsIgnoreCase(host)
                || "127.0.0.1".equals(host)
                || "::1".equals(host)
                || "[::1]".equals(host);
    }

}
