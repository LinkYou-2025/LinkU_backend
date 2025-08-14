package com.umc.linkyou.TitleImgParser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.linkyou.repository.classification.domainRepository.DomainRepository;
import com.umc.linkyou.domain.classification.Domain;
import com.umc.linkyou.repository.classification.domainRepository.DomainRepositoryCustom;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.beans.factory.annotation.Value;

import java.net.URI;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LinkToImageService {

    private final DomainRepository domainRepository;

    @Value("${custom.search.api.key}")
    private String apiKey;
    @Value("${custom.search.engine.id}")
    private String searchEngineId;

    // URL에서 제목 크롤링
    public String extractTitle(String url) {
        try {
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0")
                    .get();
            String ogTitle = doc.select("meta[property=og:title]").attr("content");
            if (ogTitle != null && !ogTitle.isEmpty()) {
                return ogTitle.replaceAll("[^ㄱ-ㅎㅏ-ㅣ가-힣a-zA-Z0-9\\s]", "");
            }
            return doc.title().replaceAll("[^ㄱ-ㅎㅏ-ㅣ가-힣a-zA-Z0-9\\s]", "");
        } catch (Exception e) {
            return null;
        }
    }

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
            Document doc = Jsoup.connect(blogUrl)
                    .userAgent("Mozilla/5.0")
                    .get();
            String frameSrc = doc.select("iframe#mainFrame").attr("src");
            if (frameSrc == null || frameSrc.isEmpty()) return null;

            String realUrl = "https://blog.naver.com" + frameSrc;
            Document realDoc = Jsoup.connect(realUrl)
                    .userAgent("Mozilla/5.0")
                    .get();

            String ogImage = realDoc.select("meta[property=og:image]").attr("content");
            if (ogImage != null && !ogImage.isEmpty()) return ogImage;

            String firstImg = realDoc.select("img").attr("src");
            return firstImg != null && !firstImg.isEmpty() ? firstImg : null;
        } catch (Exception e) {
            return null;
        }
    }

    // 일반 웹페이지 대표 이미지 크롤링
    private String extractRepresentativeImage(String url) {
        try {
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0")
                    .get();

            String[] selectors = {
                    "meta[property=og:image]",
                    "meta[name=twitter:image]",
                    "meta[itemprop=image]",
                    "link[rel=image_src]"
            };

            for (String selector : selectors) {
                String imgUrl = doc.select(selector).attr("content");
                if (imgUrl == null || imgUrl.isEmpty()) {
                    imgUrl = doc.select(selector).attr("href");
                }
                if (imgUrl != null && !imgUrl.isEmpty()) {
                    return imgUrl;
                }
            }

            String imgTag = doc.select("img").attr("src");
            if (imgTag != null && !imgTag.isEmpty()) {
                return imgTag;
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }

    // Google Custom Search API 특정 이미지 직접 검색
    public String searchFirstDirectImageUrl(String query) {
        final int MAX_TRY = 5;
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

    public String getRelatedImageFromUrl(String url) {
        String title = extractTitle(url);
        return getRelatedImageFromUrl(url, title);
    }
}
