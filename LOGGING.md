# 로깅 및 에러 처리 가이드

## 📋 목차
- [개요](#개요)
- [로깅 설정](#로깅-설정)
- [에러 로그 형식](#에러-로그-형식)
- [예외 타입별 로그](#예외-타입별-로그)
- [Docker 로그 확인 방법](#docker-로그-확인-방법)
- [에러 처리 구조](#에러-처리-구조)

---

## 개요

이 문서는 LinkU 백엔드 서버의 로깅 및 에러 처리 시스템에 대한 가이드입니다.  
모든 API 에러는 `ExceptionAdvice`를 통해 일관되게 처리되며, Docker 로그를 통해 확인할 수 있습니다.

---

## 로깅 설정

### application.properties

```properties
# Logging 설정
logging.level.root=INFO
logging.level.com.umc.linkyou=DEBUG
logging.level.org.springframework.web=INFO
logging.level.org.hibernate=INFO
logging.pattern.console=%d{yyyy-MM-dd HH:mm:ss} - %msg%n
logging.pattern.file=%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n
```

### 로깅 레벨 설명

- **root**: `INFO` - 전체 애플리케이션 기본 로그 레벨
- **com.umc.linkyou**: `DEBUG` - 애플리케이션 패키지 상세 로그
- **org.springframework.web**: `INFO` - Spring Web 관련 로그
- **org.hibernate**: `INFO` - Hibernate/JPA 관련 로그

### 로그 패턴

- **콘솔 패턴**: `yyyy-MM-dd HH:mm:ss - 메시지`
- **파일 패턴**: `yyyy-MM-dd HH:mm:ss [스레드] 레벨 로거 - 메시지`

---

## 에러 로그 형식

### 기본 형식

```
2024-01-15 14:30:25 - [태그] 상세 정보
예외 스택 트레이스
```

### 로그 태그 종류

| 태그 | 설명 | 발생 시점 |
|------|------|-----------|
| `[GeneralException]` | 비즈니스 로직 예외 | `GeneralException` 발생 시 |
| `[API Error]` | 일반 API 에러 | 내부 처리 메서드에서 |
| `[Validation Error]` | 요청 파라미터 검증 실패 | `ConstraintViolationException` 발생 시 |
| `[Method Argument Not Valid]` | 요청 바디 검증 실패 | `MethodArgumentNotValidException` 발생 시 |
| `[API Validation Error]` | 상세 검증 에러 | 검증 실패 상세 정보 포함 |
| `[API Constraint Error]` | 제약 조건 위반 | 제약 조건 검증 실패 시 |
| `[Unhandled Exception]` | 처리되지 않은 예외 | 예상치 못한 예외 발생 시 |

---

## 예외 타입별 로그

### 1. GeneralException (비즈니스 예외)

**발생 위치**: 서비스 레이어에서 비즈니스 로직 위반 시

**로그 형식**:
```
2024-01-15 14:30:25 - [GeneralException] Code: 4000, Message: AI Article not found, URI: /api/ai-article/123
com.umc.linkyou.apiPayload.exception.GeneralException: AI Article not found
    at com.umc.linkyou.service.aiArticle.AiArticleServiceImpl.showAiArticle(AiArticleServiceImpl.java:173)
    at com.umc.linkyou.web.controller.AiArticleController.getAiArticle(AiArticleController.java:45)
    ...
```

**포함 정보**:
- 에러 코드
- 에러 메시지
- 요청 URI
- 전체 스택 트레이스

**예시 코드**:
```java
throw new GeneralException(ErrorStatus._AI_ARTICLE_NOT_FOUND);
```

---

### 2. API Error (일반 API 에러)

**발생 위치**: `handleExceptionInternal` 메서드에서

**로그 형식**:
```
2024-01-15 14:30:25 - [API Error] URI: /api/ai-article/123, Method: GET, Code: 4000, Message: AI Article not found
com.umc.linkyou.apiPayload.exception.GeneralException: AI Article not found
    at ...
```

**포함 정보**:
- 요청 URI
- HTTP Method (GET, POST, PUT, DELETE 등)
- 에러 코드
- 에러 메시지
- 전체 스택 트레이스

---

### 3. Validation Error (요청 파라미터 검증 실패)

**발생 위치**: `@RequestParam`, `@PathVariable` 검증 실패 시

**로그 형식**:
```
2024-01-15 14:30:25 - [Validation Error] _BAD_REQUEST
jakarta.validation.ConstraintViolationException: ...
    at ...
```

**포함 정보**:
- 검증 실패 메시지
- 전체 스택 트레이스

**예시**:
```java
@GetMapping("/api/linku/{linkuId}")
public ResponseEntity<?> getLinku(
    @PathVariable @Positive Long linkuId  // 검증 실패 시
) { ... }
```

---

### 4. Method Argument Not Valid (요청 바디 검증 실패)

**발생 위치**: `@RequestBody` 검증 실패 시

**로그 형식**:
```
2024-01-15 14:30:25 - [Method Argument Not Valid] Errors: {userId=must not be null, linkuId=must be positive}
org.springframework.web.bind.MethodArgumentNotValidException: ...
    at ...
```

**포함 정보**:
- 검증 실패 필드와 메시지 (Map 형태)
- 전체 스택 트레이스

**예시**:
```java
@PostMapping("/api/linku")
public ResponseEntity<?> createLinku(
    @RequestBody @Valid LinkuRequestDTO request  // 검증 실패 시
) { ... }
```

---

### 5. API Validation Error (상세 검증 에러)

**발생 위치**: `handleExceptionInternalArgs` 메서드에서

**로그 형식**:
```
2024-01-15 14:30:25 - [API Validation Error] URI: /api/linku, Method: POST, Code: 4000, Message: Bad Request, Errors: {userId=must not be null}
org.springframework.web.bind.MethodArgumentNotValidException: ...
    at ...
```

**포함 정보**:
- 요청 URI
- HTTP Method
- 에러 코드
- 에러 메시지
- 검증 실패 필드 상세 정보
- 전체 스택 트레이스

---

### 6. API Constraint Error (제약 조건 위반)

**발생 위치**: `handleExceptionInternalConstraint` 메서드에서

**로그 형식**:
```
2024-01-15 14:30:25 - [API Constraint Error] URI: /api/linku, Method: POST, Code: 4000, Message: Bad Request
jakarta.validation.ConstraintViolationException: ...
    at ...
```

**포함 정보**:
- 요청 URI
- HTTP Method
- 에러 코드
- 에러 메시지
- 전체 스택 트레이스

---

### 7. Unhandled Exception (처리되지 않은 예외)

**발생 위치**: 예상치 못한 예외 발생 시

**로그 형식**:
```
2024-01-15 14:30:25 - [Unhandled Exception] NullPointerException: Cannot invoke "getIsAiExist()" because "usersLinku" is null
java.lang.NullPointerException: Cannot invoke "getIsAiExist()" because "usersLinku" is null
    at com.umc.linkyou.service.aiArticle.AiArticleServiceImpl.showAiArticle(AiArticleServiceImpl.java:180)
    at ...
```

**포함 정보**:
- 예외 타입 및 메시지
- 전체 스택 트레이스

**주의**: 이 예외는 `ErrorStatus._INTERNAL_SERVER_ERROR`로 처리됩니다.

---

## Docker 로그 확인 방법

### 기본 명령어

```bash
# 실시간 로그 확인 (follow 모드)
sudo docker logs -f [컨테이너명]

# 최근 100줄 로그 확인
sudo docker logs --tail 100 [컨테이너명]

# 특정 시간 이후 로그 확인
sudo docker logs --since 2024-01-15T14:00:00 [컨테이너명]

# 특정 시간 이전 로그 확인
sudo docker logs --until 2024-01-15T15:00:00 [컨테이너명]

# 타임스탬프 포함하여 로그 확인
sudo docker logs -t [컨테이너명]
```

### 에러 필터링

```bash
# 에러만 필터링해서 보기
sudo docker logs [컨테이너명] 2>&1 | grep -i "error\|exception"

# 특정 API 엔드포인트 에러만 보기
sudo docker logs [컨테이너명] 2>&1 | grep "/api/ai-article"

# GeneralException만 보기
sudo docker logs [컨테이너명] 2>&1 | grep "GeneralException"

# 최근 에러 50개만 보기
sudo docker logs [컨테이너명] 2>&1 | grep -i "error\|exception" | tail -50
```

### 로그 저장

```bash
# 로그를 파일로 저장
sudo docker logs [컨테이너명] > app.log 2>&1

# 에러만 파일로 저장
sudo docker logs [컨테이너명] 2>&1 | grep -i "error\|exception" > errors.log
```

---

## 에러 처리 구조

### ExceptionAdvice 클래스

모든 예외는 `ExceptionAdvice` 클래스에서 중앙 집중식으로 처리됩니다.

**위치**: `com.umc.linkyou.apiPayload.exception.ExceptionAdvice`

**주요 메서드**:

1. **`onThrowException`**: `GeneralException` 처리
2. **`validation`**: `ConstraintViolationException` 처리
3. **`handleMethodArgumentNotValid`**: `MethodArgumentNotValidException` 처리
4. **`exception`**: 처리되지 않은 모든 예외 처리

### 에러 응답 형식

모든 에러는 다음 형식으로 응답됩니다:

```json
{
  "isSuccess": false,
  "code": "4000",
  "message": "AI Article not found",
  "result": null
}
```

### ErrorStatus 코드

주요 에러 코드는 `ErrorStatus` enum에 정의되어 있습니다:

- `_BAD_REQUEST`: 잘못된 요청
- `_USER_NOT_FOUND`: 사용자를 찾을 수 없음
- `_AI_ARTICLE_NOT_FOUND`: AI Article을 찾을 수 없음
- `_INTERNAL_SERVER_ERROR`: 내부 서버 오류
- 기타 비즈니스 로직별 에러 코드

---

## 로그 모니터링 팁

### 1. 실시간 모니터링

```bash
# 실시간으로 에러만 모니터링
sudo docker logs -f [컨테이너명] 2>&1 | grep --line-buffered -i "error\|exception"
```

### 2. 특정 시간대 로그 분석

```bash
# 오늘 오후 2시 이후 에러 확인
sudo docker logs --since $(date -d 'today 14:00' +%Y-%m-%dT%H:%M:%S) [컨테이너명] 2>&1 | grep -i "error\|exception"
```

### 3. 에러 통계 확인

```bash
# 에러 타입별 개수 확인
sudo docker logs [컨테이너명] 2>&1 | grep -o "\[.*Exception\]" | sort | uniq -c

# 가장 많이 발생하는 에러 확인
sudo docker logs [컨테이너명] 2>&1 | grep "\[API Error\]" | awk -F'Code: ' '{print $2}' | awk '{print $1}' | sort | uniq -c | sort -rn | head -10
```

---

## 주의사항

1. **중복 로깅**: 일부 예외는 두 번 로그가 찍힐 수 있습니다 (예: `[GeneralException]`과 `[API Error]`). 이는 정상 동작입니다.

2. **로그 레벨**: 프로덕션 환경에서는 `logging.level.com.umc.linkyou=INFO`로 변경하는 것을 권장합니다.

3. **민감한 정보**: 로그에 비밀번호, 토큰 등 민감한 정보가 포함되지 않도록 주의하세요.

4. **로그 용량**: Docker 로그는 기본적으로 제한이 없으므로, 필요시 로그 로테이션 설정을 고려하세요.

---

## 문제 해결

### 로그가 출력되지 않는 경우

1. **로깅 레벨 확인**: `application.properties`의 로깅 레벨이 올바른지 확인
2. **Docker 컨테이너 재시작**: 설정 변경 후 컨테이너 재시작 필요
3. **stdout/stderr 확인**: Docker는 stdout/stderr로만 로그를 출력합니다

### 로그가 너무 많은 경우

1. **로깅 레벨 조정**: `DEBUG` → `INFO`로 변경
2. **특정 패키지만 로깅**: 필요한 패키지만 `DEBUG` 레벨 유지

---

## 참고 자료

- [Spring Boot Logging](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.logging)
- [SLF4J Documentation](http://www.slf4j.org/manual.html)
- [Docker Logs Documentation](https://docs.docker.com/engine/reference/commandline/logs/)

---

**문서 작성일**: 2024-01-15  
**최종 수정일**: 2024-01-15


