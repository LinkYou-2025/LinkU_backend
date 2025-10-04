package com.umc.linkyou.aiCategoryClassifier.util;

import com.umc.linkyou.domain.classification.Domain;
import com.umc.linkyou.domain.enums.CrawlStrategy;
import com.umc.linkyou.repository.classification.domainRepository.DomainRepository;
import com.umc.linkyou.apiPayload.code.status.ErrorStatus;
import com.umc.linkyou.apiPayload.exception.GeneralException;
import com.umc.linkyou.utils.UrlUtils;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
public class WebContentExtractor {

    private final DomainRepository domainRepository;

    public WebContentExtractor(DomainRepository domainRepository) {
        this.domainRepository = domainRepository;
    }

    interface ContentExtractorStrategy {
        String extract(Document doc, String url) throws Exception;
    }

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

                    String iframeBody = iframeDoc.body().text();
                    if (!iframeBody.isBlank())
                        return iframeBody;
                }
            }
            return "";
        }
    }

    static class BodyExtractor implements ContentExtractorStrategy {
        @Override
        public String extract(Document doc, String url) {
            return doc.body() != null ? doc.body().text() : "";
        }
    }

    private Map<String, ContentExtractorStrategy> crawlerStrategies;

    private void initStrategies(List<Domain> domains) {
        crawlerStrategies = domains.stream().collect(
                Collectors.toMap(
                        Domain::getDomainTail,
                        domain -> {
                            CrawlStrategy strategy = domain.getCrawlStrategy();
                            switch (strategy) {
                                case IFRAME:
                                    return new NaverBlogExtractor();
                                case BODY:
                                    return new BodyExtractor();
                                case DEFAULT:
                                default:
                                    return new DefaultExtractor();
                            }
                        }
                )
        );
    }

    public String extractTextFromUrl(String url) {
        try {
            if (crawlerStrategies == null) {
                List<Domain> domains = domainRepository.findAll();
                initStrategies(domains);
            }

            String safeUrl = UrlUtils.normalizeUrl(url);
            Document doc = Jsoup.connect(safeUrl)
                    .userAgent("Mozilla/5.0")
                    .timeout(15000)
                    .get();

            String host = new java.net.URL(safeUrl).getHost();

            String targetDomainTail = crawlerStrategies.keySet().stream()
                    .filter(host::endsWith)
                    .max((a, b) -> Integer.compare(a.length(), b.length()))
                    .orElse(null);

            ContentExtractorStrategy strategy = targetDomainTail != null ? crawlerStrategies.get(targetDomainTail) : new DefaultExtractor();

            String extracted = strategy.extract(doc, safeUrl);

            if (extracted == null || extracted.isBlank()) {
                extracted = doc.body() != null ? doc.body().text() : "";
            }

            if (extracted == null || extracted.isBlank()) {
                log.warn("[본문 추출 실패] URL: {}", url);
                throw new GeneralException(ErrorStatus._CONTENT_EXTRACTION_FAILED);
            }

            return extracted;

        } catch (GeneralException e) {
            throw e;
        } catch (Exception e) {
            log.error("[크롤링 실패] URL: {}, 이유: {}", url, e.getMessage());
            throw new GeneralException(ErrorStatus._CONTENT_EXTRACTION_FAILED);
        }
    }
}
