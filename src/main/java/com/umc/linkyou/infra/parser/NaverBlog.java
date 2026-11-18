package com.umc.linkyou.infra.parser;

import java.net.URI;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

// PC 페이지는 본문이 iframe#mainFrame 안에 있고, 모바일 페이지는 본문이 바로 있다.
// 다른 네이버 서비스는 구조가 달라 기본 추출기를 쓴다.
public final class NaverBlog {

    private NaverBlog() {}

    private static final Set<String> BLOG_HOSTS = Set.of("blog.naver.com", "m.blog.naver.com");
    private static final String PC_BLOG_ORIGIN = "https://blog.naver.com";

    public static boolean isBlogUrl(String url) {
        String host = hostOf(url);
        return host != null && BLOG_HOSTS.contains(host);
    }

    // PC 페이지면 본문이 든 iframe 주소, 아니면 empty다.
    public static Optional<String> postFrameUrl(Document doc) {
        Element frame = doc.selectFirst("iframe#mainFrame");
        if (frame == null) {
            return Optional.empty();
        }
        String abs = frame.absUrl("src");
        if (!abs.isBlank()) {
            return Optional.of(abs);
        }
        // 문서에 base URI가 없으면 absUrl이 비어 직접 만든다.
        String src = frame.attr("src").trim();
        if (src.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(
                src.startsWith("http")
                        ? src
                        : PC_BLOG_ORIGIN + (src.startsWith("/") ? "" : "/") + src);
    }

    // 본문을 못 찾으면 body 전체 텍스트를 돌려준다.
    public static String extractPostBody(Document postDoc, String url) {
        String logNo = logNoOf(url);
        // 한 문서에 여러 글이 있을 수 있어 해당 글 영역을 먼저 찾는다.
        Elements containers =
                logNo != null
                        ? postDoc.select("#post-view" + logNo + " .se-main-container")
                        : new Elements();
        if (containers.isEmpty()) {
            containers = postDoc.select(".se-main-container");
        }
        if (!containers.isEmpty() && !containers.text().isBlank()) {
            return containers.text();
        }
        return postDoc.body().text();
    }

    // 쿼리의 logNo, 없으면 경로 마지막 숫자를 쓴다.
    static String logNoOf(String url) {
        if (url == null) {
            return null;
        }
        for (String pair : url.split("[?&#]")) {
            if (pair.startsWith("logNo=")) {
                return pair.substring("logNo=".length());
            }
        }
        try {
            String path = URI.create(url).getPath();
            if (path != null) {
                String last = path.substring(path.lastIndexOf('/') + 1);
                if (!last.isEmpty() && last.chars().allMatch(Character::isDigit)) {
                    return last;
                }
            }
        } catch (IllegalArgumentException ignored) {
            // URL 형식이 아니면 logNo 없이 처리
        }
        return null;
    }

    private static String hostOf(String url) {
        try {
            String host = URI.create(url).getHost();
            return host == null ? null : host.toLowerCase(Locale.ROOT);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
