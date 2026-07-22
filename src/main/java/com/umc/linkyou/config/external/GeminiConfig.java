package com.umc.linkyou.config.external;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.genai.Client;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;
import java.util.Collections;

@Slf4j
@Configuration
public class GeminiConfig {

    @Bean(destroyMethod = "close")
    @ConditionalOnMissingBean(Client.class)
    public Client vertexAiClient(
            @Value("${spring.cloud.gcp.project-id}") String projectId,
            @Value("${spring.cloud.gcp.location}") String location,
            @Value("${GCP_CREDENTIALS_JSON:}") String credentialsBase64
    ) throws IOException {
        if (credentialsBase64 == null || credentialsBase64.isBlank()) {
            log.warn("GCP credentials not configured — Gemini Client disabled");
            return null;
        }
        byte[] decoded = Base64.getDecoder().decode(credentialsBase64);
        try (InputStream is = new ByteArrayInputStream(decoded)) {
            GoogleCredentials credentials = GoogleCredentials.fromStream(is)
                    .createScoped(Collections.singletonList("https://www.googleapis.com/auth/cloud-platform"));

            Client client = Client.builder()
                    .vertexAI(true)
                    .project(projectId)
                    .location(location)
                    .credentials(credentials)
                    .build();

            log.info("Gemini Client 초기화 성공");
            return client;
        }
    }
}
