package com.backend.backend.config;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Collections;

@Component
public class GoogleTokenVerifier {

    @Value("${google.web.client.id}")
    private String googleClientId;

    private final GoogleIdTokenVerifier verifier;

    public GoogleTokenVerifier(
            @Value("${google.web.client.id}") String webClientId
    ) {
        verifier = new GoogleIdTokenVerifier.Builder(
                new NetHttpTransport(),
                new JacksonFactory()
        )
                .setAudience(Collections.singletonList(webClientId))
                .build();
    }


    public GoogleIdToken.Payload verify(String idTokenString) throws Exception {
        GoogleIdToken idToken = verifier.verify(idTokenString);
        if (idToken == null) throw new Exception("Invalid ID token");
        return idToken.getPayload();
    }
}


