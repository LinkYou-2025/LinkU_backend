package com.umc.linkyou.infra.net;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Set;

/**
 * 사용자가 입력한 URL로 서버가 직접 나가는 모든 outbound 요청이 반드시 거쳐야 하는 SSRF 방어 게이트.
 * OWASP SSRF Prevention Cheat Sheet 권고를 따른다: 스킴/포트/목적지(IP) allowlist.
 * 리다이렉트 처리(매 hop 재검증)는 {@link SafeUrlFetcher}가 담당한다.
 *
 * 주의(TOCTOU/DNS rebinding): 여기서는 "검증 시점에 확인한 IP"와 "실제 연결 시점에 재조회되는 IP"가
 * 다를 수 있는 문제를 완전히 막지는 못한다. 완전히 막으려면 여기서 확인한 IP로 직접 소켓을 열고
 * Host 헤더만 원래 호스트명으로 보내는 방식(IP pinning)이 필요한데, Jsoup/HttpURLConnection 양쪽에
 * 그런 훅이 마땅치 않아 이번 범위에서는 검증 직후 곧바로 연결해 창을 최대한 좁히는 선에서 처리한다.
 */
@Slf4j
@Component
public class SsrfGuard {

    public static final int MAX_REDIRECTS = 5;

    private static final Set<String> ALLOWED_SCHEMES = Set.of("http", "https");
    private static final Set<Integer> ALLOWED_PORTS = Set.of(80, 443);

    // 테스트에서 로컬 HTTP 서버(127.0.0.1)를 쓸 수 있도록 하기 위한 예외. 운영 빈은 항상 false.
    private final boolean allowLoopback;

    public SsrfGuard() {
        this(false);
    }

    // package-private: 테스트 전용 생성자. infra.net 패키지 밖(예: infra.parser)의 테스트는
    // 생성자를 직접 못 부르므로 아래 forTesting() 팩토리를 통해서만 접근한다.
    SsrfGuard(boolean allowLoopback) {
        this.allowLoopback = allowLoopback;
    }

    /**
     * 테스트 전용 팩토리: 로컬 HTTP 서버(127.0.0.1/::1)를 대상으로 통합 테스트할 때만 사용한다.
     * 운영 코드에서는 절대 호출하지 말 것 - 운영 빈은 항상 기본 생성자({@link #SsrfGuard()})로 구성된다.
     */
    public static SsrfGuard forTesting(boolean allowLoopback) {
        return new SsrfGuard(allowLoopback);
    }

    public static class BlockedException extends RuntimeException {
        public BlockedException(String message) {
            super(message);
        }
    }

    // URL 문자열을 파싱하면서 동시에 안전성을 검증한다. 통과하면 URL 객체를 그대로 돌려준다.
    // UnknownHostException은 일부러 그대로 던진다 - DNS가 안 풀리는 건 "위험해서 막은 것"이 아니라
    // 일시적 네트워크 문제일 수 있어서, 호출부가 기존처럼 실패로 처리(대개 fail-open)하게 둔다.
    public URL validate(String urlStr) throws UnknownHostException {
        URL url;
        try {
            url = new URL(urlStr);
        } catch (MalformedURLException e) {
            throw new BlockedException("URL 파싱 실패: " + urlStr);
        }
        assertSafe(url);
        return url;
    }

    public void assertSafe(URL url) throws UnknownHostException {
        assertSchemeAllowed(url);
        assertPortAllowed(url);
        assertHostResolvesToPublicAddress(url.getHost());
    }

    private void assertSchemeAllowed(URL url) {
        String scheme = url.getProtocol() == null ? "" : url.getProtocol().toLowerCase();
        if (!ALLOWED_SCHEMES.contains(scheme)) {
            throw new BlockedException("허용되지 않은 스킴: " + scheme);
        }
    }

    private void assertPortAllowed(URL url) {
        // allowLoopback(테스트 전용 플래그)이 켜져 있고 호스트가 리터럴 loopback이면 포트 제한을 건너뛴다.
        // 로컬 통합 테스트가 띄우는 HttpServer는 80/443에 바인딩할 권한이 없어 임의 포트를 쓰므로,
        // 이 예외가 없으면 forTesting(true)로 loopback IP 차단만 풀어도 포트 검사에서 다시 막힌다.
        // 운영 빈은 allowLoopback이 항상 false이므로 이 분기는 운영 경로에 영향을 주지 않는다.
        if (allowLoopback && isLiteralLoopbackHost(url.getHost())) {
            return;
        }
        int effectivePort = url.getPort() == -1 ? url.getDefaultPort() : url.getPort();
        if (!ALLOWED_PORTS.contains(effectivePort)) {
            throw new BlockedException("허용되지 않은 포트: " + effectivePort);
        }
    }

    // 문자열 그대로 localhost/127.0.0.1/::1 형태인지만 확인한다(DNS 조회 없이 저비용으로).
    // 실제 사설/예약 IP 여부의 최종 판정은 뒤이은 assertHostResolvesToPublicAddress()가 담당한다.
    private boolean isLiteralLoopbackHost(String host) {
        if (host == null || host.isBlank()) {
            return false;
        }
        String h = host;
        if (h.startsWith("[") && h.endsWith("]")) {
            h = h.substring(1, h.length() - 1);
        }
        return h.equals("localhost") || h.equals("127.0.0.1") || h.equals("::1") || h.startsWith("127.");
    }

    private void assertHostResolvesToPublicAddress(String host) throws UnknownHostException {
        if (host == null || host.isBlank()) {
            throw new BlockedException("호스트가 비어 있음");
        }
        // URL#getHost()는 IPv6 리터럴을 대괄호 포함 형태("[::1]")로 돌려주는데,
        // InetAddress는 대괄호 없는 형태만 인식하므로 조회 전에 벗겨준다.
        String lookupHost = host;
        if (lookupHost.startsWith("[") && lookupHost.endsWith("]")) {
            lookupHost = lookupHost.substring(1, lookupHost.length() - 1);
        }

        // DNS 조회 실패(UnknownHostException)는 여기서 잡지 않고 그대로 던진다.
        InetAddress[] addresses = InetAddress.getAllByName(lookupHost);
        if (addresses.length == 0) {
            throw new BlockedException("DNS 조회 결과 없음: " + host);
        }

        // A/AAAA 레코드가 여러 개면 그중 하나라도 사설/예약 대역이면 전체를 차단한다(DNS rebinding 완화).
        for (InetAddress address : addresses) {
            if (isBlockedAddress(address)) {
                throw new BlockedException(
                        "사설/예약 IP 대역으로 판정되어 차단: " + host + " -> " + address.getHostAddress());
            }
        }
    }

    private boolean isBlockedAddress(InetAddress address) {
        if (allowLoopback && address.isLoopbackAddress()) {
            return false;
        }
        if (address.isLoopbackAddress()          // 127.0.0.0/8, ::1
                || address.isLinkLocalAddress()   // 169.254.0.0/16 (AWS 메타데이터 포함), fe80::/10
                || address.isSiteLocalAddress()   // 10/8, 172.16/12, 192.168/16
                || address.isAnyLocalAddress()    // 0.0.0.0, ::
                || address.isMulticastAddress()) {// 224.0.0.0/4, ff00::/8
            return true;
        }
        return isIpv6UniqueLocal(address) || isIpv4ZeroPrefix(address);
    }

    // fc00::/7 (Unique Local Address) - InetAddress#isSiteLocalAddress()는 IPv6는 구식 fec0::/10만 걸러서 별도 처리 필요.
    private boolean isIpv6UniqueLocal(InetAddress address) {
        if (!(address instanceof Inet6Address)) {
            return false;
        }
        byte[] bytes = address.getAddress();
        return (bytes[0] & 0xfe) == 0xfc;
    }

    // 0.0.0.0/8 - isAnyLocalAddress()는 정확히 0.0.0.0(와일드카드)만 잡고 0.0.0.1 같은 나머지는 안 잡는다.
    private boolean isIpv4ZeroPrefix(InetAddress address) {
        byte[] bytes = address.getAddress();
        return bytes.length == 4 && bytes[0] == 0;
    }
}
