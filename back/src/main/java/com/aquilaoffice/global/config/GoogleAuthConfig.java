package com.aquilaoffice.global.config;

import com.aquilaoffice.global.config.properties.GoogleOAuthProperties;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import java.io.IOException;
import java.security.GeneralSecurityException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Google ID token verifier를 Google 공개키 cache가 가능한 singleton bean으로 구성한다.
 */
@Configuration
public class GoogleAuthConfig {

    @Bean
    public GoogleIdTokenVerifier googleIdTokenVerifier(GoogleOAuthProperties googleOAuthProperties)
            throws GeneralSecurityException, IOException {
        return new GoogleIdTokenVerifier.Builder(
                        GoogleNetHttpTransport.newTrustedTransport(),
                        GsonFactory.getDefaultInstance())
                .setAudience(googleOAuthProperties.clientIds())
                .build();
    }
}
