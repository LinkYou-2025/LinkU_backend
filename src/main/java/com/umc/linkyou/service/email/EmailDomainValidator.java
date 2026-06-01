package com.umc.linkyou.service.email;

import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.InitialDirContext;
import java.util.Hashtable;
import java.util.Locale;

@Slf4j
@Component
public class EmailDomainValidator {

    public boolean isDeliverableAddress(String email) {
        if (email == null || email.isBlank()) return false;

        String domain = extractDomain(email);
        return domain != null && hasDnsRecord(domain);
    }

    private String extractDomain(String email) {
        try {
            InternetAddress address = new InternetAddress(email, true);
            address.validate();
            String addr = address.getAddress();
            return addr.substring(addr.indexOf('@') + 1).toLowerCase(Locale.ROOT);
        } catch (AddressException e) {
            return null;
        }
    }

    private boolean hasDnsRecord(String domain) {
        try {
            Hashtable<String, String> env = new Hashtable<>();
            env.put("java.naming.factory.initial", "com.sun.jndi.dns.DnsContextFactory");
            env.put("java.naming.provider.url", "dns:");
            InitialDirContext ctx = new InitialDirContext(env);

            Attributes mxAttrs = ctx.getAttributes(domain, new String[]{"MX"});
            if (mxAttrs.get("MX") != null) return true;

            Attributes aAttrs = ctx.getAttributes(domain, new String[]{"A", "AAAA"});
            return aAttrs.get("A") != null || aAttrs.get("AAAA") != null;
        } catch (NamingException e) {
            log.warn("이메일 도메인 DNS 조회 실패 domain={}", domain);
            return false;
        }
    }
}
