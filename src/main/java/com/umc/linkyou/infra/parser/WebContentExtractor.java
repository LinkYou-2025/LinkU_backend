package com.umc.linkyou.infra.parser;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.umc.linkyou.domain.classification.Domain;
import com.umc.linkyou.domain.enums.CrawlStrategy;
import com.umc.linkyou.infra.net.SafeUrlFetcher;
import com.umc.linkyou.infra.net.SsrfGuard;
import com.umc.linkyou.repository.classification.domainRepository.DomainRepository;
import com.umc.linkyou.apiPayload.code.status.aiarticle.AiArticleErrorStatus;
import com.umc.linkyou.apiPayload.exception.GeneralException;
import com.umc.linkyou.utils.UrlUtils;
import com.umc.linkyou.utils.UrlValidUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

// 도메인별 크롤링 전략(DEFAULT/IFRAME/BODY)으로 링크 페이지의 본문 텍스트를 추출한다.
@Slf4j
@Component
@RequiredArgsConstructor
public class WebContentExtractor {

    private final DomainRepository domainRepository;
    private final RobotsTxtChecker robotsTxtChecker;
    private final SafeUrlFetcher safeUrlFetcher;

    // 도메인 tail -> 크롤링 전략 캐시
    private final Cache<String, ContentExtractorStrategy> crawlerStrategies =
            Caffeine.newBuilder().maximumSize(2_000).build();

    interface ContentExtractorStrategy {
        String extract(Document doc, String url, NaverBlogIframeFetcher.Result iframePage) throws Exception;
    }

    // article → main → content/body → p → div → body 순으로 본문을 찾는 기본 전략
    static class DefaultExtractor implements ContentExtractorStrategy {
        @Override
        public String extract(Document doc, String url, NaverBlogIframeFetcher.Result iframePage) {
            Elements article = doc.select("article");
            if (!article.isEmpty() && !article.text().isBlank()) return article.text();

            Elements main = doc.select("main");
            if (!main.isEmpty() && !main.text().isBlank()) return main.text();

            Elements content = doc.select("[class*=content], [id*=content], [class*=body], [id*=body]");
            if (!content.isEmpty() && !content.text().isBlank()) return content.text();

            Elements ps = doc.select("p");
            if (!ps.isEmpty() && !ps.text().isBlank()) return ps.text();

            Elements divs = doc.select("div");
            if (!divs.isEmpty() && !divs.text().isBlank()) return divs.text();

            return doc.body().text();
        }
    }

    // 네이버 블로그 iframe 내부 페이지를 추가로 fetch해 본문을 추출하는 전략
    static class NaverBlogExtractor implements ContentExtractorStrategy {
        private final SafeUrlFetcher safeUrlFetcher;
        private final RobotsTxtChecker robotsTxtChecker;

        NaverBlogExtractor(SafeUrlFetcher safeUrlFetcher, RobotsTxtChecker robotsTxtChecker) {
            this.safeUrlFetcher = safeUrlFetcher;
            this.robotsTxtChecker = robotsTxtChecker;
        }

        @Override
        public String extract(Document doc, String url, NaverBlogIframeFetcher.Result iframePage) throws Exception {
            Elements naverIframe = doc.select("iframe#mainFrame");
            if (!naverIframe.isEmpty()) {
                String src = naverIframe.attr("src");
                Document iframeDoc;
                if (iframePage != null && iframePage.status() != NaverBlogIframeFetcher.Status.NOT_PRESENT) {
                    if (iframePage.status() == NaverBlogIframeFetcher.Status.DISALLOWED) {
                        return doc.body().text();
                    }
                    if (iframePage.status() != NaverBlogIframeFetcher.Status.FETCHED
                            || iframePage.document() == null) {
                        throw new IOException("네이버 iframe 페이지를 가져오지 못했습니다.");
                    }
                    iframeDoc = iframePage.document();
                } else {
                    String iframeUrl = src.startsWith("http") ? src : "https://blog.naver.com" + src;
                    // 공용 iframe 결과가 없으면 기존 경로대로 robots 확인 후 fetch한다.
                    if (!robotsTxtChecker.isAllowed(iframeUrl, "Mozilla/5.0")) {
                        log.warn("[크롤링 제한] robots.txt에 의해 본문 추출 금지된 iframe URL: {}", iframeUrl);
                        return doc.body().text();
                    }
                    iframeDoc = safeUrlFetcher.fetchDocument(iframeUrl, "Mozilla/5.0", 15000);
                }

                String logNo = null;
                String[] paramPairs = src.split("&");
                for (String pair : paramPairs) {
                    if (pair.startsWith("logNo=")) {
                        logNo = pair.substring("logNo=".length());
                        break;
                    }
                }

                if (logNo != null) {
                    Elements containers = iframeDoc.select("#post-view" + logNo + " .se-main-container");
                    if (containers.isEmpty()) {
                        containers = iframeDoc.select(".se-main-container");
                    }
                    if (!containers.isEmpty() && !containers.text().isBlank())
                        return containers.text();
                }
                return iframeDoc.body().text();
            }
            return doc.body().text();
        }
    }

    static class BodyExtractor implements ContentExtractorStrategy {
        @Override
        public String extract(Document doc, String url, NaverBlogIframeFetcher.Result iframePage) {
            return doc.body().text();
        }
    }


    // 정확한 호스트 → apex 도메인 순으로 domains 행을 찾아 전략을 만든다.
    private ContentExtractorStrategy createStrategy(List<String> domainTailCandidates) {
        Domain domain = domainTailCandidates.stream()
                .map(domainRepository::findByDomainTail)
                .flatMap(Optional::stream)
                .findFirst()
                .orElse(null);
        if (domain == null) return new DefaultExtractor();

        CrawlStrategy strategy = domain.getCrawlStrategy() != null ? domain.getCrawlStrategy() : CrawlStrategy.DEFAULT;
        return switch (strategy) {
            case IFRAME -> new NaverBlogExtractor(safeUrlFetcher, robotsTxtChecker);
            case BODY -> new BodyExtractor();
            default -> new DefaultExtractor();
        };
    }

    // robots.txt 확인 후 직접 fetch해서 본문을 추출한다.
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

    // 이미 fetch된 Document로 본문만 추출한다.
    public String extractTextFromDocument(String url, Document doc) {
        return extractTextFromDocument(url, doc, null);
    }

    // iframe 문서가 이미 fetch됐다면 본문 추출에서 다시 요청하지 않고 재사용한다.
    public String extractTextFromDocument(
            String url, Document doc, NaverBlogIframeFetcher.Result iframePage) {
        try {
            if (doc == null) {
                log.warn("[본문 추출 실패] fetch된 Document 없음, URL: {}", url);
                throw new GeneralException(AiArticleErrorStatus._CONTENT_EXTRACTION_FAILED);
            }

            String safeUrl = UrlUtils.normalizeUrl(url);
            List<String> domainTailCandidates = UrlValidUtils.extractDomainTailCandidates(safeUrl);
            ContentExtractorStrategy strategy = domainTailCandidates.isEmpty()
                    ? new DefaultExtractor()
                    : crawlerStrategies.get(domainTailCandidates.get(0), key -> createStrategy(domainTailCandidates));

            String extracted = strategy.extract(doc, safeUrl, iframePage);

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
