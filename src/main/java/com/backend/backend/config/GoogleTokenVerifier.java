package com.backend.backend.config;

//import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
//import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
//import com.google.api.client.http.javanet.NetHttpTransport;
//import com.google.api.client.json.jackson2.JacksonFactory;
//import jakarta.annotation.PostConstruct;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.stereotype.Component;
//
//import java.util.Collections;
//
//@Component
//public class GoogleTokenVerifier {
//
//    @Value("${google.web.client.id}")
//    private String googleClientId;
//
//    private GoogleIdTokenVerifier verifier;
//
//    @PostConstruct
//    private void init() {
//        verifier = new GoogleIdTokenVerifier.Builder(
//                new NetHttpTransport(),
//                new JacksonFactory()
//        )
//                .setAudience(Collections.singletonList(googleClientId))
//                .build();
//    }
//
//    public GoogleIdToken.Payload verify(String idTokenString) throws Exception {
//        GoogleIdToken idToken = verifier.verify(idTokenString);
//        if (idToken == null) {
//            System.out.println("Token verification failed. Raw token: " + idTokenString);
//            throw new Exception("Invalid ID token");
//        }
//        GoogleIdToken.Payload payload = idToken.getPayload();
//        System.out.println("Token verified for user: " + payload.getEmail() + ", aud: " + payload.getAudience());
//        return payload;
//    }
//}
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;

@Component
public class GoogleTokenVerifier {

    private static final String clientId = System.getenv("GOOGLE_WEB_CLIENT_ID");



    public static GoogleIdToken.Payload verifyToken(String idTokenString) throws Exception {
        GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
                .setAudience(Collections.singletonList(clientId))
                .build();

        GoogleIdToken idToken = verifier.verify(idTokenString);




        if (idToken != null) {
            GoogleIdToken.Payload payload = idToken.getPayload();
//            System.out.println("✅ Token verified successfully!");
//            System.out.println("User ID: " + payload.getSubject());
//            System.out.println("Email: " + payload.getEmail());
            return payload;
        } else {
            throw new IllegalArgumentException("❌ Invalid ID token");
        }
    }
}

