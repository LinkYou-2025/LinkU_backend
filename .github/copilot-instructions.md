# Copilot Review Instructions (Spring Boot Backend)

## Language
- 모든 Pull Request 리뷰는 반드시 한국어로 작성한다.
- 코드 (클래스명, 메서드명, 변수명)는 영어 그대로 유지한다.
- 기술 용어는 필요 시 한국어 + 영어 병기한다.

## Review Style
- 문제점 → 이유 → 해결 방법 순서로 설명한다.
- 불필요한 칭찬 금지
- 가능하면 코드 예시 포함

## Focus (Spring Boot)
- Controller / Service / Repository 계층 분리 여부
- @Transactional 적절한 사용 여부
- 예외 처리 (GlobalExceptionHandler 등)
- DTO / Entity 분리 여부
- N+1 문제 (JPA fetch 전략)
- null 처리 및 Optional 사용
- API 응답 구조 일관성
- 보안 (인증/인가, 민감정보 노출)

## Avoid
- "Looks good", "Nice" 같은 의미 없는 리뷰 금지
