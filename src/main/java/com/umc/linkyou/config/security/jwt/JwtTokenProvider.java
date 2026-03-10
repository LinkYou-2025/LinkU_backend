package com.umc.linkyou.config.security.jwt;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.linkyou.apiPayload.exception.handler.UserHandler;
import com.umc.linkyou.domain.Users;
import com.umc.linkyou.repository.authAccountRepository.AuthAccountRepository;
import com.umc.linkyou.repository.userRepository.UserRepository;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import com.umc.linkyou.apiPayload.code.status.ErrorStatus;
import com.umc.linkyou.config.properties.Constants;
import com.umc.linkyou.config.properties.JwtProperties;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Base64;
import java.util.Date;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private final JwtProperties jwtProperties;

    private final UserRepository userRepository;
    private final AuthAccountRepository authAccountRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final RefreshTokenManager refreshTokenManager;

    @Value("${jwt.hmac-secret}")
    private String hmacSecret;

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(jwtProperties.getKeys().getAccess().getBytes());
    }

    public String generateToken(Authentication authentication) {
        String email = authentication.getName();

        return Jwts.builder()
                .setSubject(email)
                .claim("role", authentication.getAuthorities().iterator().next().getAuthority())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtProperties.getExpiration().getAccess()))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            validateAndParseAccess(token);
            return true;
        } catch (ExpiredJwtException e) {
            // AccessToken이 만료된 경우 예외를 던져서 필터에서 처리하게 함
            throw e;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public Authentication getAuthentication(String token) {
        Claims claims = validateAndParseAccess(token).getBody();
        String email = claims.getSubject();
        // String role = claims.get("role", String.class);
        String provider = claims.get("provider", String.class);

        // Users users = ... // 이메일로 Users 엔티티 조회
        Users users = authAccountRepository.findUserByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("해당 이메일을 가진 유저가 존재하지 않습니다: " + email));
        CustomUserDetails principal = new CustomUserDetails(users,provider);

        return new UsernamePasswordAuthenticationToken(principal, token, principal.getAuthorities());
    }

    public static String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(Constants.AUTH_HEADER);
        if(StringUtils.hasText(bearerToken) && bearerToken.startsWith(Constants.TOKEN_PREFIX)) {
            return bearerToken.substring(Constants.TOKEN_PREFIX.length());
        }
        return null;
    }

    public Authentication extractAuthentication(HttpServletRequest request){
        String accessToken = resolveToken(request);
        if(accessToken == null || !validateToken(accessToken)) {
            throw new UserHandler(ErrorStatus._INVALID_TOKEN);
        }
        return getAuthentication(accessToken);
    }

    // 리프레시 토큰 발급
    public String createRefreshToken(String subjectEmail) {
        return Jwts.builder()
                .setSubject(subjectEmail)
                .signWith(Keys.hmacShaKeyFor(jwtProperties.getKeys().getRefresh().getBytes()), SignatureAlgorithm.HS256)
                .setIssuer(jwtProperties.getIssuer())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtProperties.getExpiration().getRefresh()))
                .compact();
    }

    public String hmac(String token) {
        try {
            var mac = javax.crypto.Mac.getInstance("HmacSHA256");
            mac.init(new javax.crypto.spec.SecretKeySpec(hmacSecret.getBytes(StandardCharsets.UTF_8),  "HmacSHA256"));
            return java.util.HexFormat.of().formatHex(mac.doFinal(token.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) { throw new IllegalStateException(e); }
    }

    // 리프레시 토큰 유효성 검증 (Redis 화이트리스트: HMAC id 존재 여부)
    @Transactional(readOnly = true)
    public void validateRefreshToken(String refreshToken) {
        // 1) 정규화
        String raw = normalizeStrict(refreshToken);

        // 2) 서명/만료 검증 (여기서 JJWT가 검증함)
        Jws<Claims> jws = validateAndParseRefresh(raw);
        Claims claims = jws.getBody();
        String email = claims.getSubject();

        // 3) 토큰 소유자 userId 조회
        Long expectedUserId = authAccountRepository.findUserByEmail(email)
                .map(Users::getId)
                .orElseThrow(() -> new UserHandler(ErrorStatus._USER_NOT_FOUND));
        // 4) HMAC id 생성
        String id = hmac(raw);

        // 5) Redis Hash 화이트리스트에 키가 존재하는지 확인
        if (!refreshTokenManager.validateToken(expectedUserId, id)) {
            throw new UserHandler(ErrorStatus._INVALID_TOKEN);
        }
    }

    // 액세스 토큰 생성 (subject 기반)
    public String createAccessToken(String subject, String provider) {
        return Jwts.builder()
                .setSubject(subject)
                .claim("provider", provider)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtProperties.getExpiration().getAccess()))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    private Jws<Claims> parse(String token, Key key) {
        String cleanToken = normalizeStrict(token);
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(cleanToken);
    }

    private Key getAccessKey()  { return Keys.hmacShaKeyFor(jwtProperties.getKeys().getAccess().getBytes()); }
    private Key getRefreshKey() { return Keys.hmacShaKeyFor(jwtProperties.getKeys().getRefresh().getBytes()); }

    // 액세스 토큰 파싱/검증
    private Jws<Claims> validateAndParseAccess(String token) {
        String cleanToken = normalizeStrict(token);
        return parse(cleanToken, getAccessKey());
    }

    // 리프레시 토큰 파싱/검증
    public Jws<Claims> validateAndParseRefresh(String token) {
        String cleanToken = normalizeStrict(token);
        return parse(cleanToken, getRefreshKey());
    }

    public String normalizeStrict(String token) {
        if (token == null) return null;
        String t = token.trim();
        if (t.startsWith("Bearer ")) t = t.substring(7);
        // 공백/개행/탭/제어문자 제거
        return t.replaceAll("[\\r\\n\\t]", "");
    }

    // 액세스 토큰에서 subject 추출
    private String decodeJwtPayloadSubject(String token) throws JsonProcessingException {
        return objectMapper.readValue(
                new String(Base64.getDecoder().decode(token.split("\\.")[1]), StandardCharsets.UTF_8),
                Map.class
        ).get("sub").toString();
    }



}
