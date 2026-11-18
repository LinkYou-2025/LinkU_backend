package com.umc.linkyou.infra.parser;

import java.util.Optional;

import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import com.umc.linkyou.apiPayload.code.status.aiarticle.AiArticleErrorStatus;
import com.umc.linkyou.apiPayload.exception.GeneralException;
import com.umc.linkyou.infra.net.SafeUrlFetcher;
import com.umc.linkyou.infra.net.SsrfGuard;
import com.umc.linkyou.utils.UrlUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebContentExtractor {

    private final RobotsTxtChecker robotsTxtChecker;
    private final SafeUrlFetcher safeUrlFetcher;

    private static final ContentExtractorStrategy DEFAULT_EXTRACTOR = new DefaultExtractor();

    interface ContentExtractorStrategy {
        String extract(Document doc, String url) throws Exception;
    }

    // 본문일 가능성이 높은 태그부터 차례로 본다.
    static class DefaultExtractor implements ContentExtractorStrategy {
        @Override
        public String extract(Document doc, String url) {
            Elements article = doc.select("article");
            if (!article.isEmpty() && !article.text().isBlank()) return article.text();

            Elements main = doc.select("main");
            if (!main.isEmpty() && !main.text().isBlank()) return main.text();

            Elements content =
                    doc.select("[class*=content], [id*=content], [class*=body], [id*=body]");
            if (!content.isEmpty() && !content.text().isBlank()) return content.text();

            Elements ps = doc.select("p");
            if (!ps.isEmpty() && !ps.text().isBlank()) return ps.text();

            Elements divs = doc.select("div");
            if (!divs.isEmpty() && !divs.text().isBlank()) return divs.text();

            return doc.body().text();
        }
    }

    // 네이버 블로그 PC 페이지는 본문이 iframe 안에 있어 한 번 더 요청한다.
    static class NaverBlogExtractor implements ContentExtractorStrategy {
        private final SafeUrlFetcher safeUrlFetcher;
        private final RobotsTxtChecker robotsTxtChecker;

        NaverBlogExtractor(SafeUrlFetcher safeUrlFetcher, RobotsTxtChecker robotsTxtChecker) {
            this.safeUrlFetcher = safeUrlFetcher;
            this.robotsTxtChecker = robotsTxtChecker;
        }

        @Override
        public String extract(Document doc, String url) throws Exception {
            Optional<String> frameUrl = NaverBlog.postFrameUrl(doc);
            if (frameUrl.isEmpty()) {
                // iframe 안 문서나 모바일 페이지는 이미 본문이 있다.
                return NaverBlog.extractPostBody(doc, url);
            }
            String iframeUrl = frameUrl.get();
            // iframe 주소는 다른 호스트일 수 있어 robots.txt를 다시 확인한다.
            if (!robotsTxtChecker.isAllowed(iframeUrl, "Mozilla/5.0")) {
                log.warn("[크롤링 제한] robots.txt에 의해 본문 추출 금지된 iframe URL: {}", iframeUrl);
                return doc.body().text();
            }
            Document iframeDoc = safeUrlFetcher.fetchDocument(iframeUrl, "Mozilla/5.0", 15000);
            return NaverBlog.extractPostBody(iframeDoc, iframeUrl);
        }
    }

    private ContentExtractorStrategy strategyFor(String url) {
        if (NaverBlog.isBlogUrl(url)) {
            return new NaverBlogExtractor(safeUrlFetcher, robotsTxtChecker);
        }
        return DEFAULT_EXTRACTOR;
    }

    public String extractTextFromUrl(String url) {
        try {
            if (!robotsTxtChecker.isAllowed(url, "Mozilla/5.0")) {
                log.warn("[크롤링 제한] robots.txt에 의해 접근 금지된 URL: {}", url);
                throw new GeneralException(AiArticleErrorStatus._CONTENT_EXTRACTION_PROHIBITED);
            }
            String safeUrl = UrlUtils.normalizeUrl(url);
            Document doc = safeUrlFetcher.fetchDocument(safeUrl, "Mozilla/5.0", 15000);
            return extractTextFromDocument(url, doc);
        } catch (GeneralException e) {
            throw e;
        } catch (SsrfGuard.BlockedException e) {
            log.warn("[크롤링 제한] SSRF 정책에 의해 차단된 URL: {}, 이유: {}", url, e.getMessage());
            throw new GeneralException(AiArticleErrorStatus._CONTENT_EXTRACTION_PROHIBITED);
        } catch (Exception e) {
            log.error("[크롤링 실패] URL: {}, 이유: {}", url, e.getMessage());
            throw new GeneralException(AiArticleErrorStatus._CONTENT_EXTRACTION_FAILED);
        }
    }

    // 이미 받아온 문서를 다시 요청하지 않고 본문만 뽑는다.
    public String extractTextFromDocument(String url, Document doc) {
        try {
            if (doc == null) {
                log.warn("[본문 추출 실패] fetch된 Document 없음, URL: {}", url);
                throw new GeneralException(AiArticleErrorStatus._CONTENT_EXTRACTION_FAILED);
            }

            String safeUrl = UrlUtils.normalizeUrl(url);
            String extracted = strategyFor(safeUrl).extract(doc, safeUrl);

            if (extracted.isBlank()) {
                log.warn("[본문 추출 실패] URL: {}", url);
                throw new GeneralException(AiArticleErrorStatus._CONTENT_EXTRACTION_FAILED);
            }

            return extracted;

        } catch (GeneralException e) {
            throw e;
        } catch (SsrfGuard.BlockedException e) {
            log.warn("[크롤링 제한] SSRF 정책에 의해 차단된 URL: {}, 이유: {}", url, e.getMessage());
            throw new GeneralException(AiArticleErrorStatus._CONTENT_EXTRACTION_PROHIBITED);
        } catch (Exception e) {
            log.error("[크롤링 실패] URL: {}, 이유: {}", url, e.getMessage());
            throw new GeneralException(AiArticleErrorStatus._CONTENT_EXTRACTION_FAILED);
        }
    }
}
