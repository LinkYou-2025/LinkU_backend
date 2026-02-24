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

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final UserRefreshTokenRepository userRefreshTokenRepository;
    private final StringRedisTemplate stringRedisTemplate;

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
        String requestUri = request.getRequestURI();
        String provider = extractProvider(requestUri);

        Optional<Users> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            response.sendRedirect(String.format(
                    "%s/auth?provider=%s&result=FAIL&errorCode=USER_NOT_FOUND",
                    deepLinkBaseUrl, provider));
            return;
        }

        Users user = userOpt.get();
        String redirectUrl;

        try {
            if (user.getStatus() == UserStatus.TEMP) {
                // TEMP: socialToken은 추가정보 입력용 → 보안 민감도 낮아서 기존 유지
                String socialToken = jwtTokenProvider.createAccessToken(email);
                redirectUrl = String.format(
                        "%s/auth?provider=%s&result=SUCCESS&status=TEMP&socialToken=%s",
                        deepLinkBaseUrl, provider,
                        URLEncoder.encode(socialToken, StandardCharsets.UTF_8)
                );

            } else if (user.getStatus() == UserStatus.ACTIVE) {
                String accessToken  = jwtTokenProvider.createAccessToken(email);
                String refreshToken = jwtTokenProvider.createRefreshToken(email);

                // refreshToken 화이트리스트 저장
                userRefreshTokenRepository.findByUserId(user.getId())
                        .ifPresent(t -> userRefreshTokenRepository.deleteById(t.getRefreshToken()));
                String id = hmac(jwtTokenProvider.normalizeStrict(refreshToken));
                userRefreshTokenRepository.save(new UserRefreshToken(id, user.getId(), refreshTtlMs));

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
