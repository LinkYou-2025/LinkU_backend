package com.umc.linkyou.jwt;

import com.umc.linkyou.apiPayload.code.status.user.UserErrorStatus;
import com.umc.linkyou.apiPayload.exception.handler.UserHandler;
import com.umc.linkyou.apiPayload.code.status.auth.AuthErrorStatus;
import com.umc.linkyou.domain.Users;
import com.umc.linkyou.domain.enums.Provider;
import com.umc.linkyou.domain.enums.Role;
import com.umc.linkyou.repository.authAccountRepository.AuthAccountRepository;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import com.umc.linkyou.config.properties.Constants;
import com.umc.linkyou.config.properties.JwtProperties;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.HexFormat;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private final JwtProperties jwtProperties;

    private final AuthAccountRepository authAccountRepository;

    private final RefreshTokenManager refreshTokenManager;

    @Value("${jwt.hmac-secret}")
    private String hmacSecret;

    private Key accessKey;
    private Key refreshKey;

    @PostConstruct
    public void init() {
        this.accessKey  = Keys.hmacShaKeyFor(jwtProperties.keys().access().getBytes(StandardCharsets.UTF_8));
        this.refreshKey = Keys.hmacShaKeyFor(jwtProperties.keys().refresh().getBytes(StandardCharsets.UTF_8));
    }

    // 액세스 토큰 생성 (subject 기반)
    public String createAccessToken(Long userId, String subject, String provider, Role role) {
        return createAccessToken(userId, subject, provider, role, null);
    }

    public String createAccessToken(Long userId, String subject, String provider, Role role, String sessionId) {
        JwtBuilder builder = Jwts.builder()
                .setSubject(subject)
                .claim("userId", userId)
                .claim("provider", provider)
                .claim("role", role.getAuthority())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtProperties.expiration().access()))
                .signWith(accessKey, SignatureAlgorithm.HS256);

        // sessionId가 null이 아니고 공백이 아닌 경우에만 claim에 추가
        if (sessionId != null && !sessionId.isBlank()) {
            builder.claim("sid", sessionId);
        }

        return builder.compact();
    }

    private static final String RECOVERY_PURPOSE = "RECOVER";
    private static final long RECOVERY_TOKEN_EXPIRATION_MS = 10 * 60 * 1000L; // 10분

    // 탈퇴 유예 기간 복구 전용 토큰 — 회원 복구 API 외 다른 엔드포인트에는 사용할 수 없음
    public String createRecoveryToken(Long userId, String subject, String provider, Role role) {
        return Jwts.builder()
                .setSubject(subject)
                .claim("userId", userId)
                .claim("provider", provider)
                .claim("role", role.getAuthority())
                .claim("purpose", RECOVERY_PURPOSE)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + RECOVERY_TOKEN_EXPIRATION_MS))
                .signWith(accessKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public boolean isRecoveryToken(String token) {
        Claims claims = validateAndParseAccess(token).getBody();
        return RECOVERY_PURPOSE.equals(claims.get("purpose", String.class));
    }

    // 클레임 기반으로 Authentication 구성 — DB 조회 없음
    public Authentication getAuthentication(String token) {
        Claims claims = validateAndParseAccess(token).getBody();
        Long userId     = claims.get("userId", Long.class);
        String provider = claims.get("provider", String.class);
        String roleStr  = claims.get("role", String.class);

        if (roleStr == null || roleStr.isBlank()) {
            throw new UserHandler(AuthErrorStatus.UNAUTHORIZED);
        }

        Role role;
        try {
            role = Role.fromAuthority(roleStr);
        } catch (IllegalArgumentException ex) {
            throw new UserHandler(AuthErrorStatus.UNAUTHORIZED);
        }

        CustomUserDetails principal = new CustomUserDetails(userId, role, provider);
        return new UsernamePasswordAuthenticationToken(principal, token, principal.getAuthorities());
    }

    public static String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(Constants.AUTH_HEADER);
        if(StringUtils.hasText(bearerToken) && bearerToken.startsWith(Constants.TOKEN_PREFIX)) {
            return bearerToken.substring(Constants.TOKEN_PREFIX.length());
        }
        return null;
    }


    // 리프레시 토큰 발급
    public String createRefreshToken(String subjectEmail, String provider) {
        return Jwts.builder()
                .setSubject(subjectEmail)
                .claim("provider", provider)
                .signWith(refreshKey, SignatureAlgorithm.HS256)
                .setIssuer(jwtProperties.issuer())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtProperties.expiration().refresh()))
                .compact();
    }

    public String hmac(String token) {
        try {
            var mac = javax.crypto.Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(hmacSecret.getBytes(StandardCharsets.UTF_8),  "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(token.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) { throw new IllegalStateException(e); }
    }

    // 리프레시 토큰 유효성 검증 (Redis 화이트리스트: HMAC id 존재 여부)
    @Transactional(readOnly = true)
    public void validateRefreshToken(String refreshToken, String deviceId) {
        // 1) 정규화
        String raw = normalizeStrict(refreshToken);

        // 2) 서명/만료 검증 (여기서 JJWT가 검증함)
        Jws<Claims> jws = validateAndParseRefresh(raw);
        Claims claims = jws.getBody();
        String email = claims.getSubject();

        String providerStr = claims.get("provider", String.class);  // String 그대로!

        if (providerStr == null || providerStr.isBlank()) {
            throw new UserHandler(AuthErrorStatus.INVALID_TOKEN);
        }

        // 3) 토큰 소유자 userId 조회
        Long expectedUserId = authAccountRepository.findUserByEmailAndProvider(email, Provider.valueOf(providerStr))
                .map(Users::getId)
                .orElseThrow(() -> new UserHandler(UserErrorStatus._USER_NOT_FOUND));
        // 4) HMAC id 생성
        String id = hmac(raw);

        // 5) Redis Hash 화이트리스트에 키가 존재하는지 확인
        if (!refreshTokenManager.validateToken(expectedUserId, providerStr, deviceId, id)) {
            throw new UserHandler(AuthErrorStatus.INVALID_TOKEN);
        }
    }


    private Jws<Claims> parse(String token, Key key) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token);
    }

    // 액세스 토큰 파싱/검증
    private Jws<Claims> validateAndParseAccess(String token) {
        return parse(normalizeStrict(token), accessKey);
    }

    // 리프레시 토큰 파싱/검증
    public Jws<Claims> validateAndParseRefresh(String token) {
        return parse(normalizeStrict(token), refreshKey);
    }

    public String normalizeStrict(String token) {
        if (token == null) return null;
        String t = token.trim();
        if (t.startsWith("Bearer ")) t = t.substring(7);
        return t.replaceAll("[\\r\\n\\t]", "");
    }

    public long getRemainingExpiryMs(String token) {
        Claims claims = validateAndParseAccess(normalizeStrict(token)).getBody();
        long expiry = claims.getExpiration().getTime() - System.currentTimeMillis();
        return Math.max(expiry, 0);
    }

    public long getAccessTokenExpiresAt(String token) {
        return validateAndParseAccess(normalizeStrict(token)).getBody().getExpiration().getTime();
    }

    public String getSessionId(String token) {
        return validateAndParseAccess(normalizeStrict(token)).getBody().get("sid", String.class);
    }



}
