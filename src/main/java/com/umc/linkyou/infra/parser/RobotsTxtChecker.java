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

    // package-private: 테스트에서 parseRules()/isPathAllowed() 반환값을 직접 다루기 위함
    record Rule(String pattern, boolean isAllow) {}

    public boolean isAllowed(String urlStr, String userAgent) {
        try {
            URL url = new URL(urlStr);
            // getHost()는 포트를 포함하지 않으므로, 표준 포트(80/443)가 아닌 경우를 위해 명시적으로 붙여준다.
            String hostPart = url.getPort() != -1 ? url.getHost() + ":" + url.getPort() : url.getHost();
            String robotsUrl = url.getProtocol() + "://" + hostPart + "/robots.txt";

            HttpURLConnection conn = (HttpURLConnection) new URL(robotsUrl).openConnection();
            conn.setRequestProperty("User-Agent", userAgent);
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                // robots.txt 없으면 기본 허용
                return true;
            }

            List<Rule> rules;
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                rules = parseRules(reader, userAgent);
            }

            String path = url.getPath();
            if (path == null || path.isEmpty()) {
                path = "/";
            }
            // '$' 앵커가 URL의 진짜 끝(쿼리스트링 포함)을 기준으로 판단하도록 쿼리까지 매칭 대상에 포함한다.
            // 예: /*.xls$ 는 /cats.xls 는 막지만 /cats.xls?id=1 은 막지 않아야 한다.
            String query = url.getQuery();
            String matchTarget = query != null ? path + "?" + query : path;

            boolean allowed = isPathAllowed(matchTarget, rules);
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

    // package-private: 네트워크 I/O 없이 순수 파싱 로직만 테스트할 수 있도록 Reader를 직접 받는다.
    List<Rule> parseRules(BufferedReader reader, String userAgent) throws Exception {
        List<Rule> rules = new ArrayList<>();
        boolean applicableUserAgent = false;
        // 연속된 'User-agent:' 줄은 하나의 그룹으로 OR 결합되어야 한다(Google 스펙 "행 및 규칙 그룹").
        // 직전 줄이 User-agent였는지를 추적해서, 그룹 중간이면 결합하고 새 그룹이면 새로 판단한다.
        boolean previousLineWasUserAgent = false;
        boolean firstLine = true;

        String rawLine;
        while ((rawLine = reader.readLine()) != null) {
            if (firstLine) {
                rawLine = stripBom(rawLine);
                firstLine = false;
            }

            String line = rawLine.trim();

            if (line.isEmpty()) {
                applicableUserAgent = false; // 빈 줄은 그룹 구분자
                previousLineWasUserAgent = false;
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

            if (field.equals("user-agent")) {
                boolean matchesThis = value.equals("*") || value.equalsIgnoreCase(userAgent);
                applicableUserAgent = previousLineWasUserAgent
                        ? (applicableUserAgent || matchesThis) // 같은 그룹 내 추가 User-agent → OR 결합
                        : matchesThis;                          // 새 그룹 시작 → 새로 판단
                previousLineWasUserAgent = true;
                continue;
            }

            previousLineWasUserAgent = false;

            switch (field) {
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
        return rules;
    }

    // UTF-8 BOM(EF BB BF, 디코딩 시 U+FEFF)이 첫 줄 앞에 붙어 있으면 제거한다.
    // String.trim()은 U+FEFF를 공백으로 취급하지 않아 별도 처리가 필요하다.
    private static final char BOM = '\uFEFF';

    String stripBom(String line) {
        if (!line.isEmpty() && line.charAt(0) == BOM) {
            return line.substring(1);
        }
        return line;
    }

    // 매칭되는 규칙 중 가장 구체적인(긴) 패턴이 우선한다. (Google robots.txt 스펙과 동일한 방식)
    // 길이가 같으면 Allow가 우선한다(충돌 시 가장 제한이 적은 규칙을 사용).
    boolean isPathAllowed(String path, List<Rule> rules) {
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

    boolean matches(String path, String pattern) {
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
