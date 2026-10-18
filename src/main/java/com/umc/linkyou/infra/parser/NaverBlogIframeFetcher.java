package com.umc.linkyou.infra.parser;

import com.umc.linkyou.infra.net.SafeUrlFetcher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.concurrent.TimeUnit;

// 네이버 블로그 iframe 문서를 한 번 가져와 본문/이미지 추출에서 재사용한다.
@Slf4j
@Component
@RequiredArgsConstructor
public class NaverBlogIframeFetcher {

    private final SafeUrlFetcher safeUrlFetcher;
    private final RobotsTxtChecker robotsTxtChecker;

    public enum Status {
        NOT_PRESENT,
        FETCHED,
        DISALLOWED,
        FAILED
    }

    public record Result(Status status, Document document) {
        public static Result notPresent() {
            return new Result(Status.NOT_PRESENT, null);
        }

        public static Result disallowed() {
            return new Result(Status.DISALLOWED, null);
        }

        public static Result failed() {
            return new Result(Status.FAILED, null);
        }

        public static Result fetched(Document document) {
            return new Result(Status.FETCHED, document);
        }

        public Result copyForAsyncUse() {
            return document == null ? this : new Result(status, document.clone());
        }
    }

    public Result fetchIfPresent(String pageUrl, Document outerDocument) {
        try {
            String host = URI.create(pageUrl).getHost();
            if (host == null || !("blog.naver.com".equalsIgnoreCase(host)
                    || "m.blog.naver.com".equalsIgnoreCase(host))) {
                return Result.notPresent();
            }
        } catch (Exception e) {
            return Result.notPresent();
        }

        Element frame = outerDocument == null ? null : outerDocument.selectFirst("iframe#mainFrame");
        if (frame == null) {
            return Result.notPresent();
        }

        String iframeUrl = frame.absUrl("src");
        if (iframeUrl.isBlank()) {
            log.warn("[네이버 iframe] iframe URL이 비어 있어 본문/이미지 추출을 건너뜁니다.");
            return Result.failed();
        }

        if (!robotsTxtChecker.isAllowed(iframeUrl, "Mozilla/5.0")) {
            log.info("[네이버 iframe] robots.txt에서 접근을 허용하지 않았습니다.");
            return Result.disallowed();
        }

        long startedAt = System.nanoTime();
        try {
            Document iframeDocument = safeUrlFetcher.fetchDocument(iframeUrl, "Mozilla/5.0", 15000);
            log.info("[링크 생성 시간] 단계=naver-iframe-fetch elapsedMs={} success=true",
                    TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt));
            return Result.fetched(iframeDocument);
        } catch (Exception e) {
            log.info("[링크 생성 시간] 단계=naver-iframe-fetch elapsedMs={} success=false",
                    TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt));
            log.warn("[네이버 iframe] iframe 페이지 fetch 실패: {}", e.getMessage());
            return Result.failed();
        }
    }
}
