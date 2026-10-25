package com.umc.linkyou.infra.parser;

import com.umc.linkyou.infra.net.SafeUrlFetcher;
import com.umc.linkyou.infra.net.SsrfGuard;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

import java.net.URL;

// 링크의 도메인과 제목(og:title → <title>)을 추출해 AI 분류의 참고값으로 제공한다.
@Slf4j
@Component
@RequiredArgsConstructor
public class TitleDomainParser {

    private final RobotsTxtChecker robotsTxtChecker;
    private final SafeUrlFetcher safeUrlFetcher;

    // robots.txt 확인 후 직접 fetch해서 파싱한다.
    public ParsedPageInfo parseUrl(String url) {
        Document doc = null;
        try {
            if (!robotsTxtChecker.isAllowed(url, "Mozilla/5.0")) {
                log.warn("[크롤링 제한] robots.txt에 의해 제목 추출 금지된 URL: {}", url);
            } else {
                doc = safeUrlFetcher.fetchDocument(url, "Mozilla/5.0", 10000);
            }
        } catch (SsrfGuard.BlockedException e) {
            log.warn("[크롤링 제한] SSRF 정책에 의해 차단된 URL: {}, 이유: {}", url, e.getMessage());
        } catch (Exception e) {
            log.warn("[도메인/제목 추출 실패] {}", e.getMessage());
        }
        return parseUrl(url, doc);
    }

    // 이미 fetch된 Document로 파싱만 한다.
    public ParsedPageInfo parseUrl(String url, Document doc) {
        String domain = null;
        try {
            domain = new URL(url).getHost();
        } catch (Exception e) {
            log.warn("[도메인 추출 실패] {}", e.getMessage());
        }

        String title = null;
        if (doc != null) {
            Element ogTitle = doc.selectFirst("meta[property=og:title]");
            if (ogTitle != null) {
                title = ogTitle.attr("content");
            }
            if (title == null || title.isBlank()) {
                title = doc.title();
            }
            if (title != null) {
                title = title.replaceAll("[^ㄱ-ㅎㅏ-ㅣ가-힣a-zA-Z0-9\\s]", "");
            }
        }

        return new ParsedPageInfo(domain, title);
    }

    public static record ParsedPageInfo(String domain, String title) {}
}
