package com.umc.linkyou.service.email;

import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.naming.NameNotFoundException;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.InitialDirContext;
import java.util.Hashtable;
import java.util.Locale;

/**
 * 이메일 도메인 유효성 검사
 */
@Slf4j
@Component
public class EmailDomainValidator {

    private static final String DNS_TIMEOUT_MS = "3000";
    private static final String DNS_RETRIES = "1";

    // 전체 email 도메인 검증 로직, 서비스에서 따로 예외를 던지도록 설정
    public boolean isDeliverableAddress(String email) throws AddressException {
        if (email == null || email.isBlank()) return false;
        String domain = extractDomain(email);
        return hasDnsRecord(domain);
    }

    private String extractDomain(String email) throws AddressException {
        InternetAddress address = new InternetAddress(email, true);
        address.validate();
        String addr = address.getAddress();
        return addr.substring(addr.indexOf('@') + 1).toLowerCase(Locale.ROOT);
    }

    private boolean hasDnsRecord(String domain) {
        Hashtable<String, String> env = new Hashtable<>();
        env.put("java.naming.factory.initial", "com.sun.jndi.dns.DnsContextFactory");
        env.put("java.naming.provider.url", "dns:");
        env.put("com.sun.jndi.dns.timeout.initial", DNS_TIMEOUT_MS);
        env.put("com.sun.jndi.dns.timeout.retries", DNS_RETRIES);

        InitialDirContext ctx = null;
        try {
            ctx = new InitialDirContext(env);
            Attributes mxAttrs = ctx.getAttributes(domain, new String[]{"MX"});
            if (mxAttrs.get("MX") != null) return true;

            Attributes aAttrs = ctx.getAttributes(domain, new String[]{"A", "AAAA"});
            return aAttrs.get("A") != null || aAttrs.get("AAAA") != null;
        } catch (NameNotFoundException e) {
            // 도메인이 존재하지 않음 (NXDOMAIN)
            return false;
        } catch (NamingException e) {
            // timeout 등 일시적 인프라 오류 — 검증 불가이므로 통과 처리
            log.warn("이메일 도메인 DNS 조회 실패, fail-open 처리 domain={}", domain);
            return true;
        } finally {
            if (ctx != null) {
                try { ctx.close(); } catch (NamingException ignored) {}
            }
        }
    }
}
