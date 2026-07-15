package com.umc.linkyou.config.external;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;

@Slf4j
@Configuration
public class FcmConfig {

    @Value("${FIREBASE_CREDENTIALS_JSON:}")
    private String credentialsBase64;

    @Bean(destroyMethod = "")
    @ConditionalOnMissingBean(FirebaseApp.class)
    public FirebaseApp firebaseApp() throws IOException {
        if (credentialsBase64 == null || credentialsBase64.isBlank()) {
            log.warn("Firebase credentials not configured — FCM disabled");
            return null;
        }
        try {
            return FirebaseApp.getInstance();
        } catch (IllegalStateException ignored) {
            // 기본 앱이 없으므로 초기화
        }
        byte[] decoded = Base64.getDecoder().decode(credentialsBase64);
        try (InputStream is = new ByteArrayInputStream(decoded)) {
            GoogleCredentials credentials = GoogleCredentials.fromStream(is);
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(credentials)
                    .build();
            return FirebaseApp.initializeApp(options);
        }
    }

    @Bean
    @ConditionalOnBean(FirebaseApp.class)
    @ConditionalOnMissingBean(FirebaseMessaging.class)
    public FirebaseMessaging firebaseMessaging(FirebaseApp firebaseApp) {
        return FirebaseMessaging.getInstance(firebaseApp);
    }
}
