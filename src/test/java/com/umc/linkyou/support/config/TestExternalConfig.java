package com.umc.linkyou.support.config;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.genai.Client;
import com.umc.linkyou.jwt.RefreshTokenManager;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class TestExternalConfig {

    @Bean
    public Client vertexAiClient() {
        return Mockito.mock(Client.class);
    }

    @Bean
    public FirebaseApp firebaseApp() {
        return Mockito.mock(FirebaseApp.class);
    }

    @Bean
    public FirebaseMessaging firebaseMessaging() {
        return Mockito.mock(FirebaseMessaging.class);
    }

    @Bean
    @Primary
    public RefreshTokenManager refreshTokenManager() {
        return Mockito.mock(RefreshTokenManager.class);
    }
}
