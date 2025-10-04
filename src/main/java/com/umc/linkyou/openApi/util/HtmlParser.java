package com.umc.linkyou.openApi.util;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

import java.net.URI;

@Slf4j
@Component
public class HtmlParser {

    public ParsedPageInfo parseUrl(String url) {
        String domain = null;
        String title = null;

        try {
            domain = new URI(url).getHost();

            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0")
                    .timeout(10000)
                    .get();

            Element ogTitle = doc.selectFirst("meta[property=og:title]");
            if (ogTitle != null) {
                title = ogTitle.attr("content");
            }
            if (title == null || title.isBlank()) {
                title = doc.title();
            }
        } catch (Exception e) {
            log.warn("[도메인/제목 추출 실패] {}", e.getMessage());
        }

        return new ParsedPageInfo(domain, title);
    }

    public static record ParsedPageInfo(String domain, String title) {}
}
