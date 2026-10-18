package com.umc.linkyou.infra.net;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Set;

// 사용자 입력 URL로 나가는 outbound 요청의 스킴/포트/목적지 IP를 검증하는 SSRF 방어 게이트.
@Slf4j
@Component
public class SsrfGuard {

    public static final int MAX_REDIRECTS = 5;

    private static final Set<String> ALLOWED_SCHEMES = Set.of("http", "https");
    private static final Set<Integer> ALLOWED_PORTS = Set.of(80, 443);

    // 테스트에서만 loopback(로컬 HTTP 서버) 허용, 운영 빈은 항상 false.
    private final boolean allowLoopback;

    public SsrfGuard() {
        this(false);
    }

    SsrfGuard(boolean allowLoopback) {
        this.allowLoopback = allowLoopback;
    }

    // 테스트 전용 팩토리 - 운영 코드에서 호출 금지.
    public static SsrfGuard forTesting(boolean allowLoopback) {
        return new SsrfGuard(allowLoopback);
    }

    public static class BlockedException extends RuntimeException {
        public BlockedException(String message) {
            super(message);
        }
    }

    // URL을 파싱·검증해 반환한다. DNS 실패(UnknownHostException)는 차단이 아닌 일시 장애로 보고 그대로 던진다.
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

    // 80/443만 허용 (테스트용 loopback은 예외).
    private void assertPortAllowed(URL url) {
        if (allowLoopback && isLiteralLoopbackHost(url.getHost())) {
            return;
        }
        int effectivePort = url.getPort() == -1 ? url.getDefaultPort() : url.getPort();
        if (!ALLOWED_PORTS.contains(effectivePort)) {
            throw new BlockedException("허용되지 않은 포트: " + effectivePort);
        }
    }

    // DNS 조회 없이 문자열만으로 loopback 호스트인지 판단한다.
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

    // 호스트가 해석되는 모든 IP 중 하나라도 사설/예약 대역이면 차단한다.
    private void assertHostResolvesToPublicAddress(String host) throws UnknownHostException {
        if (host == null || host.isBlank()) {
            throw new BlockedException("호스트가 비어 있음");
        }
        String lookupHost = host;
        if (lookupHost.startsWith("[") && lookupHost.endsWith("]")) {
            lookupHost = lookupHost.substring(1, lookupHost.length() - 1);
        }

        InetAddress[] addresses = InetAddress.getAllByName(lookupHost);
        if (addresses.length == 0) {
            throw new BlockedException("DNS 조회 결과 없음: " + host);
        }

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
                || address.isLinkLocalAddress()   // 169.254.0.0/16, fe80::/10
                || address.isSiteLocalAddress()   // 10/8, 172.16/12, 192.168/16
                || address.isAnyLocalAddress()    // 0.0.0.0, ::
                || address.isMulticastAddress()) {// 224.0.0.0/4, ff00::/8
            return true;
        }
        return isIpv6UniqueLocal(address) || isIpv4ZeroPrefix(address);
    }

    // fc00::/7 (IPv6 Unique Local Address)
    private boolean isIpv6UniqueLocal(InetAddress address) {
        if (!(address instanceof Inet6Address)) {
            return false;
        }
        byte[] bytes = address.getAddress();
        return (bytes[0] & 0xfe) == 0xfc;
    }

    // 0.0.0.0/8
    private boolean isIpv4ZeroPrefix(InetAddress address) {
        byte[] bytes = address.getAddress();
        return bytes.length == 4 && bytes[0] == 0;
    }
}
