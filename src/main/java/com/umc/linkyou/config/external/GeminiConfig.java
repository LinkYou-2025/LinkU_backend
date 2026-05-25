package com.umc.linkyou.config.external;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.genai.Client;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;

@Slf4j
@Configuration
public class GeminiConfig {

    @Bean(destroyMethod = "close")
    public Client vertexAiClient(
            @Value("${spring.cloud.gcp.project-id}") String projectId,
            @Value("${spring.cloud.gcp.location}") String location,
            @Value("${gemini.credentials.path}") Resource credentialsResource
    ) {
        try (InputStream is = credentialsResource.getInputStream()) {
            // Vertex AI 용 최소 요구 스코프 설정
            GoogleCredentials credentials = GoogleCredentials.fromStream(is)
                    .createScoped(Collections.singletonList("https://www.googleapis.com/auth/cloud-platform"));

            Client client = Client.builder()
                    .vertexAI(true)
                    .project(projectId)
                    .location(location)
                    .credentials(credentials)
                    .build();

            log.info("Gemini Client 초기화 성공: {}", credentialsResource.getFilename());
            return client;
        } catch (IOException e) {
            log.error("Gemini 인증 파일 로드 실패: {}", credentialsResource.getDescription());
            throw new RuntimeException(e);
        }
    }
}
