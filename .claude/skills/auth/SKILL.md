---
name: auth
description: Use when working on JWT tokens, Apple/Kakao OAuth2 flows, @CurrentUser custom annotation + CurrentUserArgumentResolver, SecurityConfig, JwtAuthenticationFilter, or login/logout/withdraw/refresh endpoints. Never use @AuthenticationPrincipal directly — always @CurrentUser. Trigger on any auth-related controller, service, filter, or config.
---

# Auth Patterns

## 핵심 원칙

- `@AuthenticationPrincipal`을 **절대 직접 사용하지 않는다**. 항상 `@CurrentUser`를 사용한다.
- Auth 관련 예외는 `AuthErrorStatus`의 에러 코드를 사용한다.
- OAuth 외부 API 호출은 `infra/` 레이어에서 수행한다.

---

## @CurrentUser 어노테이션

`com.umc.linkyou.jwt.CurrentUser`

```java
// 인증 필수 엔드포인트
public ApiResponse<ProfileResponse> getProfile(
    @CurrentUser CustomUserDetails userDetails
) {
    Long userId = userDetails.getUserId();
    ...
}

// Optional 인증 (비회원도 접근 가능) — null 허용 시 명시적으로 처리
public ApiResponse<PublicData> getPublicData(
    @CurrentUser CustomUserDetails userDetails  // null이면 비회원
) {
    boolean isGuest = (userDetails == null);
    ...
}
```

## CustomUserDetails

```java
// JWT 인증 후 SecurityContext에 저장되는 객체
CustomUserDetails userDetails = ...;
Long userId = userDetails.getUserId();
String email = userDetails.getEmail();
Role role = userDetails.getRole();
```

## JWT 흐름

```
클라이언트 → JwtAuthenticationFilter
              → JwtTokenProvider.validateToken()
              → CustomUserDetailsService.loadUserByUsername()
              → SecurityContextHolder에 Authentication 저장
              → @CurrentUser로 컨트롤러에 주입
```

## 토큰 발급

```java
// TokenIssueService 사용
IssuedTokenPair tokens = tokenIssueService.issueTokenPair(
    userId, email, provider, role, deviceId, deviceType
);
// tokens.getAccessToken(), tokens.getRefreshToken()
```

## OAuth2 Provider

| Provider | 로그인 방식 | 클라이언트 클래스 | 특이사항 |
|---|---|---|---|
| Google | ID Token 검증 | `GoogleTokenVerifier` | `google-api-client` 라이브러리 사용, `_INVALID_ID_TOKEN` 에러 주의 |
| Kakao | Access Token → 유저 정보 조회 | `KakaoTokenClient` | `https://kapi.kakao.com/v2/user/me` 호출 |
| Naver | Access Token → 유저 정보 조회 | `NaverTokenClient` | 추후 지원 예정 |

```java
// OAuth 에러 코드
throw new GeneralException(AuthErrorStatus.UNAUTHORIZED);
throw new GeneralException(ErrorStatus._INVALID_ID_TOKEN);       // Google ID Token 검증 실패
throw new GeneralException(ErrorStatus._SOCIAL_UNSUPPORTED_PROVIDER);  // 미지원 provider
throw new GeneralException(ErrorStatus._SOCIAL_EMAIL_REQUIRED);  // 이메일 미제공
```

각 Provider 서비스는 `oauth2/mobile/service/` 하위에 위치:
- `GoogleMobileAuthService` — ID Token 검증 후 유저 조회/생성
- `KakaoMobileAuthService` — Access Token으로 유저 정보 조회 후 처리
- `NaverMobileAuthService` — Naver 지원 시 동일 패턴으로 구현

## SecurityConfig 패턴

```java
// 공개 엔드포인트 추가 시
.requestMatchers("/v1/auth/**").permitAll()
.requestMatchers("/v1/public/**").permitAll()
// 나머지는 인증 필요
.anyRequest().authenticated()
```

## Refresh Token 흐름

- Refresh Token은 Redis에 저장 (`RefreshTokenManager`)
- `deviceId` + `deviceType` 기준으로 토큰 관리 (멀티 디바이스 지원)
- 만료된 Refresh Token → `AuthErrorStatus._REFRESH_TOKEN_EXPIRED` 예외

## 토큰 블랙리스트

- 로그아웃 시 Access Token을 `AccessTokenBlackListManager`(Redis)에 등록
- JwtAuthenticationFilter에서 블랙리스트 확인 후 인증 거부
