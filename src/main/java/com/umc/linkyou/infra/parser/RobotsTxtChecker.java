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
import java.util.regex.Pattern;

// title/body 크롤링이 공통으로 사용하는 robots.txt 검사기.
// Allow/Disallow 둘 다 반영하며, 더 구체적인(긴) 경로 규칙이 우선한다.
// 도메인(host+userAgent) 단위로 파싱 결과를 Caffeine에 캐싱해 같은 호스트에 대한 반복 조회가 매번 네트워크를 타지 않게 한다.
// robots.txt 자체도 SafeUrlFetcher를 거쳐야 사설/내부망 호스트에 대한 조회 시도가 막힌다.
@Slf4j
@Component
public class RobotsTxtChecker {

    // package-private: 테스트에서 parseRules()/isPathAllowed() 반환값을 직접 다루기 위함
    record Rule(String pattern, boolean isAllow) {}

    // 캐시 항목: 파싱된 규칙 + 이 항목에 적용할 TTL. robots.txt 조회에 성공했든 실패했든 결과를 캐시한다.
    // 실제 만료 판단은 Caffeine의 Expiry가 이 ttl 값을 읽어 처리한다(아래 생성자 참고).
    private record CachedRules(List<Rule> rules, Duration ttl) {}

    // robots.txt는 자주 바뀌지 않으므로 정상 응답은 길게 캐시한다.
    // package-private: 테스트에서 어떤 TTL이 선택됐는지 cachedTtlFor()로 비교하기 위함
    static final Duration SUCCESS_TTL = Duration.ofHours(24);
    // 타임아웃 등 일시적 fetch 실패는 짧게 캐시해서 곧 재시도되도록 한다.
    static final Duration FAILURE_TTL = Duration.ofMinutes(1);

    // 캐시 키: scheme://host(:port)|userAgent. maximumSize로 상한을 둬서 다양한 도메인이 계속 쌓여도
    // 메모리가 무한정 늘어나지 않게 한다(예전 ConcurrentHashMap 구현은 상한이 없었다).
    // 만료 시각 계산은 Caffeine 기본 ticker(실제 시스템 시간)를 그대로 쓴다 - 우리가 직접 시간을
    // 관리할 이유가 없고, "TTL이 지나면 실제로 만료되는지"는 Caffeine 자체가 이미 검증하는 영역이라
    // 우리 테스트에서 다시 검증할 필요도 없다(아래 cachedTtlFor() 참고).
    private final Cache<String, CachedRules> ruleCache;

    // 패턴 문자열 -> 컴파일된 정규식 캐시. rules 목록 자체는 도메인 단위로 캐시되지만,
    // isPathAllowed()가 같은 규칙 목록을 URL마다 반복 검사하면서 matches()도 매번 호출되므로,
    // 여기서 한 번 더 캐싱하지 않으면 동일한 패턴을 URL 검사마다 계속 재컴파일하게 된다.
    private final Cache<String, Pattern> compiledPatternCache;

    private final SafeUrlFetcher safeUrlFetcher;

    // 유일한 생성자: Spring이 자동으로 주입한다.
    // 테스트는 이 생성자에 직접 SafeUrlFetcher를 넘겨서 SSRF 정책을 통제한다.
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
                        return currentDuration; // 읽는다고 만료 시각을 늘리지 않는다(원래도 expireAfterWrite 방식이었음)
                    }
                })
                .build();
        this.compiledPatternCache = Caffeine.newBuilder().maximumSize(5_000).build();
    }

    public boolean isAllowed(String urlStr, String userAgent) {
        try {
            URL url = new URL(urlStr);
            String hostPart = hostPart(url);
            String cacheKey = cacheKey(url.getProtocol(), hostPart, userAgent);

            List<Rule> rules = resolveRules(url.getProtocol(), hostPart, userAgent, cacheKey);

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

        } catch (SsrfGuard.BlockedException e) {
            // 목적지가 사설/내부망 주소 등으로 판정되어 SSRF 정책에 의해 막힌 경우는
            // 네트워크 일시 장애와 달리 "의도적으로 막은 것"이므로 fail-open하지 않는다.
            log.warn("[robots.txt] SSRF 정책에 의해 차단됨: URL {}, 이유: {}", urlStr, e.getMessage());
            return false;
        } catch (Exception e) {
            log.warn("[robots.txt 검사 실패] URL: {}, 이유: {}", urlStr, e.getMessage());
            // 검사 실패 시 기본 허용
            return true;
        }
    }

    // getHost()는 포트를 포함하지 않으므로, 표준 포트(80/443)가 아닌 경우를 위해 명시적으로 붙여준다.
    private String hostPart(URL url) {
        return url.getPort() != -1 ? url.getHost() + ":" + url.getPort() : url.getHost();
    }

    // scheme까지 키에 포함시킨다 - host만 쓰면 http/https robots.txt가 서로 다를 수 있는데도
    // 같은 캐시 항목을 공유해서, 한쪽에서 조회한 규칙이 최대 SUCCESS_TTL(24h) 동안 다른 쪽에도
    // 잘못 적용될 수 있었다.
    private String cacheKey(String scheme, String hostPart, String userAgent) {
        return scheme + "://" + hostPart + "|" + userAgent;
    }

    // package-private: 테스트에서 특정 host/userAgent가 어떤 TTL(성공/실패)로 캐시됐는지 확인하기 위함.
    // Caffeine이 그 TTL이 지난 뒤 실제로 만료시키는지는 라이브러리 자체의 책임 영역이라 우리가 다시
    // 검증할 필요가 없고, 우리 코드가 책임질 부분은 "성공/실패에 맞는 TTL을 골라 캐시에 넣었는가"뿐이다.
    Duration cachedTtlFor(String urlStr, String userAgent) throws java.net.MalformedURLException {
        URL url = new URL(urlStr);
        String cacheKey = cacheKey(url.getProtocol(), hostPart(url), userAgent);
        CachedRules cached = ruleCache.getIfPresent(cacheKey);
        return cached != null ? cached.ttl() : null;
    }

    // 캐시에 유효한 항목이 있으면 그대로 쓰고, 없거나 만료됐으면 새로 fetch한다.
    // 중복 fetch 허용형: 동시에 같은 호스트가 만료 시점을 맞히면 여러 스레드가 각자 fetch할 수 있다(락 없음).
    private List<Rule> resolveRules(String protocol, String hostPart, String userAgent, String cacheKey) {
        CachedRules cached = ruleCache.getIfPresent(cacheKey);
        if (cached != null) {
            return cached.rules();
        }
        return fetchAndCache(protocol, hostPart, userAgent, cacheKey);
    }

    // robots.txt를 실제로 가져와 파싱하고, 성공/실패 여부에 따라 다른 TTL로 캐시에 저장한다.
    // SsrfGuard.BlockedException은 캐시하지 않고 그대로 위로 던져서 isAllowed()가 fail-closed 처리하게 한다.
    private List<Rule> fetchAndCache(String protocol, String hostPart, String userAgent, String cacheKey) {
        List<Rule> rules;
        Duration ttl;
        try {
            String robotsUrl = protocol + "://" + hostPart + "/robots.txt";
            HttpURLConnection conn = safeUrlFetcher.openConnection(robotsUrl, userAgent, 5000, 5000);
            try {
                int responseCode = conn.getResponseCode();
                if (responseCode != 200) {
                    // robots.txt 없으면 기본 허용 - 정상적으로 확인된 상태이므로 길게 캐시한다.
                    rules = List.of();
                } else {
                    try (BufferedReader reader = new BufferedReader(
                            new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                        rules = parseRules(reader, userAgent);
                    }
                }
            } finally {
                // 응답 코드와 무관하게 항상 닫는다 - robots.txt가 없는 사이트(흔함)에서
                // non-200 응답을 읽지 않고 그냥 두면 커넥션이 새는 문제가 있었다.
                conn.disconnect();
            }
            ttl = SUCCESS_TTL;
        } catch (SsrfGuard.BlockedException e) {
            throw e;
        } catch (Exception e) {
            log.warn("[robots.txt fetch 실패] host: {}, 이유: {}", hostPart, e.getMessage());
            // 검사 실패 시 기본 허용하되, 일시적 문제일 수 있으니 짧게만 캐시한다.
            rules = List.of();
            ttl = FAILURE_TTL;
        }

        ruleCache.put(cacheKey, new CachedRules(rules, ttl));
        return rules;
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
        Pattern compiled = compiledPatternCache.get(pattern, this::compilePattern);
        if (compiled == null) {
            return false;
        }
        return compiled.matcher(path).lookingAt();
    }

    // 컴파일 실패(사실상 거의 발생하지 않음 - toRegex()가 메타문자를 전부 이스케이프한다)는
    // null을 반환하며, Caffeine은 null 값을 캐싱하지 않으므로 다음 호출에서 다시 시도된다.
    private Pattern compilePattern(String pattern) {
        try {
            return Pattern.compile(toRegex(pattern));
        } catch (Exception e) {
            log.warn("[robots.txt] 패턴 변환 실패: {}, 이유: {}", pattern, e.getMessage());
            return null;
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
