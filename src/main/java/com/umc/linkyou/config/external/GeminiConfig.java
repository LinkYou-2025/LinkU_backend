package com.umc.linkyou.config.external;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.genai.Client;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.io.IOException;

@Slf4j
@Configuration
public class GeminiConfig {

    @Bean(destroyMethod = "close")
    public Client geminiClient(
            @Value("${spring.cloud.gcp.project-id}") String projectId,
            @Value("${spring.cloud.gcp.location}") String location,
            @Value("${gemini.credentials.path}") Resource credentialsResource
    ) {
        try {
            // JSON 파일에서 직접 credentials 로드
            GoogleCredentials credentials = GoogleCredentials
                    .fromStream(credentialsResource.getInputStream());

            Client client = Client.builder()
                    .vertexAI(true)
                    .project(projectId)
                    .location(location)
                    .credentials(credentials)
                    .build();

            log.info("Gemini Client 초기화 완료 (Project: {}, Location: {})", projectId, location);
            return client;
        } catch (IOException e) {
            log.error("Gemini Client 초기화 실패: credentials 파일을 확인하세요. 경로: {}",
                    credentialsResource.getDescription(), e);
            throw new RuntimeException("GCP 인증 파일 로드 실패", e);
        }
    }
}
