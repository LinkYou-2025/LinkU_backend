package com.umc.linkyou.infra.parser;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import com.umc.linkyou.infra.net.SafeUrlFetcher;
import com.umc.linkyou.infra.net.SsrfGuard;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

// robots.txt를 조회·파싱해 URL 크롤링 허용 여부를 판단하고, 결과를 scheme+host+userAgent 단위로 캐싱한다.
@Slf4j
@Component
public class RobotsTxtChecker {

    record Rule(String pattern, boolean isAllow) {}

    private record CachedRules(List<Rule> rules, Duration ttl) {}

    static final Duration SUCCESS_TTL = Duration.ofHours(24);
    static final Duration FAILURE_TTL = Duration.ofMinutes(10);

    // robots.txt 조회 타임아웃 - 링크 저장 응답 지연을 막기 위해 짧게 둔다.
    private static final int ROBOTS_CONNECT_TIMEOUT_MS = 2000;
    private static final int ROBOTS_READ_TIMEOUT_MS = 2000;

    private final Cache<String, CachedRules> ruleCache;

    // robots 패턴 문자열 -> 컴파일된 정규식 캐시
    private final Cache<String, Pattern> compiledPatternCache;

    private final SafeUrlFetcher safeUrlFetcher;

    public RobotsTxtChecker(SafeUrlFetcher safeUrlFetcher) {
        this.safeUrlFetcher = safeUrlFetcher;
        this.ruleCache = Caffeine.newBuilder()
                .maximumSize(5_000)
                .expireAfter(new Expiry<String, CachedRules>() {
                    @Override
                    public long expireAfterCreate(String key, CachedRules value, long currentTime) {
                        return value.ttl().toNanos();
                    }

                    @Override
                    public long expireAfterUpdate(
                            String key, CachedRules value, long currentTime, long currentDuration) {
                        return value.ttl().toNanos();
                    }

                    @Override
                    public long expireAfterRead(
                            String key, CachedRules value, long currentTime, long currentDuration) {
                        return currentDuration;
                    }
                })
                .build();
        this.compiledPatternCache = Caffeine.newBuilder().maximumSize(5_000).build();
    }

    // 허용 여부 반환. SSRF 차단은 false(fail-closed), 그 외 오류는 true(fail-open).
    public boolean isAllowed(String urlStr, String userAgent) {
        long startedAt = System.nanoTime();
        boolean allowed = false;
        String cacheStatus = "unknown";
        try {
            URL url = new URL(urlStr);
            String hostPart = hostPart(url);
            String cacheKey = cacheKey(url.getProtocol(), hostPart, userAgent);
            cacheStatus = ruleCache.getIfPresent(cacheKey) != null ? "hit" : "miss";

            List<Rule> rules = resolveRules(url.getProtocol(), hostPart, userAgent, cacheKey);

            String path = url.getPath();
            if (path == null || path.isEmpty()) {
                path = "/";
            }
            String query = url.getQuery();
            String matchTarget = query != null ? path + "?" + query : path;

            allowed = isPathAllowed(matchTarget, rules);
            if (!allowed) {
                log.info("[robots.txt] URL {} is disallowed for userAgent {}", urlStr, userAgent);
            }
            return allowed;

        } catch (SsrfGuard.BlockedException e) {
            log.warn("[robots.txt] SSRF 정책에 의해 차단됨: URL {}, 이유: {}", urlStr, e.getMessage());
            return false;
        } catch (Exception e) {
            log.warn("[robots.txt 검사 실패] URL: {}, 이유: {}", urlStr, e.getMessage());
            allowed = true;
            return true;
        } finally {
            log.info("[링크 생성 시간] 단계=robots elapsedMs={} cache={} allowed={}",
                    TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt), cacheStatus, allowed);
        }
    }

    private String hostPart(URL url) {
        return url.getPort() != -1 ? url.getHost() + ":" + url.getPort() : url.getHost();
    }

    private String cacheKey(String scheme, String hostPart, String userAgent) {
        return scheme + "://" + hostPart + "|" + userAgent;
    }

    // 테스트용: 해당 URL의 캐시 항목에 적용된 TTL 조회
    Duration cachedTtlFor(String urlStr, String userAgent) throws java.net.MalformedURLException {
        URL url = new URL(urlStr);
        String cacheKey = cacheKey(url.getProtocol(), hostPart(url), userAgent);
        CachedRules cached = ruleCache.getIfPresent(cacheKey);
        return cached != null ? cached.ttl() : null;
    }

    private List<Rule> resolveRules(String protocol, String hostPart, String userAgent, String cacheKey) {
        CachedRules cached = ruleCache.getIfPresent(cacheKey);
        if (cached != null) {
            return cached.rules();
        }
        return fetchAndCache(protocol, hostPart, userAgent, cacheKey);
    }

    // robots.txt를 가져와 파싱하고 성공/실패에 따라 다른 TTL로 캐시한다. SSRF 차단은 캐시하지 않고 던진다.
    private List<Rule> fetchAndCache(String protocol, String hostPart, String userAgent, String cacheKey) {
        List<Rule> rules;
        Duration ttl;
        try {
            String robotsUrl = protocol + "://" + hostPart + "/robots.txt";
            HttpURLConnection conn = safeUrlFetcher.openConnection(
                    robotsUrl, userAgent, ROBOTS_CONNECT_TIMEOUT_MS, ROBOTS_READ_TIMEOUT_MS);
            try {
                int responseCode = conn.getResponseCode();
                if (responseCode != 200) {
                    rules = List.of();
                } else {
                    try (BufferedReader reader = new BufferedReader(
                            new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                        rules = parseRules(reader, userAgent);
                    }
                }
            } finally {
                conn.disconnect();
            }
            ttl = SUCCESS_TTL;
        } catch (SsrfGuard.BlockedException e) {
            throw e;
        } catch (Exception e) {
            log.warn("[robots.txt fetch 실패] host: {}, 이유: {}", hostPart, e.getMessage());
            rules = List.of();
            ttl = FAILURE_TTL;
        }

        ruleCache.put(cacheKey, new CachedRules(rules, ttl));
        return rules;
    }

    // robots.txt 본문에서 해당 userAgent에 적용되는 Allow/Disallow 규칙을 추출한다.
    List<Rule> parseRules(BufferedReader reader, String userAgent) throws Exception {
        List<Rule> rules = new ArrayList<>();
        boolean applicableUserAgent = false;
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
                applicableUserAgent = false;
                previousLineWasUserAgent = false;
                continue;
            }

            int hashIdx = line.indexOf('#');
            if (hashIdx >= 0) {
                line = line.substring(0, hashIdx).trim();
            }
            if (line.isEmpty()) {
                continue;
            }

            int colonIdx = line.indexOf(':');
            if (colonIdx < 0) {
                continue;
            }
            String field = line.substring(0, colonIdx).trim().toLowerCase();
            String value = line.substring(colonIdx + 1).trim();

            if (field.equals("user-agent")) {
                boolean matchesThis = value.equals("*") || value.equalsIgnoreCase(userAgent);
                // 연속된 User-agent 줄은 같은 그룹으로 OR 결합
                applicableUserAgent = previousLineWasUserAgent
                        ? (applicableUserAgent || matchesThis)
                        : matchesThis;
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
                }
            }
        }
        return rules;
    }

    private static final char BOM = '﻿';

    // 첫 줄의 UTF-8 BOM 제거
    String stripBom(String line) {
        if (!line.isEmpty() && line.charAt(0) == BOM) {
            return line.substring(1);
        }
        return line;
    }

    // 가장 긴(구체적인) 매칭 규칙이 우선, 길이가 같으면 Allow 우선, 매칭 없으면 허용.
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
        return best == null || best.isAllow();
    }

    private static final String REGEX_METACHARACTERS = ".^$|?+()[]{}\\";

    boolean matches(String path, String pattern) {
        Pattern compiled = compiledPatternCache.get(pattern, this::compilePattern);
        if (compiled == null) {
            return false;
        }
        return compiled.matcher(path).lookingAt();
    }

    private Pattern compilePattern(String pattern) {
        try {
            return Pattern.compile(toRegex(pattern));
        } catch (Exception e) {
            log.warn("[robots.txt] 패턴 변환 실패: {}, 이유: {}", pattern, e.getMessage());
            return null;
        }
    }

    // robots 패턴('*' 와일드카드, 끝의 '$' 앵커)을 정규식으로 변환한다.
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
