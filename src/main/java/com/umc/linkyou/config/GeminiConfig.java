package com.umc.linkyou.config;

import com.google.genai.Client;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class GeminiConfig {

    @Bean(destroyMethod = "close")
    public Client geminiClient(
            @Value("${spring.cloud.gcp.project-id}") String projectId,
            @Value("${spring.cloud.gcp.location}") String location
    ) {
        try {
            Client client = Client.builder()
                    .vertexAI(true)
                    .project(projectId)
                    .location(location)
                    .build();
            log.info("Gemini Client 초기화 완료 (Project: {}, Location: {})", projectId, location);
            return client;
        } catch (Exception e) {
            log.error("Gemini Client 초기화 실패: GCP 인증 파일이나 설정을 확인하세요.", e);
            throw new RuntimeException(e);
        }
    }
}
