package com.umc.linkyou.infra.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.linkyou.infra.net.SafeUrlFetcher;
import com.umc.linkyou.repository.classification.domainRepository.DomainRepository;
import com.umc.linkyou.domain.classification.Domain;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.beans.factory.annotation.Value;

import java.net.URI;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class LinkToImageService {

    private final DomainRepository domainRepository;
    private final SafeUrlFetcher safeUrlFetcher;
    private final RobotsTxtChecker robotsTxtChecker;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final int MAX_IMAGE_SEARCH_TRY = 5;
    private static final int IMAGE_FETCH_TIMEOUT_MS = 5000;
    private static final int HTTP_CONNECT_TIMEOUT_MS = 3000;
    private static final int HTTP_READ_TIMEOUT_MS = 5000;

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

    // URL에서 도메인 추출
    private String extractDomainFromUrl(String url) {
        try {
            URI uri = new URI(url);
            String host = uri.getHost();
            if (host == null) return null;
            return host.startsWith("www.") ? host.substring(4) : host;
        } catch (Exception e) {
            return null;
        }
    }

    // DB 기반 네이버 계열 판별 (패턴 매칭)
    private boolean isNaverFromDB(String url) {
        String domainTail = extractDomainFromUrl(url);
        if (domainTail == null) return false;

        // ".naver.com"으로 끝나는 모든 domain_tail 조회
        List<Domain> naverDomains = domainRepository.findByDomainTailIn(
                List.of(domainTail)
        );
        // 또는 QueryDSL이라면 domainTail.endsWith("naver.com") 조건 가능
        return naverDomains.stream()
                .anyMatch(d -> d.getDomainTail().endsWith("naver.com"));
    }

    // 네이버 블로그 iframe 내부 본문 접근 + 대표 이미지 추출
    private String extractFromNaverBlog(String blogUrl) {
        try {
            if (!robotsTxtChecker.isAllowed(blogUrl, "Mozilla/5.0")) {
                log.warn("[크롤링 제한] robots.txt에 의해 이미지 추출 금지된 URL: {}", blogUrl);
                return null;
            }
            Document doc = safeUrlFetcher.fetchDocument(blogUrl, "Mozilla/5.0", IMAGE_FETCH_TIMEOUT_MS);
            String frameSrc = doc.select("iframe#mainFrame").attr("src");
            if (frameSrc.isEmpty()) return null;

            String realUrl = "https://blog.naver.com" + frameSrc;
            if (!robotsTxtChecker.isAllowed(realUrl, "Mozilla/5.0")) {
                log.warn("[크롤링 제한] robots.txt에 의해 이미지 추출 금지된 URL: {}", realUrl);
                return null;
            }
            Document realDoc = safeUrlFetcher.fetchDocument(realUrl, "Mozilla/5.0", IMAGE_FETCH_TIMEOUT_MS);

            String ogImage = realDoc.select("meta[property=og:image]").attr("content");
            if (!ogImage.isEmpty()) return ogImage;

            String firstImg = realDoc.select("img").attr("src");
            return !firstImg.isEmpty() ? firstImg : null;
        } catch (Exception e) {
            return null;
        }
    }

    // 일반 웹페이지 대표 이미지 크롤링
    private String extractRepresentativeImage(String url) {
        try {
            if (!robotsTxtChecker.isAllowed(url, "Mozilla/5.0")) {
                log.warn("[크롤링 제한] robots.txt에 의해 이미지 추출 금지된 URL: {}", url);
                return null;
            }
            Document doc = safeUrlFetcher.fetchDocument(url, "Mozilla/5.0", IMAGE_FETCH_TIMEOUT_MS);

            String[] selectors = {
                    "meta[property=og:image]",
                    "meta[name=twitter:image]",
                    "meta[itemprop=image]",
                    "link[rel=image_src]"
            };

            for (String selector : selectors) {
                String imgUrl = doc.select(selector).attr("content");
                if (imgUrl.isEmpty()) {
                    imgUrl = doc.select(selector).attr("href");
                }
                if (!imgUrl.isEmpty()) {
                    return imgUrl;
                }
            }

            String imgTag = doc.select("img").attr("src");
            if (!imgTag.isEmpty()) {
                return imgTag;
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }

    // Google Custom Search API 특정 이미지 직접 검색
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

    private boolean isImageUrl(String url) {
        String lower = url.toLowerCase();
        return lower.endsWith(".jpg") ||
                lower.endsWith(".jpeg") ||
                lower.endsWith(".png") ||
                lower.endsWith(".gif") ||
                lower.endsWith(".webp");
    }

    // 전체 플로우
    public String getRelatedImageFromUrl(String url, String title) {
        String imgUrl;
        if (isNaverFromDB(url)) {
            imgUrl = extractFromNaverBlog(url);
            if (imgUrl != null && !imgUrl.isEmpty()) return imgUrl;
        } else {
            imgUrl = extractRepresentativeImage(url);
            if (imgUrl != null && !imgUrl.isEmpty()) return imgUrl;
        }

        if (title != null && !title.isEmpty()) {
            imgUrl = searchFirstDirectImageUrl(title);
            if (imgUrl != null) return imgUrl;
        }

        String domainOnly = extractDomainFromUrl(url);
        if (domainOnly != null && !domainOnly.isEmpty()) {
            imgUrl = searchFirstDirectImageUrl(domainOnly);
            if (imgUrl != null) return imgUrl;
        }

        return null;
    }
}
