package com.umc.linkyou.oauth;

import com.umc.linkyou.config.security.jwt.JwtTokenProvider;
import com.umc.linkyou.domain.UserRefreshToken;
import com.umc.linkyou.domain.Users;
import com.umc.linkyou.domain.enums.UserStatus;
import com.umc.linkyou.oauth.utils.CustomOAuth2User;
import com.umc.linkyou.repository.UserRefreshTokenRepository;
import com.umc.linkyou.repository.userRepository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;         // ← 추가!
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final UserRefreshTokenRepository userRefreshTokenRepository;

    @Value("${app.deeplink.base-url}")
    private String deepLinkBaseUrl;

    @Value("${jwt.token.expiration.refresh}")
    private long refreshTtlMs;

    @Value("${jwt.hmac-secret}")
    private String hmacSecret;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        CustomOAuth2User oAuth2User = (CustomOAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getEmail();
        // provider 파라미터 추출 (session 또는 request-uri 기반)
        String requestUri = request.getRequestURI(); // /login/oauth2/code/kakao
        String provider = extractProvider(requestUri); // "kakao", "google", ...


        // User 조회
        Users user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("User not found: " + email));

        String redirectUrl;

        if (user.getStatus() == UserStatus.TEMP) {
            // TEMP: socialToken(accessToken)만 발급, refreshToken 없음
            String socialToken = jwtTokenProvider.createAccessToken(email);

            redirectUrl = String.format(
                    "%s/auth?provider=%s&result=SUCCESS&status=TEMP&socialToken=%s",
                    deepLinkBaseUrl,
                    provider,
                    URLEncoder.encode(socialToken, StandardCharsets.UTF_8)
            );

        } else if (user.getStatus() == UserStatus.ACTIVE) {
            // ACTIVE: accessToken + refreshToken 발급
            String accessToken  = jwtTokenProvider.createAccessToken(email);
            String refreshToken = jwtTokenProvider.createRefreshToken(email);

            // refreshToken 화이트리스트 저장 (기존 토큰 교체)
            userRefreshTokenRepository.findByUserId(user.getId())
                    .ifPresent(t -> userRefreshTokenRepository.deleteById(t.getRefreshToken()));

            String id = hmac(jwtTokenProvider.normalizeStrict(refreshToken));
            userRefreshTokenRepository.save(new UserRefreshToken(id, user.getId(), refreshTtlMs));

            redirectUrl = String.format(
                    "%s/auth?provider=%s&result=SUCCESS&status=ACTIVE&accessToken=%s&refreshToken=%s",
                    deepLinkBaseUrl,
                    provider,
                    URLEncoder.encode(accessToken,  StandardCharsets.UTF_8),
                    URLEncoder.encode(refreshToken, StandardCharsets.UTF_8)
            );

        } else {
            // INACTIVE 등 기타 상태 → FAIL 처리
            redirectUrl = String.format(
                    "%s/auth?provider=%s&result=FAIL&errorCode=INACTIVE_USER",
                    deepLinkBaseUrl,
                    provider
            );
        }

        log.info("OAuth2 딥링크 리다이렉트: {} (user={}, status={})",
                redirectUrl.split("&accessToken")[0], email, user.getStatus());
        response.sendRedirect(redirectUrl);
    }

    /** /login/oauth2/code/{provider} URI에서 provider 추출 */
    private String extractProvider(String requestUri) {
        if (requestUri == null) return "unknown";
        String[] parts = requestUri.split("/");
        return parts[parts.length - 1]; // 마지막 세그먼트
    }

    private String hmac(String token) {
        try {
            var mac = javax.crypto.Mac.getInstance("HmacSHA256");
            mac.init(new javax.crypto.spec.SecretKeySpec(
                    hmacSecret.getBytes(), "HmacSHA256"));
            return java.util.HexFormat.of().formatHex(mac.doFinal(token.getBytes()));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
