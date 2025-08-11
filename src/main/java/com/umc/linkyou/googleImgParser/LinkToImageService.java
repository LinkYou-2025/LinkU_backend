package com.umc.linkyou.googleImgParser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.beans.factory.annotation.Value;

@Service
public class LinkToImageService {
    @Value("${custom.search.api.key}")
    private String apiKey;

    @Value("${custom.search.engine.id}")
    private String searchEngineId;

    // 1. 링크에서 제목 크롤링
    public String extractTitle(String url) {
        try {
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0")
                    .get();
            String ogTitle = doc.select("meta[property=og:title]").attr("content");
            if (ogTitle != null && !ogTitle.isEmpty())
                return ogTitle.replaceAll("[^ㄱ-ㅎㅏ-ㅣ가-힣a-zA-Z0-9\\s]", "");  // 특수문자 모두 삭제
            return doc.title().replaceAll("[^ㄱ-ㅎㅏ-ㅣ가-힣a-zA-Z0-9\\s]", "");  // 특수문자 모두 삭제
        } catch (Exception e) {
            return null;
        }
    }

    // 2. 진짜 이미지 URL인지 체크 -> 뽑아서 반환!
    public String searchFirstDirectImageUrl(String query) {
        final int MAX_TRY = 5; // 최대 5개까지만 검사
        try {
            String url = "https://www.googleapis.com/customsearch/v1?"
                    + "key=" + apiKey
                    + "&cx=" + searchEngineId
                    + "&searchType=image"
                    + "&q=" + java.net.URLEncoder.encode(query, "UTF-8");
            RestTemplate restTemplate = new RestTemplate();
            String response = restTemplate.getForObject(url, String.class);

            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response);
            JsonNode items = root.get("items");
            if (items != null && items.isArray()) {
                for (int i = 0; i < items.size() && i < MAX_TRY; i++) {
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

    // 이미지 파일 URL(확장자 기반)인지 판별
    private boolean isImageUrl(String url) {
        String lower = url.toLowerCase();
        // jpg, jpeg, png, gif, webp 등 일반적인 확장자
        return lower.endsWith(".jpg") ||
                lower.endsWith(".jpeg") ||
                lower.endsWith(".png") ||
                lower.endsWith(".gif") ||
                lower.endsWith(".webp");
    }

    // 도메인 추출 (title 없을 때 보조 키워드로 사용)
    private String extractDomainFromUrl(String url) {
        try {
            java.net.URI uri = new java.net.URI(url);
            String host = uri.getHost();
            if (host == null) return null;
            return host.startsWith("www.") ? host.substring(4) : host;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 전체 플로우
     */
    public String getRelatedImageFromUrl(String url, String title) {
        if (title != null && !title.isEmpty()) {
            String img = searchFirstDirectImageUrl(title);
            if (img != null) return img;
        }

        // 제목 없거나 이미지 결과 못 찾음 → 도메인으로도 한 번 검색
        String domain = extractDomainFromUrl(url);
        if (domain != null && !domain.isEmpty()) {
            String img = searchFirstDirectImageUrl(domain);
            if (img != null) return img;
        }

        // 마지막까지 실패 시 null 반환
        return null;
    }

    public String getRelatedImageFromUrl(String url) {
        String title = extractTitle(url);
        return getRelatedImageFromUrl(url, title);
    }

}
