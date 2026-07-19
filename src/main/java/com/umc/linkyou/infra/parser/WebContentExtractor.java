package com.umc.linkyou.infra.parser;

import com.umc.linkyou.domain.classification.Domain;
import com.umc.linkyou.domain.enums.CrawlStrategy;
import com.umc.linkyou.repository.classification.domainRepository.DomainRepository;
import com.umc.linkyou.apiPayload.code.status.aiarticle.AiArticleErrorStatus;
import com.umc.linkyou.apiPayload.exception.GeneralException;
import com.umc.linkyou.utils.UrlUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.umc.linkyou.utils.UrlValidUtils.extractDomainTail;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebContentExtractor {

    private final DomainRepository domainRepository;
    private final RobotsTxtChecker robotsTxtChecker;

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

    static class NaverBlogExtractor implements ContentExtractorStrategy {
        @Override
        public String extract(Document doc, String url) throws Exception {
            Elements naverIframe = doc.select("iframe#mainFrame");
            if (!naverIframe.isEmpty()) {
                String src = naverIframe.attr("src");
                String iframeUrl = src.startsWith("http") ? src : "https://blog.naver.com" + src;
                Document iframeDoc = Jsoup.connect(iframeUrl)
                        .userAgent("Mozilla/5.0")
                        .timeout(15000)
                        .get();

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


    private ContentExtractorStrategy createStrategy(String domainTail) {
        Domain domain = domainRepository.findByDomainTail(domainTail).orElse(null);
        if (domain == null) return new DefaultExtractor();

        CrawlStrategy strategy = domain.getCrawlStrategy() != null ? domain.getCrawlStrategy() : CrawlStrategy.DEFAULT;
        return switch (strategy) {
            case IFRAME -> new NaverBlogExtractor();
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
            String domainTail = extractDomainTail(safeUrl);
            ContentExtractorStrategy strategy = crawlerStrategies.computeIfAbsent(domainTail, this::createStrategy);

            Document doc = Jsoup.connect(safeUrl)
                    .userAgent("Mozilla/5.0")
                    .timeout(15000)
                    .get();

            String extracted = strategy.extract(doc, safeUrl);

            if (extracted.isBlank()) {
                log.warn("[본문 추출 실패] URL: {}", url);
                throw new GeneralException(AiArticleErrorStatus._CONTENT_EXTRACTION_FAILED);
            }

            return extracted;

        } catch (GeneralException e) {
            throw e;
        } catch (Exception e) {
            log.error("[크롤링 실패] URL: {}, 이유: {}", url, e.getMessage());
            throw new GeneralException(AiArticleErrorStatus._CONTENT_EXTRACTION_FAILED);
        }
    }
}
