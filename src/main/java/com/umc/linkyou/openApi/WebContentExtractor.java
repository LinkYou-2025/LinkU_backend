package com.umc.linkyou.openApi;

import com.umc.linkyou.apiPayload.code.status.ErrorStatus;
import com.umc.linkyou.apiPayload.exception.GeneralException;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
public class WebContentExtractor {

    // URL 인코딩 메서드 추가
    private String normalizeUrl(String inputUrl) {
        try {
            URI uri = new URI(inputUrl);
            String encodedPath = uri.getRawPath() != null
                    ? URLEncoder.encode(uri.getRawPath(), StandardCharsets.UTF_8)
                    .replace("+", "%20")
                    : null;
            String encodedQuery = uri.getRawQuery() != null
                    ? URLEncoder.encode(uri.getRawQuery(), StandardCharsets.UTF_8)
                    .replace("+", "%20")
                    : null;

            return new URI(
                    uri.getScheme(),
                    uri.getAuthority(),
                    encodedPath,
                    encodedQuery,
                    uri.getFragment()
            ).toString();

        } catch (Exception e) {
            log.error("[URL 인코딩 실패] {}", inputUrl, e);
            return inputUrl; // 실패 시 원본 반환
        }
    }

    public String extractTextFromUrl(String url) {
        try {
            // 한글/특수문자 안전 인코딩 처리
            String safeUrl = normalizeUrl(url);

            Document doc = Jsoup.connect(safeUrl) //safeUrl 사용
                    .userAgent("Mozilla/5.0")
                    .timeout(15000)
                    .get();

            String extracted = null; //모든 추출 결과를 담음

            // 1. <article>
            Elements article = doc.select("article");
            if (!article.isEmpty() && !article.text().isBlank()) extracted = article.text();

            // 2. <main>
            if (extracted == null) {
                Elements main = doc.select("main");
                if (!main.isEmpty() && !main.text().isBlank()) extracted = main.text();
            }

            // 3. class/id에 content, body
            if (extracted == null) {
                Elements content = doc.select("[class*=content], [id*=content], [class*=body], [id*=body]");
                if (!content.isEmpty() && !content.text().isBlank()) extracted = content.text();
            }

            // 4. <p>
            if (extracted == null) {
                Elements ps = doc.select("p");
                if (!ps.isEmpty() && !ps.text().isBlank()) extracted = ps.text();
            }

            // 5. <div>
            if (extracted == null) {
                Elements divs = doc.select("div");
                if (!divs.isEmpty() && !divs.text().isBlank()) extracted = divs.text();
            }

            // 6. 네이버 블로그(iframe)
            if (extracted == null) {
                Elements naverIframe = doc.select("iframe#mainFrame");
                if (!naverIframe.isEmpty()) {
                    String src = naverIframe.attr("src");
                    String iframeUrl = src.startsWith("http") ? src : "https://blog.naver.com" + src;
                    try {
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
                                extracted = containers.text();

                            if (extracted == null || extracted.isBlank()) {
                                String iframeBody = iframeDoc.body().text();
                                if (!iframeBody.isBlank())
                                    extracted = iframeBody;
                            }
                        }
                    } catch (Exception ee) {
                        log.warn("[네이버 블로그 iframe 크롤링 실패] iframeUrl: {} | 이유: {}", iframeUrl, ee.getMessage());
                    }
                }
            }

            // 7. <body> 전체
            if (extracted == null) {
                String bodyText = doc.body() != null ? doc.body().text() : "";
                if (!bodyText.isBlank()) extracted = bodyText;
            }

            // 최종 체크
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
