package com.umc.linkyou.infra.parser;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

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
        boolean firstLine = true;

        // robots.txt는 UTF-8 텍스트 파일이 규격이므로 플랫폼 기본 인코딩에 의존하지 않고 명시한다.
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            String rawLine;
            while ((rawLine = reader.readLine()) != null) {
                if (firstLine) {
                    rawLine = stripBom(rawLine);
                    firstLine = false;
                }

                String line = rawLine.trim();

                if (line.isEmpty()) {
                    applicableUserAgent = false; // 빈 줄은 그룹 구분자
                    continue;
                }

                // 인라인 주석 제거: '#' 이후는 전부 무시
                int hashIdx = line.indexOf('#');
                if (hashIdx >= 0) {
                    line = line.substring(0, hashIdx).trim();
                }
                if (line.isEmpty()) {
                    continue; // 주석만 있던 줄
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

    // UTF-8 BOM(EF BB BF, 디코딩 시 U+FEFF)이 첫 줄 앞에 붙어 있으면 제거한다.
    // String.trim()은 U+FEFF를 공백으로 취급하지 않아 별도 처리가 필요하다.
    private static final char BOM = '\uFEFF';

    private String stripBom(String line) {
        if (!line.isEmpty() && line.charAt(0) == BOM) {
            return line.substring(1);
        }
        return line;
    }

    // 매칭되는 규칙 중 가장 구체적인(긴) 패턴이 우선한다. (Google robots.txt 스펙과 동일한 방식)
    // 길이가 같으면 Allow가 우선한다(충돌 시 가장 제한이 적은 규칙을 사용).
    private boolean isPathAllowed(String path, List<Rule> rules) {
        Rule best = null;
        for (Rule rule : rules) {
            if (!matches(path, rule.pattern())) {
                continue;
            }
            if (best == null) {
                best = rule;
                continue;
            }
            int lengthDiff = rule.pattern().length() - best.pattern().length();
            if (lengthDiff > 0 || (lengthDiff == 0 && rule.isAllow() && !best.isAllow())) {
                best = rule;
            }
        }
        // 매칭되는 규칙이 하나도 없으면 기본 허용
        return best == null || best.isAllow();
    }

    // '*'는 0개 이상의 임의 문자, 패턴 끝의 '$'는 URL의 끝을 의미한다. (Google robots.txt 와일드카드 규칙)
    private static final String REGEX_METACHARACTERS = ".^$|?+()[]{}\\";

    private boolean matches(String path, String pattern) {
        try {
            return Pattern.compile(toRegex(pattern)).matcher(path).lookingAt();
        } catch (Exception e) {
            log.warn("[robots.txt] 패턴 변환 실패: {}, 이유: {}", pattern, e.getMessage());
            return false;
        }
    }

    // robots.txt 경로 패턴을 정규식으로 변환한다.
    private String toRegex(String pattern) {
        boolean endAnchor = pattern.endsWith("$");
        String body = endAnchor ? pattern.substring(0, pattern.length() - 1) : pattern;

        StringBuilder regex = new StringBuilder("^");
        for (int i = 0; i < body.length(); i++) {
            char c = body.charAt(i);
            if (c == '*') {
                regex.append(".*");
            } else if (REGEX_METACHARACTERS.indexOf(c) >= 0) {
                regex.append('\\').append(c);
            } else {
                regex.append(c);
            }
        }
        if (endAnchor) {
            regex.append('$');
        }
        return regex.toString();
    }
}
