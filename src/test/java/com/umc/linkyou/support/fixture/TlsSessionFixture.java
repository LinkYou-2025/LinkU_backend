package com.umc.linkyou.support.fixture;

import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.List;
import javax.net.ssl.SSLSession;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public final class TlsSessionFixture {
    private TlsSessionFixture() {}

    public static SSLSession withDnsSan(String dnsName) throws Exception {
        X509Certificate certificate = mock(X509Certificate.class);
        when(certificate.getSubjectAlternativeNames()).thenReturn(List.of(List.of(2, dnsName)));
        return session(certificate);
    }

    public static SSLSession withoutDnsSan() throws Exception {
        return session(mock(X509Certificate.class));
    }

    private static SSLSession session(X509Certificate certificate) throws Exception {
        SSLSession session = mock(SSLSession.class);
        when(session.getPeerCertificates()).thenReturn(new Certificate[]{certificate});
        return session;
    }
}
