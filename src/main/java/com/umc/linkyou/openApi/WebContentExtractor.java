package com.umc.linkyou.openApi;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class WebContentExtractor {

    public String extractTextFromUrl(String url) {
        try {
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0")
                    .timeout(15000)
                    .get();

            // 1. <article>
            Elements article = doc.select("article");
            if (!article.isEmpty() && !article.text().isBlank()) return article.text();

            // 2. <main>
            Elements main = doc.select("main");
            if (!main.isEmpty() && !main.text().isBlank()) return main.text();

            // 3. class/id에 content, body
            Elements content = doc.select("[class*=content], [id*=content], [class*=body], [id*=body]");
            if (!content.isEmpty() && !content.text().isBlank()) return content.text();

            // 4. <p>
            Elements ps = doc.select("p");
            if (!ps.isEmpty() && !ps.text().isBlank()) return ps.text();

            // 5. <div>
            Elements divs = doc.select("div");
            if (!divs.isEmpty() && !divs.text().isBlank()) return divs.text();

            // 6. 네이버 블로그(iframe) 케이스 감지 및 처리
            Elements naverIframe = doc.select("iframe#mainFrame");
            if (!naverIframe.isEmpty()) {
                String src = naverIframe.attr("src");
                // 절대경로 보장
                String iframeUrl = src.startsWith("http") ? src : "https://blog.naver.com" + src;
                try {
                    Document iframeDoc = Jsoup.connect(iframeUrl)
                            .userAgent("Mozilla/5.0")
                            .timeout(15000)
                            .get();
                    // logNo 추출
                    String logNo = null;
                    String[] paramPairs = src.split("&");
                    for (String pair : paramPairs) {
                        if (pair.startsWith("logNo=")) {
                            logNo = pair.substring("logNo=".length());
                            break;
                        }
                    }
                    // 본문 컨테이너 탐색
                    if (logNo != null) {
                        // 최신 구조 우선
                        Elements containers = iframeDoc.select("#post-view" + logNo + " .se-main-container");
                        if (containers.isEmpty()) {
                            containers = iframeDoc.select(".se-main-container"); // 예전/특수 구조
                        }
                        if (!containers.isEmpty() && !containers.text().isBlank())
                            return containers.text();
                        // 그래도 없으면 fallback
                        String iframeBody = iframeDoc.body().text();
                        if (!iframeBody.isBlank())
                            return iframeBody;
                    }
                } catch (Exception ee) {
                    log.warn("[네이버 블로그 iframe 크롤링 실패] iframeUrl: {} | 이유: {}", iframeUrl, ee.getMessage());
                }
            }

            // 7. <body> 전체 텍스트
            String bodyText = doc.body() != null ? doc.body().text() : "";
            if (!bodyText.isBlank()) return bodyText;

            // 그래도 없으면 HTML 일부 로그
            log.warn("[본문 추출 실패] URL: {} | HTML 길이: {}", url, doc.html().length());
            log.debug("[HTML 미리보기] {}", doc.html().substring(0, Math.min(500, doc.html().length())));
            return "";

        } catch (Exception e) {
            log.error("[크롤링 실패] URL: {}, 이유: {}", url, e.getMessage());
            return null;
        }
    }
}
