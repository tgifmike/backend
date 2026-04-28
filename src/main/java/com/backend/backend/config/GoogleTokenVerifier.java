package com.backend.backend.config;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class GoogleTokenVerifier {

    private static final String webClientId =
            System.getenv("GOOGLE_WEB_CLIENT_ID");

    private static final String iosClientId =
            System.getenv("GOOGLE_IOS_CLIENT_ID");

    public static GoogleIdToken.Payload verifyToken(String idTokenString)
            throws Exception {

        GoogleIdTokenVerifier verifier =
                new GoogleIdTokenVerifier.Builder(
                        new NetHttpTransport(),
                        new GsonFactory()
                )
                        .setAudience(List.of(
                                webClientId,
                                iosClientId
                        ))
                        .build();

        GoogleIdToken idToken =
                verifier.verify(idTokenString);

        if (idToken == null) {
            throw new IllegalArgumentException("Invalid Google ID token");
        }

        return idToken.getPayload();
    }
}

