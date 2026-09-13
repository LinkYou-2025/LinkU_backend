package com.umc.linkyou.infra.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
public class CustomSearchImageClient {

    private static final int MAX_IMAGE_SEARCH_TRY = 5;
    private static final int HTTP_CONNECT_TIMEOUT_MS = 3000;
    private static final int HTTP_READ_TIMEOUT_MS = 5000;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final RestClient restClient = RestClient.builder()
            .requestFactory(createTimeoutRequestFactory())
            .build();

    // 타임아웃을 설정한 RestClient용 RequestFactory 생성,
    private static SimpleClientHttpRequestFactory createTimeoutRequestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(HTTP_CONNECT_TIMEOUT_MS);
        factory.setReadTimeout(HTTP_READ_TIMEOUT_MS);
        return factory;
    }

    @Value("${custom.search.api.key}")
    private String apiKey;
    @Value("${custom.search.engine.id}")
    private String searchEngineId;

    // Google Custom Search API 특정 이미지 직접 검색
    @CircuitBreaker(name = "customSearch", fallbackMethod = "searchFirstDirectImageUrlFallback")
    public String searchFirstDirectImageUrl(String query) {
        try {
            String url = "https://www.googleapis.com/customsearch/v1?"
                    + "key=" + apiKey
                    + "&cx=" + searchEngineId
                    + "&searchType=image"
                    + "&q=" + java.net.URLEncoder.encode(query, "UTF-8");

            String response = restClient.get().uri(url).retrieve().body(String.class);

            JsonNode root = objectMapper.readTree(response);
            JsonNode items = root.get("items");
            if (items != null && items.isArray()) {
                for (int i = 0; i < items.size() && i < MAX_IMAGE_SEARCH_TRY; i++) {
                    String link = items.get(i).get("link").asText();
                    if (isImageUrl(link)) {
                        return link;
                    }
                }
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }

    private String searchFirstDirectImageUrlFallback(String query, Throwable t) {
        log.warn("[CUSTOM_SEARCH] 서킷 OPEN 또는 호출 실패로 이미지 검색 건너뜀 query={}", query, t);
        return null;
    }

    private boolean isImageUrl(String url) {
        //CDN 등에서 내려오는 이미지는 "photo.jpg?w=800"처럼 쿼리스트링이 붙는 경우가 많아 확장자 검사 전에 쿼리스트링 분리
        String path = url.split("\\?", 2)[0].toLowerCase();
        return path.endsWith(".jpg") ||
                path.endsWith(".jpeg") ||
                path.endsWith(".png") ||
                path.endsWith(".gif") ||
                path.endsWith(".webp");
    }
}
