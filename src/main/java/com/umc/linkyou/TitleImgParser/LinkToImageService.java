package com.umc.linkyou.TitleImgParser;

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

    //네이버 블로그 판별 함수
    private boolean isNaverBlog(String url) {
        return url.contains("blog.naver.com");
    }

    //네이버 블로그 iframe 내부 본문 접근 + 대표 이미지 추출
    private String extractFromNaverBlog(String blogUrl) {
        try {
            Document doc = Jsoup.connect(blogUrl)
                    .userAgent("Mozilla/5.0")
                    .get();
            // iframe src 추출
            String frameSrc = doc.select("iframe#mainFrame").attr("src");
            if (frameSrc == null || frameSrc.isEmpty()) return null;

            // 절대경로 조합
            String realUrl = "https://blog.naver.com" + frameSrc;
            Document realDoc = Jsoup.connect(realUrl)
                    .userAgent("Mozilla/5.0")
                    .get();

            // og:image 메타검색
            String ogImage = realDoc.select("meta[property=og:image]").attr("content");
            if (ogImage != null && !ogImage.isEmpty()) return ogImage;

            // 본문 내 첫 이미지 태그
            String firstImg = realDoc.select("img").attr("src");
            return firstImg != null && !firstImg.isEmpty() ? firstImg : null;
        } catch (Exception e) {
            return null;
        }
    }

    //일반 웹페이지 대표 이미지 크롤링 메서드
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

            // 본문 내 첫 이미지
            String imgTag = doc.select("img").attr("src");
            if (imgTag != null && !imgTag.isEmpty()) {
                return imgTag;
            }
        } catch (Exception e) {
            return null;
        }
        return null;
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
        String imgUrl;

        // 1. 네이버 블로그 전용 처리
        if (isNaverBlog(url)) {
            imgUrl = extractFromNaverBlog(url);
            if (imgUrl != null && !imgUrl.isEmpty()) return imgUrl;
        } else {
            // 2. 일반 페이지 대표 이미지 크롤링
            imgUrl = extractRepresentativeImage(url);
            if (imgUrl != null && !imgUrl.isEmpty()) return imgUrl;
        }

        // 3. 실패 시 → 구글 API 검색
        if (title != null && !title.isEmpty()) {
            imgUrl = searchFirstDirectImageUrl(title);
            if (imgUrl != null) return imgUrl;
        }

        // 4. 제목 없으면 도메인으로 검색
        String domain = extractDomainFromUrl(url);
        if (domain != null && !domain.isEmpty()) {
            imgUrl = searchFirstDirectImageUrl(domain);
            if (imgUrl != null) return imgUrl;
        }

        return null;
    }

    public String getRelatedImageFromUrl(String url) {
        String title = extractTitle(url);
        return getRelatedImageFromUrl(url, title);
    }

}
