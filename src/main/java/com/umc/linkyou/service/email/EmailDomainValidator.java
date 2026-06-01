package com.umc.linkyou.service.email;

import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.MXRecord;
import org.xbill.DNS.Record;
import org.xbill.DNS.TextParseException;
import org.xbill.DNS.Type;

import java.util.Locale;

/**
 * 이메일 도메인 유효성 검사
 */
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
            Record[] mx = new Lookup(domain, Type.MX).run();
            if (mx != null && mx.length > 0) return hasUsableMx(mx);

            Record[] a = new Lookup(domain, Type.A).run();
            if (a != null && a.length > 0) return true;

            Record[] aaaa = new Lookup(domain, Type.AAAA).run();
            return aaaa != null && aaaa.length > 0;
        } catch (TextParseException e) {
            log.warn("이메일 도메인 DNS 조회 실패 domain={}", domain);
            return false;
        }
    }

    private boolean hasUsableMx(Record[] records) {
        for (Record r : records) {
            MXRecord mx = (MXRecord) r;
            if (!mx.getTarget().toString().equals(".")) return true;
        }
        return false;
    }
}
