package com.umc.linkyou.infra.parser;

import com.umc.linkyou.infra.net.SafeUrlFetcher;
import com.umc.linkyou.repository.classification.domainRepository.DomainRepository;
import com.umc.linkyou.domain.classification.Domain;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;

import java.net.HttpURLConnection;
import java.net.URI;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class LinkToImageService {

    private final DomainRepository domainRepository;
    private final SafeUrlFetcher safeUrlFetcher;
    private final RobotsTxtChecker robotsTxtChecker;

    private static final int IMAGE_FETCH_TIMEOUT_MS = 5000;
    private static final int MAX_IMG_CANDIDATES = 3;

    // 대표 이미지 최소 용량 기준, 아이콘류 방지용임
    private static final long MIN_IMAGE_BYTES = 15 * 1024;

    // Content-Length만 확인, 실패하면 false로 처리함
    private boolean isLargeEnough(String imageUrl) {
        try {
            HttpURLConnection conn = safeUrlFetcher.openConnection(imageUrl, "Mozilla/5.0", 1500, 1500);
            long length = conn.getContentLengthLong();
            conn.disconnect();
            return length >= MIN_IMAGE_BYTES;
        } catch (Exception e) {
            return false;
        }
    }

    // baseUri 기준으로 절대 URL 변환, http(s)가 아니면 null
    private String toAbsoluteHttpUrl(Element el, String attr) {
        String abs = el.absUrl(attr);
        String lower = abs.toLowerCase();
        return (lower.startsWith("http://") || lower.startsWith("https://")) ? abs : null;
    }

    private String firstValidImage(Document doc, String selector, String attr, int limit) {
        int checked = 0;
        for (Element el : doc.select(selector)) {
            if (checked >= limit) break;
            String abs = toAbsoluteHttpUrl(el, attr);
            if (abs == null) continue;
            checked++;
            if (isLargeEnough(abs)) return abs;
        }
        return null;
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
            if (!robotsTxtChecker.isAllowed(blogUrl, "Mozilla/5.0")) {
                log.warn("[크롤링 제한] robots.txt에 의해 이미지 추출 금지된 URL: {}", blogUrl);
                return null;
            }
            Document doc = safeUrlFetcher.fetchDocument(blogUrl, "Mozilla/5.0", IMAGE_FETCH_TIMEOUT_MS);
            Element frame = doc.selectFirst("iframe#mainFrame");
            if (frame == null) return null;

            String realUrl = toAbsoluteHttpUrl(frame, "src");
            if (realUrl == null) return null;
            if (!robotsTxtChecker.isAllowed(realUrl, "Mozilla/5.0")) {
                log.warn("[크롤링 제한] robots.txt에 의해 이미지 추출 금지된 URL: {}", realUrl);
                return null;
            }
            Document realDoc = safeUrlFetcher.fetchDocument(realUrl, "Mozilla/5.0", IMAGE_FETCH_TIMEOUT_MS);

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

    // 네이버 블로그는 iframe 안 다른 호스트를 따로 fetch해야 해서 doc 재사용 대상이 아님
    public String getRelatedImageFromUrl(String url, String title, Document doc) {
        if (isNaverFromDB(url)) {
            return extractFromNaverBlog(url);
        }
        return (doc != null) ? extractRepresentativeImageFromDoc(doc) : extractRepresentativeImage(url);
    }
}
