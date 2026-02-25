package com.umc.linkyou.oauth;

import com.umc.linkyou.config.security.jwt.JwtTokenProvider;
import com.umc.linkyou.domain.AuthAccount;
import com.umc.linkyou.domain.UserRefreshToken;
import com.umc.linkyou.domain.Users;
import com.umc.linkyou.domain.enums.Provider;
import com.umc.linkyou.domain.enums.UserStatus;
import com.umc.linkyou.oauth.utils.CustomOAuth2User;
import com.umc.linkyou.repository.UserRefreshTokenRepository;
import com.umc.linkyou.repository.authAccountRepository.AuthAccountRepository;
import com.umc.linkyou.repository.userRepository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * OAuth2 인증 성공 후 처리 플로우
 *
 * ① CustomOAuth2User에서 email, externalId, provider 추출 (소셜 프로필 기반)
 * ② provider null/blank 체크 → 실패 시 FAIL/INVALID_PROVIDER 리다이렉트
 * ③ Provider.valueOf()로 enum 변환 → 알 수 없는 provider면 FAIL/INVALID_PROVIDER 리다이렉트
 * ④ externalId null/blank 체크 → 실패 시 FAIL/INVALID_EXTERNAL_ID 리다이렉트
 * ⑤ (provider + externalId)로 AuthAccount 조회 → 없으면 FAIL/USER_NOT_FOUND 리다이렉트
 * ⑥ DB에서 확정된 Users 레코드 추출 → confirmedEmail 사용 (소셜 프로필 email 대신)
 * ⑦ UserStatus 분기
 *    - TEMP   : socialToken 발급 → 추가 프로필 입력 페이지로 리다이렉트
 *    - ACTIVE : accessToken + refreshToken 발급 → 1회용 code(30초 TTL)로 간접 전달
 *    - 그 외  : FAIL/INACTIVE_USER 리다이렉트
 * ⑧ 예외 발생 시 FAIL/TOKEN_GENERATION_FAILED 리다이렉트
 */

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final UserRefreshTokenRepository userRefreshTokenRepository;
    private final StringRedisTemplate stringRedisTemplate;
    private final AuthAccountRepository authAccountRepository;

    @Value("${app.deeplink.base-url}")
    private String deepLinkBaseUrl;

    @Value("${jwt.token.expiration.refresh}")
    private long refreshTtlMs;

    @Value("${jwt.hmac-secret}")
    private String hmacSecret;

    private static final long AUTH_CODE_TTL_SECONDS = 30L; // 1회용 코드 유효시간

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        CustomOAuth2User oAuth2User = (CustomOAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getEmail();
        String externalId = oAuth2User.getExternalId();
        String provider   = oAuth2User.getProvider();
        if (provider == null || provider.isBlank()) {
            log.warn("OAuth2SuccessHandler: provider is null or blank, email={}", email);
            response.sendRedirect(String.format(
                    "%s/auth?provider=unknown&result=FAIL&errorCode=INVALID_PROVIDER",
                    deepLinkBaseUrl));
            return;
        }
        Provider providerEnum;
        try {
            providerEnum = Provider.valueOf(provider);
        } catch (IllegalArgumentException e) {
            log.warn("OAuth2SuccessHandler: unknown provider={}, email={}", provider, email);
            response.sendRedirect(String.format(
                    "%s/auth?provider=%s&result=FAIL&errorCode=INVALID_PROVIDER",
                    deepLinkBaseUrl, provider));
            return;
        }
        if (externalId == null || externalId.isBlank()) {
            log.warn("OAuth2SuccessHandler: externalId is null or blank, provider={}, email={}", provider, email);
            response.sendRedirect(String.format(
                    "%s/auth?provider=%s&result=FAIL&errorCode=INVALID_EXTERNAL_ID",
                    deepLinkBaseUrl, provider));
            return;
        }

        Optional<AuthAccount> authAccountOpt = authAccountRepository
                .findByProviderAndExternalId(providerEnum, externalId);

        if (authAccountOpt.isEmpty()) {
            log.warn("AuthAccount not found: provider={}, externalId={}", provider, externalId);
            response.sendRedirect(String.format(
                    "%s/auth?provider=%s&result=FAIL&errorCode=USER_NOT_FOUND",
                    deepLinkBaseUrl, provider));
            return;
        }

        Users user = authAccountOpt.get().getUser();
        String confirmedEmail = user.getEmail();
        String redirectUrl;

        try {
            if (user.getStatus() == UserStatus.TEMP) {
                // TEMP: socialToken은 추가정보 입력용 → 보안 민감도 낮아서 기존 유지
                String socialToken = jwtTokenProvider.createAccessToken(confirmedEmail, provider);
                redirectUrl = String.format(
                        "%s/auth?provider=%s&result=SUCCESS&status=TEMP&socialToken=%s",
                        deepLinkBaseUrl, provider,
                        URLEncoder.encode(socialToken, StandardCharsets.UTF_8)
                );

            } else if (user.getStatus() == UserStatus.ACTIVE) {
                String accessToken  = jwtTokenProvider.createAccessToken(confirmedEmail, provider);
                String refreshToken = jwtTokenProvider.createRefreshToken(confirmedEmail);

                // refreshToken 화이트리스트 저장
                userRefreshTokenRepository.findByUserId(user.getId())
                        .ifPresent(t -> userRefreshTokenRepository.deleteById(t.getRefreshToken()));
                String id = hmac(jwtTokenProvider.normalizeStrict(refreshToken));
                userRefreshTokenRepository.save(
                        new UserRefreshToken(id, user.getId(), provider, refreshTtlMs) // provider = "KAKAO", "GOOGLE"
                );
                // 1회용 코드 생성 → Redis에 토큰 임시 저장 (30초 TTL)
                String code = UUID.randomUUID().toString().replace("-", "");
                String redisKey = "auth:code:" + code;
                String redisValue = accessToken + "::" + refreshToken;
                stringRedisTemplate.opsForValue().set(redisKey, redisValue, AUTH_CODE_TTL_SECONDS, TimeUnit.SECONDS);

                // URL엔 code만 노출
                redirectUrl = String.format(
                        "%s/auth?provider=%s&result=SUCCESS&status=ACTIVE&code=%s",
                        deepLinkBaseUrl, provider,
                        URLEncoder.encode(code, StandardCharsets.UTF_8)
                );

            } else {
                redirectUrl = String.format(
                        "%s/auth?provider=%s&result=FAIL&errorCode=INACTIVE_USER",
                        deepLinkBaseUrl, provider
                );
            }

        } catch (Exception e) {
            redirectUrl = String.format(
                    "%s/auth?provider=%s&result=FAIL&errorCode=TOKEN_GENERATION_FAILED",
                    deepLinkBaseUrl, provider
            );
        }

        response.sendRedirect(redirectUrl);
    }

    private String extractProvider(String requestUri) {
        if (requestUri == null) return "unknown";
        String[] parts = requestUri.split("/");
        return parts[parts.length - 1];
    }

    private String hmac(String token) {
        try {
            var mac = javax.crypto.Mac.getInstance("HmacSHA256");
            mac.init(new javax.crypto.spec.SecretKeySpec(
                    hmacSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return java.util.HexFormat.of().formatHex(mac.doFinal(token.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
