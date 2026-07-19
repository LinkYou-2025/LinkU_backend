package com.umc.linkyou.infra.parser;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

// title/body 크롤링이 공통으로 사용하는 robots.txt 검사기.
// Allow/Disallow 둘 다 반영하며, 더 구체적인(긴) 경로 규칙이 우선한다.
@Slf4j
@Component
public class RobotsTxtChecker {

    private record Rule(String pattern, boolean isAllow) {}

    public boolean isAllowed(String urlStr, String userAgent) {
        try {
            URL url = new URL(urlStr);
            String robotsUrl = url.getProtocol() + "://" + url.getHost() + "/robots.txt";

            HttpURLConnection conn = (HttpURLConnection) new URL(robotsUrl).openConnection();
            conn.setRequestProperty("User-Agent", userAgent);
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                // robots.txt 없으면 기본 허용
                return true;
            }

            List<Rule> rules = parseRules(conn, userAgent);
            String path = url.getPath();
            if (path == null || path.isEmpty()) {
                path = "/";
            }

            boolean allowed = isPathAllowed(path, rules);
            if (!allowed) {
                log.info("[robots.txt] URL {} is disallowed for userAgent {}", urlStr, userAgent);
            }
            return allowed;

        } catch (Exception e) {
            log.warn("[robots.txt 검사 실패] URL: {}, 이유: {}", urlStr, e.getMessage());
            // 검사 실패 시 기본 허용
            return true;
        }
    }

    private List<Rule> parseRules(HttpURLConnection conn, String userAgent) throws Exception {
        List<Rule> rules = new ArrayList<>();
        boolean applicableUserAgent = false;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();

                if (line.isEmpty()) {
                    applicableUserAgent = false;
                    continue;
                }
                if (line.startsWith("#")) {
                    continue;
                }

                int colonIdx = line.indexOf(':');
                if (colonIdx < 0) {
                    continue;
                }
                String field = line.substring(0, colonIdx).trim().toLowerCase();
                String value = line.substring(colonIdx + 1).trim();

                switch (field) {
                    case "user-agent" -> applicableUserAgent = value.equals("*") || value.equalsIgnoreCase(userAgent);
                    case "disallow" -> {
                        if (applicableUserAgent && !value.isEmpty()) {
                            rules.add(new Rule(value, false));
                        }
                    }
                    case "allow" -> {
                        if (applicableUserAgent && !value.isEmpty()) {
                            rules.add(new Rule(value, true));
                        }
                    }
                    default -> {
                        // sitemap 등 다른 필드는 무시
                    }
                }
            }
        }
        return rules;
    }

    // 매칭되는 규칙 중 가장 구체적인(긴) 패턴이 우선한다. (Google robots.txt 스펙과 동일한 방식)
    private boolean isPathAllowed(String path, List<Rule> rules) {
        Rule best = null;
        for (Rule rule : rules) {
            if (!matches(path, rule.pattern())) {
                continue;
            }
            if (best == null || rule.pattern().length() > best.pattern().length()) {
                best = rule;
            }
        }
        // 매칭되는 규칙이 하나도 없으면 기본 허용
        return best == null || best.isAllow();
    }

    private boolean matches(String path, String pattern) {
        if (pattern.endsWith("$")) {
            return path.equals(pattern.substring(0, pattern.length() - 1));
        }
        return path.startsWith(pattern);
    }
}
