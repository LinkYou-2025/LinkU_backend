package com.umc.linkyou.infra.parser;

import com.umc.linkyou.infra.net.SafeUrlFetcher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;

import java.net.HttpURLConnection;
import java.net.URI;
import java.util.Set;

// 링크 페이지에서 대표 이미지(og:image 등 → 본문 img 순)를 찾아 헤더로 유효성을 확인한 뒤 반환한다.
@Slf4j
@Service
@RequiredArgsConstructor
public class LinkToImageService {

    private final SafeUrlFetcher safeUrlFetcher;
    private final RobotsTxtChecker robotsTxtChecker;

    private static final int IMAGE_FETCH_TIMEOUT_MS = 5000;
    private static final int MAX_IMG_CANDIDATES = 3;

    // 아이콘류를 거르기 위한 대표 이미지 최소 용량
    private static final long MIN_IMAGE_BYTES = 15 * 1024;

    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of("image/jpeg", "image/png", "image/webp");

    // Content-Type/Content-Length 헤더로 대표 이미지로 쓸 수 있는지 확인한다.
    private boolean isValidImage(String imageUrl) {
        try {
            HttpURLConnection conn = safeUrlFetcher.openConnection(imageUrl, "Mozilla/5.0", 1500, 1500);
            String contentType = conn.getContentType();
            String mimeType = contentType == null ? "" : contentType.split(";")[0].trim().toLowerCase();
            boolean valid = ALLOWED_IMAGE_TYPES.contains(mimeType) && conn.getContentLengthLong() >= MIN_IMAGE_BYTES;
            conn.disconnect();
            return valid;
        } catch (Exception e) {
            return false;
        }
    }

    // 절대 https URL만 반환, 그 외는 null
    private String toAbsoluteHttpsUrl(Element el, String attr) {
        String abs = el.absUrl(attr);
        return abs.toLowerCase().startsWith("https://") ? abs : null;
    }

    private String firstValidImage(Document doc, String selector, String attr, int limit) {
        int checked = 0;
        for (Element el : doc.select(selector)) {
            if (checked >= limit) break;
            String abs = toAbsoluteHttpsUrl(el, attr);
            if (abs == null) continue;
            checked++;
            if (isValidImage(abs)) return abs;
        }
        return null;
    }

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

    // PC 네이버 블로그(iframe#mainFrame 구조) 여부
    private boolean isNaverBlog(String url) {
        return "blog.naver.com".equals(extractDomainFromUrl(url));
    }

    private String extractFromNaverBlog(String blogUrl) {
        return extractFromNaverBlog(blogUrl, null, null);
    }

    // 네이버 블로그 iframe 내부 페이지에서 대표 이미지를 추출한다. blogDoc이 있으면 바깥 페이지 fetch를 생략한다.
    private String extractFromNaverBlog(
            String blogUrl, Document blogDoc, NaverBlogIframeFetcher.Result iframePage) {
        try {
            Document doc = blogDoc;
            if (doc == null) {
                if (!robotsTxtChecker.isAllowed(blogUrl, "Mozilla/5.0")) {
                    log.warn("[크롤링 제한] robots.txt에 의해 이미지 추출 금지된 URL: {}", blogUrl);
                    return null;
                }
                doc = safeUrlFetcher.fetchDocument(blogUrl, "Mozilla/5.0", IMAGE_FETCH_TIMEOUT_MS);
            }
            Element frame = doc.selectFirst("iframe#mainFrame");
            if (frame == null) return null;

            Document realDoc;
            if (iframePage != null && iframePage.status() != NaverBlogIframeFetcher.Status.NOT_PRESENT) {
                // 공유 요청이 실패했거나 차단되면 재요청 없이 대표 이미지 없음으로 처리한다.
                if (iframePage.status() != NaverBlogIframeFetcher.Status.FETCHED) return null;
                realDoc = iframePage.document();
            } else {
                String realUrl = toAbsoluteHttpsUrl(frame, "src");
                if (realUrl == null) return null;
                if (!robotsTxtChecker.isAllowed(realUrl, "Mozilla/5.0")) {
                    log.warn("[크롤링 제한] robots.txt에 의해 이미지 추출 금지된 URL: {}", realUrl);
                    return null;
                }
                realDoc = safeUrlFetcher.fetchDocument(realUrl, "Mozilla/5.0", IMAGE_FETCH_TIMEOUT_MS);
            }
            if (realDoc == null) return null;

            String ogImage = firstValidImage(realDoc, "meta[property=og:image]", "content", 1);
            if (ogImage != null) return ogImage;

            return firstValidImage(realDoc, "img[src]", "src", MAX_IMG_CANDIDATES);
        } catch (Exception e) {
            return null;
        }
    }

    private String extractRepresentativeImage(String url) {
        try {
            if (!robotsTxtChecker.isAllowed(url, "Mozilla/5.0")) {
                log.warn("[크롤링 제한] robots.txt에 의해 이미지 추출 금지된 URL: {}", url);
                return null;
            }
            Document doc = safeUrlFetcher.fetchDocument(url, "Mozilla/5.0", IMAGE_FETCH_TIMEOUT_MS);
            return extractRepresentativeImageFromDoc(doc);
        } catch (Exception e) {
            return null;
        }
    }

    private String extractRepresentativeImageFromDoc(Document doc) {
        if (doc == null) return null;

        String[] selectors = {
                "meta[property=og:image]",
                "meta[name=twitter:image]",
                "meta[itemprop=image]",
                "link[rel=image_src]"
        };

        for (String selector : selectors) {
            String imgUrl = firstValidImage(doc, selector + "[content]", "content", 1);
            if (imgUrl == null) {
                imgUrl = firstValidImage(doc, selector + "[href]", "href", 1);
            }
            if (imgUrl != null) return imgUrl;
        }

        return firstValidImage(doc, "img[src]", "src", MAX_IMG_CANDIDATES);
    }

    public String getRelatedImageFromUrl(String url, String title) {
        return getRelatedImageFromUrl(url, title, null);
    }

    // doc이 있으면 재사용하고, 없으면 직접 fetch해서 대표 이미지를 찾는다.
    public String getRelatedImageFromUrl(String url, String title, Document doc) {
        return getRelatedImageFromUrl(url, title, doc, null);
    }

    public String getRelatedImageFromUrl(
            String url, String title, Document doc, NaverBlogIframeFetcher.Result iframePage) {
        if (isNaverBlog(url)) {
            return extractFromNaverBlog(url, doc, iframePage);
        }
        return (doc != null) ? extractRepresentativeImageFromDoc(doc) : extractRepresentativeImage(url);
    }
}
