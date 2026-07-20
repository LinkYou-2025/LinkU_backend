package com.umc.linkyou.infra.parser;

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

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebContentExtractor {

    private final DomainRepository domainRepository;
    private final RobotsTxtChecker robotsTxtChecker;
    private final SafeUrlFetcher safeUrlFetcher;

    private final Map<String, ContentExtractorStrategy> crawlerStrategies = new ConcurrentHashMap<>();

    interface ContentExtractorStrategy {
        String extract(Document doc, String url) throws Exception;
    }

    // 크롤링 전략 구현체
    static class DefaultExtractor implements ContentExtractorStrategy {
        @Override
        public String extract(Document doc, String url) {
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

    // iframe 안의 실제 본문(blog.naver.com)으로 2차 요청을 보내야 하므로 SafeUrlFetcher/RobotsTxtChecker를 직접 들고 있는다.
    static class NaverBlogExtractor implements ContentExtractorStrategy {
        private final SafeUrlFetcher safeUrlFetcher;
        private final RobotsTxtChecker robotsTxtChecker;

        NaverBlogExtractor(SafeUrlFetcher safeUrlFetcher, RobotsTxtChecker robotsTxtChecker) {
            this.safeUrlFetcher = safeUrlFetcher;
            this.robotsTxtChecker = robotsTxtChecker;
        }

        @Override
        public String extract(Document doc, String url) throws Exception {
            Elements naverIframe = doc.select("iframe#mainFrame");
            if (!naverIframe.isEmpty()) {
                String src = naverIframe.attr("src");
                String iframeUrl = src.startsWith("http") ? src : "https://blog.naver.com" + src;
                // 원본 url에 대한 robots.txt 허용 여부는 extractTextFromUrl()에서 이미 확인했지만,
                // 2차 요청은 다른 호스트(blog.naver.com)로 나갈 수 있으므로 별도로 다시 확인한다.
                if (!robotsTxtChecker.isAllowed(iframeUrl, "Mozilla/5.0")) {
                    log.warn("[크롤링 제한] robots.txt에 의해 본문 추출 금지된 iframe URL: {}", iframeUrl);
                    return doc.body().text();
                }
                // 2차 요청도 SafeUrlFetcher를 거쳐야 iframe src가 내부망 주소로 조작된 경우를 막을 수 있다.
                Document iframeDoc = safeUrlFetcher.fetchDocument(iframeUrl, "Mozilla/5.0", 15000);

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
        public String extract(Document doc, String url) {
            return doc.body().text();
        }
    }


    // domainTailCandidates: [정확한 호스트, (있다면) registry-suffix apex 도메인] 순서.
    // someuser.tistory.com처럼 정확히 일치하는 행이 없는 서브도메인은 apex(tistory.com) 행으로 폴백한다.
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

    public String extractTextFromUrl(String url) {
        try {
            if (!robotsTxtChecker.isAllowed(url, "Mozilla/5.0")) {
                log.warn("[크롤링 제한] robots.txt에 의해 접근 금지된 URL: {}", url);
                throw new GeneralException(AiArticleErrorStatus._CONTENT_EXTRACTION_PROHIBITED);
            }

            String safeUrl = UrlUtils.normalizeUrl(url);
            List<String> domainTailCandidates = UrlValidUtils.extractDomainTailCandidates(safeUrl);
            ContentExtractorStrategy strategy = domainTailCandidates.isEmpty()
                    ? new DefaultExtractor()
                    : crawlerStrategies.computeIfAbsent(domainTailCandidates.get(0), key -> createStrategy(domainTailCandidates));

            Document doc = safeUrlFetcher.fetchDocument(safeUrl, "Mozilla/5.0", 15000);

            String extracted = strategy.extract(doc, safeUrl);

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
