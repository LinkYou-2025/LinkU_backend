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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

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



        // User 조회 (방어적 처리)
        Optional<Users> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            // 로그 남기고 FAIL 리다이렉트
            log.warn("OAuth2SuccessHandler: userRepository.findByEmail returned empty for email={}, provider={}",
                    email, provider);

            String failUrl = String.format(
                    "%s/auth?provider=%s&result=FAIL&errorCode=USER_NOT_FOUND",
                    deepLinkBaseUrl,
                    provider
            );

            response.sendRedirect(failUrl);
            return;  // 예외 없이 종료
        }

        Users user = userOpt.get();
        String redirectUrl;

        try {
            if (user.getStatus() == UserStatus.TEMP) {
                String socialToken = jwtTokenProvider.createAccessToken(email);
                redirectUrl = String.format(
                        "%s/auth?provider=%s&result=SUCCESS&status=TEMP&socialToken=%s",
                        deepLinkBaseUrl, provider,
                        URLEncoder.encode(socialToken, StandardCharsets.UTF_8)
                );

            } else if (user.getStatus() == UserStatus.ACTIVE) {
                String accessToken  = jwtTokenProvider.createAccessToken(email);
                String refreshToken = jwtTokenProvider.createRefreshToken(email);

                userRefreshTokenRepository.findByUserId(user.getId())
                        .ifPresent(t -> userRefreshTokenRepository.deleteById(t.getRefreshToken()));

                String id = hmac(jwtTokenProvider.normalizeStrict(refreshToken));
                userRefreshTokenRepository.save(new UserRefreshToken(id, user.getId(), refreshTtlMs));

                redirectUrl = String.format(
                        "%s/auth?provider=%s&result=SUCCESS&status=ACTIVE&accessToken=%s&refreshToken=%s",
                        deepLinkBaseUrl, provider,
                        URLEncoder.encode(accessToken,  StandardCharsets.UTF_8),
                        URLEncoder.encode(refreshToken, StandardCharsets.UTF_8)
                );

            } else {
                redirectUrl = String.format(
                        "%s/auth?provider=%s&result=FAIL&errorCode=INACTIVE_USER",
                        deepLinkBaseUrl, provider
                );
            }

            log.info("OAuth2 딥링크 리다이렉트: {} (user={}, status={})",
                    redirectUrl.split("&accessToken")[0], email, user.getStatus());

        } catch (Exception e) {
            // 토큰 발급 / Redis 저장 중 예외 → 500 대신 FAIL 딥링크
            log.error("OAuth2SuccessHandler: token generation failed for email={}, provider={}", email, provider, e);
            redirectUrl = String.format(
                    "%s/auth?provider=%s&result=FAIL&errorCode=TOKEN_GENERATION_FAILED",
                    deepLinkBaseUrl, provider
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
                    hmacSecret.getBytes(StandardCharsets.UTF_8),  "HmacSHA256"));
            return java.util.HexFormat.of().formatHex(mac.doFinal(token.getBytes()));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
