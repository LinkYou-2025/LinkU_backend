package com.umc.linkyou.config.external;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.IOException;
import java.io.InputStream;

@Slf4j
@Configuration
public class FcmConfig {

    @Value("${fcm.credentials.path:classpath:linku-firebase-adminsdk.json}")
    private String firebaseCredentialsPath;

    private final ResourceLoader resourceLoader;

    public FcmConfig(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @Bean(destroyMethod = "")
    public FirebaseApp firebaseApp() throws IOException {
        Resource resource = resourceLoader.getResource(firebaseCredentialsPath);
        if (!resource.exists() || resource.contentLength() == 0) {
            log.warn("Firebase credentials file not found or empty — FCM disabled: {}", firebaseCredentialsPath);
            return null;
        }
        try {
            return FirebaseApp.getInstance();
        } catch (IllegalStateException ignored) {
            // 기본 앱이 없으므로 초기화
        }
        try (InputStream inputStream = resource.getInputStream()) {
            GoogleCredentials credentials = GoogleCredentials.fromStream(inputStream);
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(credentials)
                    .build();
            return FirebaseApp.initializeApp(options);
        }
    }

    @Bean
    public FirebaseMessaging firebaseMessaging(ObjectProvider<FirebaseApp> firebaseAppProvider) {
        FirebaseApp firebaseApp = firebaseAppProvider.getIfAvailable();
        if (firebaseApp == null) {
            return null;
        }
        return FirebaseMessaging.getInstance(firebaseApp);
    }
}
