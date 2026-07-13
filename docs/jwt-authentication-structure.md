# JWT 인증 구조 리팩터링 — `UserDetailsService` 방식의 문제와 현재 구조

## 한 줄 요약

인증을 **필터가 없던 구조**에서 **`OncePerRequestFilter` 기반 무상태(stateless) 구조**로 바꾸고,
`UserDetailsService`가 **매 요청 DB를 조회해 `Users` 엔티티 전체를 로드**하던 방식을,
**토큰 클레임만으로 `userId`·`role`만 담아** 인증 주체를 구성하는 방식으로 전환했다.
컨트롤러는 이제 `SecurityContextHolder`에서 필요한 최소 정보(`userId`)만 꺼내 쓴다.

---

## 변경 전 — `UserDetailsService` + 엔티티 보관 방식의 문제

전형적인 "Spring Security + JWT" 예제는 대개 이렇게 동작한다.

```text
요청 → 필터 → 토큰에서 username(email) 추출
     → userDetailsService.loadUserByUsername(email)   // ← 매 요청 DB 조회
     → DB에서 Users 엔티티 전체 로드
     → SecurityContext에 엔티티 기반 principal 저장
```

### 문제 1. 매 요청마다 DB 조회 — JWT의 stateless 장점을 스스로 무너뜨림

JWT를 쓰는 이유는 **서버가 상태를 들고 있지 않아도** 토큰만으로 사용자를 식별하기 위함이다.
그런데 `loadUserByUsername`으로 요청마다 DB를 치면,
**이미 토큰 안에 들어 있는 식별 정보(userId, role)를 굳이 DB에서 다시 조회**하는 셈이다.
→ 트래픽이 늘수록 인증 자체가 DB 부하가 되고, 무상태의 이점이 사라진다.

### 문제 2. 인증에 필요 없는 `Users` 엔티티 전체를 로드·보관

인증/인가에 실제로 필요한 건 **"누구인가(userId)"**, **"무슨 권한인가(role)"** 정도다.
그런데 엔티티 전체를 principal로 들고 다니면:

- 비밀번호, 프로필, 연관 관계 등 **인증과 무관한 필드까지 메모리에 상주**한다.
- 영속성 컨텍스트 밖에서 다뤄지는 **detached 엔티티 / 지연 로딩(Lazy) / 직렬화** 문제에 노출된다.
- principal이 무거워져 컨텍스트가 비대해진다.

### 문제 3. 인증 처리 지점이 흩어짐

필터가 정돈되지 않은 구조에서는 인증 검증이 여러 곳에 흩어지거나
요청 경로마다 일관성이 깨지기 쉽다.
→ "모든 요청은 반드시 한 번, 같은 방식으로 인증을 거친다"는 보장이 없다.

---

## 변경 후 — 현재 구조

### 1) 단일 진입점: `OncePerRequestFilter`

모든 요청은 `JwtAuthenticationFilter`를 **정확히 한 번** 통과한다.
토큰이 없으면 그대로 통과(익명), 있으면 블랙리스트 확인 후 인증 주체를 구성한다.

```java
// JwtAuthenticationFilter
String token = JwtTokenProvider.resolveToken(request);
if (!StringUtils.hasText(token)) { filterChain.doFilter(request, response); return; }
if (accessTokenBlackListManager.isBlacklisted(token)) {
    throw new JwtException("Blacklisted access token");
}
Authentication authentication = jwtTokenProvider.getAuthentication(token);
SecurityContextHolder.getContext().setAuthentication(authentication);
filterChain.doFilter(request, response);
```

세션은 `STATELESS`로 고정해, 서버가 세션 상태를 들고 있지 않도록 했다.

```java
.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
.addFilterBefore(jwtExceptionFilter, UsernamePasswordAuthenticationFilter.class)
.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
```

### 2) DB 조회 없이 클레임만으로 인증 구성

`UserDetailsService`를 태우지 않고, **토큰 클레임(userId·role·provider)으로 바로 principal**을 만든다.
DB 접근이 **0회**다.

```java
// JwtTokenProvider — 클레임 기반, DB 조회 없음
public Authentication getAuthentication(String token) {
    Claims claims = validateAndParseAccess(token).getBody();
    Long userId     = claims.get("userId", Long.class);
    String provider = claims.get("provider", String.class);
    String roleStr  = claims.get("role", String.class);
    // ... role 검증
    CustomUserDetails principal = new CustomUserDetails(userId, role, provider);
    return new UsernamePasswordAuthenticationToken(principal, token, principal.getAuthorities());
}
```

### 3) `CustomUserDetails`는 엔티티가 아니라 최소 식별 정보만

principal에는 **`userId`, `role`, `provider`** 만 담는다. `Users` 엔티티를 들고 다니지 않는다.

```java
public class CustomUserDetails implements UserDetails {
    private final String password;
    private final Long userId;
    private final Role role;
    private final String provider;

    // 클레임 기반 (필터에서 DB 없이 토큰 클레임으로 구성)
    public CustomUserDetails(Long userId, Role role, String provider) { ... }

    // 엔티티 기반 (OAuth2 로그인 등 Users 객체가 이미 있을 때만)
    public CustomUserDetails(Users users, String provider) { ... }
}
```

> 생성자를 둘로 나눈 이유: **일반 요청 흐름은 클레임 기반(무조회)**,
> **OAuth2 최초 로그인처럼 이미 엔티티가 있는 경우만 엔티티 기반**으로 구성하기 위함.

### 4) 컨트롤러는 `SecurityContextHolder`에서 `userId`만 꺼낸다

컨트롤러가 요청 파라미터로 유저 정보를 넘겨받는 대신,
`@CurrentUser` 어노테이션 + ArgumentResolver로 **SecurityContext에 저장된 principal**을 주입한다.

```java
// CurrentUserArgumentResolver
Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
if (authentication == null
        || !(authentication.getPrincipal() instanceof CustomUserDetails userDetails)) {
    throw new GeneralException(AuthErrorStatus.UNAUTHORIZED);
}
return userDetails;
```

컨트롤러에서는 이렇게 `userId`만 뽑아 쓴다.

```java
public ApiResponse<...> getSomething(@CurrentUser CustomUserDetails user) {
    Long userId = user.getUserId();
    // 실제 엔티티가 필요한 시점에만 서비스 계층에서 조회
}
```

→ **인증 단계에서는 엔티티를 조회하지 않고**, 정말 엔티티가 필요한 비즈니스 로직에서만
서비스 계층이 `userId`로 조회한다. 조회 시점을 인증에서 분리했다.

---

## 무엇이 나아졌나

| 관점 | 변경 전 (`UserDetailsService` + 엔티티) | 변경 후 (필터 + 클레임 + userId) |
|------|--------------------------------------|-------------------------------|
| 인증 시 DB 조회 | **매 요청마다** `loadUserByUsername` | **0회** (클레임만으로 구성) |
| principal 크기 | `Users` 엔티티 전체 | `userId`·`role`·`provider`만 |
| 엔티티 부작용 | detached / Lazy / 직렬화 위험 | 없음 (엔티티를 안 들고 다님) |
| 인증 진입점 | 흩어짐 / 불명확 | `OncePerRequestFilter` 단일 진입점 |
| stateless | 사실상 깨짐(요청마다 DB) | **진짜 stateless** (세션 STATELESS) |
| 엔티티 조회 시점 | 인증 단계에 강제 로드 | **정말 필요한 로직에서만** 조회 |

**결론:** JWT를 쓰면서도 매 요청 DB를 치던 `UserDetailsService` 방식은
무상태의 이점을 스스로 무너뜨리고 불필요한 엔티티를 계속 들고 다니는 구조였다.
현재 구조는 **필터에서 클레임만으로 인증을 끝내고, principal에는 `userId` 등 최소 정보만 담아**,
DB 부하·엔티티 부작용을 제거하고 인증 경로를 일원화했다.

---

## 함께 챙긴 안전장치

- **블랙리스트**: 로그아웃/무효화된 액세스 토큰은 `AccessTokenBlackListManager`로 필터 단계에서 차단.
- **예외 필터 분리**: `JwtExceptionFilter`를 앞단에 둬 토큰 파싱/검증 예외를 일관된 응답으로 변환.
- **역할 계층**: `RoleHierarchy`로 `ADMIN > MANAGER > USER` 상위 역할이 하위 권한을 포함.
- **리프레시 토큰 검증**: Redis 화이트리스트(HMAC id 존재 여부)로 재발급 요청을 검증.

---

## 사용 기술

`Spring Security` · `OncePerRequestFilter` · `JWT (JJWT)` · `SecurityContextHolder` ·
`HandlerMethodArgumentResolver(@CurrentUser)` · `SessionCreationPolicy.STATELESS` · `RoleHierarchy`
