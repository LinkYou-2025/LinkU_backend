package com.umc.linkyou.infra.parser;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

import java.net.URI;

@Slf4j
@Component
public class TitleDomainParser {
    // title을 추출하여 ai가 링크를 생성할 때 참조값으로 사용됨.
    public ParsedPageInfo parseUrl(String url) {
        String domain = null;
        try {
            domain = new URI(url).getHost();
        } catch (Exception e) {
            log.warn("[도메인 추출 실패] {}", e.getMessage());
        }

        String title = null;
        try {
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
            if (title != null) {
                title = title.replaceAll("[^ㄱ-ㅎㅏ-ㅣ가-힣a-zA-Z0-9\\s]", "");
            }
        } catch (Exception e) {
            log.warn("[도메인/제목 추출 실패] {}", e.getMessage());
        }

        return new ParsedPageInfo(domain, title);
    }

    public static record ParsedPageInfo(String domain, String title) {}
}
