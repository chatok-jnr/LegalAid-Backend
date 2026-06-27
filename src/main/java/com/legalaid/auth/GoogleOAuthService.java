package com.legalaid.auth;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Slf4j
@Service
public class GoogleOAuthService {

    @Value("${app.google.client-id}")
    private String googleClientId;

    // ── Verify the ID token Google sends to the frontend ────
    // Google signs this token — we verify the signature so we
    // know it's genuine and not forged by anyone.
    public GoogleIdToken.Payload verifyToken(String idToken) {
        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(),
                    GsonFactory.getDefaultInstance()
            )
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();

            GoogleIdToken token = verifier.verify(idToken);

            if (token == null) {
                throw new RuntimeException("Invalid Google ID token");
            }

            return token.getPayload();

        } catch (Exception e) {
            log.error("Google token verification failed: {}", e.getMessage());
            throw new RuntimeException("Google token verification failed");
        }
    }
}