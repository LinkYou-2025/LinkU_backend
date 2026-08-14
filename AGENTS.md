# AGENTS.md

This file provides guidance to OpenAI Codex and other AI coding agents when working with code in this repository.

## Project Summary

**LinkU**는 링크 저장·공유·큐레이션 플랫폼의 백엔드 API입니다.
Java 17 + Spring Boot 3, PostgreSQL, Redis 기반이며 Google/Kakao OAuth2 (Naver 추가 예정), JWT 인증, AWS S3, FCM 푸시를 사용합니다.

## Essential Commands

```bash
./gradlew build -x test          # 컴파일 (테스트 제외)
./gradlew test                   # 전체 테스트 실행
./gradlew clean build            # 클린 빌드 + 테스트
./gradlew spotlessCheck          # 코드 포맷 검증
./gradlew spotlessApply          # 코드 포맷 자동 교정
./gradlew jacocoTestReport       # 커버리지 리포트 생성
./gradlew test jacocoTestReport --continue  # 테스트 + 커버리지
```

## Architecture

```
com.umc.linkyou/
├── web/
│   ├── controller/    # HTTP 진입점 (@RestController)
│   ├── api/           # Swagger 인터페이스 분리용 (interface)
│   └── dto/           # Request/Response DTO
├── service/           # 비즈니스 로직 (@Service, @Transactional)
├── domain/            # JPA Entity
├── repository/        # Spring Data JPA Repository
├── converter/         # Entity ↔ DTO 변환
├── apiPayload/        # ApiResponse, GeneralException, ErrorStatus
├── jwt/               # JWT 필터, @CurrentUser, CustomUserDetails
├── infra/             # 외부 시스템 연동 (AI, S3, FCM, parser)
└── config/            # Spring 설정 클래스
```

## Tech Foundation

Java 17, Spring Boot 3.4.7, JPA/Hibernate, Spotless(Google Java Format), JWT,
Google/Kakao OAuth2 (Naver 추가 예정), AWS S3, Firebase FCM, PostgreSQL, Redis, JaCoCo.

---

## Coding Rules

아래 규칙은 **반드시** 준수해야 합니다. 위반 시 코드 리뷰에서 반려됩니다.

### API 응답

모든 엔드포인트 응답은 `ApiResponse<T>`로 감싼다.

```java
return ApiResponse.onSuccess(result);                        // 기본 성공
return ApiResponse.onSuccess(SomeSuccessStatus._OK, result); // 커스텀 코드
return ApiResponse.onSuccess(SomeSuccessStatus._OK);         // 데이터 없을 때
```

### 예외 처리

```java
throw new GeneralException(UserErrorStatus._USER_NOT_FOUND);
throw new GeneralException(FolderErrorStatus._FOLDER_FORBIDDEN);
```

도메인별 `ErrorStatus` enum을 사용한다. `ErrorStatus` 없이 직접 예외 메시지를 던지지 않는다.

### 인증

```java
// ✅ 항상 @CurrentUser 사용
public ApiResponse<...> someEndpoint(@CurrentUser CustomUserDetails userDetails) { ... }

// ❌ @AuthenticationPrincipal 직접 사용 금지
public ApiResponse<...> someEndpoint(@AuthenticationPrincipal CustomUserDetails userDetails) { ... }
```

### Swagger 문서

모든 컨트롤러 클래스에 `@Tag`, 모든 엔드포인트 메서드에 `@Operation`을 선언한다.

```java
@Tag(name = "folder-controller", description = "폴더 관련 API")
public class FolderController {

    @Operation(summary = "폴더 목록 조회", description = "내 폴더 전체를 반환합니다.")
    @GetMapping
    public ApiResponse<...> getFolders(...) { ... }
}
```

### 설정 주입

```java
// ✅ @ConfigurationProperties 사용
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(String secret, long accessExpiry) {}

// ❌ @Value 필드 직접 주입 지양
@Value("${app.jwt.secret}")
private String secret;
```

### 트랜잭션

```java
@Service
@Transactional(readOnly = true)   // 클래스 레벨: 조회 기본
public class FolderService {

    @Transactional                // 메서드 레벨: 쓰기 작업만 별도 선언
    public FolderResponseDTO create(...) { ... }
}
```

### Entity 설계

```java
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class Folder extends BaseEntity {
    // setter 노출 금지 — 상태 변경은 전용 메서드로
    public void updateName(String name) { this.name = name; }

    // 생성 의도가 명확할 때 정적 팩터리 메서드 추가
    public static Folder create(String name, Users user) {
        return Folder.builder().name(name).user(user).build();
    }
}
```

### Converter

Entity ↔ DTO 변환은 반드시 `converter/` 패키지의 Converter 클래스에서만 수행한다.
컨트롤러나 서비스에서 직접 변환하지 않는다.

---

## Commit Convention

**Conventional Commits 형식, 한글 또는 영문을 사용한다.**

```
type(scope): short description
```

| 타입 | 사용 시점 |
|---|---|
| `feat` | 새로운 기능 |
| `fix` | 버그 수정 |
| `refactor` | 기능 변경 없는 리팩토링 |
| `test` | 테스트 추가/수정 |
| `docs` | 문서 |
| `chore` | 빌드/설정/의존성 |
| `style` | 포맷/공백 |

```
# ✅
feat(auth): Google OAuth2 로그인 추가
fix(folder): 링크가 없는 폴더 조회 시 NPE 해결
```

---

## Testing Rules

- 테스트 메서드명과 `@DisplayName`: **"~시 ~한다"** 형태 한국어 언더스코어
- `@Nested`로 성공/실패 그룹 분리
- `assertNotNull` 단독 사용 금지 → `assertEquals`로 구체적 값 검증
- 예외 검증 시 타입뿐 아니라 **에러코드**까지 확인
- `$.isSuccess`, `$.code`, `$.result` 까지 응답 포맷 검증

```java
// Controller Test — 필수 MockitoBean
@MockitoBean JwtTokenProvider jwtTokenProvider;
@MockitoBean AccessTokenBlackListManager accessTokenBlackListManager;

// 인증 — @WithCustomUser 사용
@WithCustomUser(userId = 1L)
void 정상_입력_시_생성_결과를_반환한다() throws Exception { ... }

// 서비스 Test — 예외 검증
UserHandler ex = assertThrows(UserHandler.class, () -> service.doSomething(99L));
assertEquals(UserErrorStatus._NOT_FOUND, ex.getCode());
```

테스트 데이터는 `fixture/` 패키지의 `XxxFixture` 클래스로 관리한다.

---

## Branch Naming

```
feat/short-description
fix/short-description
refactor/short-description
chore/short-description
```

Base 브랜치: `develop`
