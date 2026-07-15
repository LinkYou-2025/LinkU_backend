# CLAUDE.md

This file provides guidance to Claude Code when working with code in this repository.

## Project Summary

**LinkU**는 링크 저장·공유·큐레이션 플랫폼의 백엔드 API입니다.
Java 17 + Spring Boot 3, PostgreSQL, Redis 기반이며 Apple/Kakao OAuth2, JWT 인증, AWS S3, FCM 푸시를 사용합니다.

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

## Skills (auto-discovery)

모든 skill은 `.claude/skills/<name>/SKILL.md`에 `description` frontmatter를 가지며,
현재 작업 컨텍스트와 description을 매칭해 관련 skill을 탐색합니다.

> **중요**: description 매칭으로 후보가 보여도 skill 본문은 자동 주입되지 않습니다.
> 작업 시작 전 반드시 `Skill` 도구로 해당 skill을 로드하세요.

| Skill | 언제 쓰이나 |
|---|---|
| `api-patterns` | `*Controller.java`, `*Api.java` 작성/수정, 신규 API 엔드포인트 |
| `auth` | JWT, OAuth2(Apple/Kakao), `@CurrentUser`, `SecurityConfig` |
| `testing` | `src/test/` 하위 작업, 새로운 테스트 작성 |
| `git-conventions` | 커밋/브랜치/PR/이슈 작성 |
| `domain-model` | Entity, DTO, Converter, ErrorStatus 추가/수정 |

## Critical Rules (훅이 차단하는 규칙)

아래 규칙은 `.claude/hooks/`가 **exit 2로 차단**합니다.

- **커밋 메시지**: Conventional Commits 형식 + 영문만 (`commit-msg-check.sh`)
- **`@AuthenticationPrincipal` 직접 사용 금지**: 반드시 `@CurrentUser` 사용 (`post-edit-dispatch.sh`)

## Other Always-Rules (코드 리뷰에서 확인)

훅이 강제하지 않는 컨벤션. 경고 수준으로만 알려줍니다.

- 모든 엔드포인트에 `@Operation` / `@Tag` Swagger 문서
- 응답은 `ApiResponse<T>`로 감쌀 것
- 예외는 `GeneralException(ErrorStatus.XXX)` 형태로 던질 것
- `@Value` 필드 직접 주입 지양 — `@ConfigurationProperties` 사용
- 서비스 메서드에 `@Transactional` 선언 (읽기 전용은 `readOnly = true`)

## Tech Foundation

Java 17, Spring Boot 3.4.7, JPA/Hibernate, Spotless(Google Java Format), JWT,
Google/Kakao OAuth2 (Naver 추가 예정), AWS S3, Firebase FCM, PostgreSQL, Redis, JaCoCo.

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

## CI/CD

| 워크플로우 | 트리거 | 내용 |
|---|---|---|
| `ci.yml` | PR → develop | Spotless + build + test + JaCoCo |
| `gradle.yml` | push → develop | Gradle 캐싱 검증 |

## Language Conventions

- **Commit messages**: 영문 (Conventional Commits, `commit-msg-check.sh` 훅이 강제)
- **API 응답**: 항상 `ApiResponse<T>` 래퍼 사용
- **인증**: `@CurrentUser CustomUserDetails userDetails` — `@AuthenticationPrincipal` 직접 사용 금지
