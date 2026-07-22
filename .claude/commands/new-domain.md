---
description: Scaffold a new domain (Controller + Api interface + Service + DTO + Entity + Repository + Converter + ErrorStatus) following LinkU conventions.
argument-hint: "<도메인명 (영문 소문자, 예: bookmark)>"
allowed-tools: Read, Write, Edit, Bash, Glob, Grep
---

# new-domain

새로운 도메인을 프로젝트 컨벤션에 맞게 생성합니다.

## 사용법

```
/new-domain <도메인명>
```

예: `/new-domain bookmark`

## 실행 전 확인

기존 도메인(`alarm`, `curation`, `folder` 등)의 파일 구조를 반드시 참고한다.
각 단계마다 유사한 도메인의 실제 파일을 Read로 열어 패턴을 확인하고 그에 맞게 생성한다.

---

## 실행 절차

### 1. web/api — Swagger 인터페이스 (선택, 복잡한 도메인 권장)

`src/main/java/com/umc/linkyou/web/api/{Domain}Api.java`

```java
@Tag(name = "{domain}-controller", description = "{도메인 설명} 관련 API")
@RequestMapping("/v1/{domains}")
public interface {Domain}Api {

    @Operation(summary = "...", description = "...")
    @GetMapping
    ResponseEntity<ApiResponse<{Domain}ResponseDTO.{Domain}ListDTO>> getList(
        @CurrentUser CustomUserDetails userDetails
    );
}
```

### 2. web/controller — 컨트롤러 구현

`src/main/java/com/umc/linkyou/web/controller/{Domain}Controller.java`

```java
@ApiV1
@RestController
@RequiredArgsConstructor
public class {Domain}Controller implements {Domain}Api {

    private final {Domain}Service {domain}Service;

    @Override
    public ResponseEntity<ApiResponse<{Domain}ResponseDTO.{Domain}ListDTO>> getList(
        @CurrentUser CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(ApiResponse.onSuccess(
            {domain}Service.getList(userDetails.getUserId())
        ));
    }
}
```

> 소규모 도메인이면 Api 인터페이스 없이 Controller에 @Tag/@Operation 직접 선언도 가능.

### 3. web/dto — Request/Response DTO

`src/main/java/com/umc/linkyou/web/dto/{domain}/{Domain}ResponseDTO.java`
`src/main/java/com/umc/linkyou/web/dto/{domain}/{Domain}RequestDTO.java`

기존 `FolderResponseDTO`, `AlarmResponseDTO` 패턴을 참고해 중첩 클래스로 구성.

### 4. service — 서비스 레이어

`src/main/java/com/umc/linkyou/service/{Domain}Service.java`

```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class {Domain}Service {

    private final {Domain}Repository {domain}Repository;
    private final UserRepository userRepository;

    public {Domain}ResponseDTO.{Domain}ListDTO getList(Long userId) {
        List<{Domain}> list = {domain}Repository.findAllByUserId(userId);
        return {Domain}Converter.toListDTO(list);
    }

    @Transactional
    public {Domain}ResponseDTO.{Domain}ResultDTO create(Long userId, {Domain}RequestDTO.Create{Domain}DTO request) {
        Users user = userRepository.findById(userId)
            .orElseThrow(() -> new GeneralException(UserErrorStatus._USER_NOT_FOUND));
        {Domain} entity = {Domain}Converter.toEntity(request, user);
        return {Domain}Converter.toResultDTO({domain}Repository.save(entity));
    }
}
```

### 5. domain — JPA Entity

`src/main/java/com/umc/linkyou/domain/{Domain}.java`

BaseEntity 상속, `@NoArgsConstructor(access = PROTECTED)`, `@Builder` 필수.
상태 변경은 setter 대신 전용 메서드로.

### 6. repository — Repository 인터페이스

`src/main/java/com/umc/linkyou/repository/{domain}Repository/{Domain}Repository.java`

```java
public interface {Domain}Repository extends JpaRepository<{Domain}, Long> {
    List<{Domain}> findAllByUserIdOrderByCreatedAtDesc(Long userId);
}
```

### 7. converter — 변환 클래스

`src/main/java/com/umc/linkyou/converter/{Domain}Converter.java`

Entity ↔ DTO 변환 static 메서드만 포함. `domain-model` skill 참고.

### 8. apiPayload/code/status — ErrorStatus enum

`src/main/java/com/umc/linkyou/apiPayload/code/status/{domain}/{Domain}ErrorStatus.java`

```java
@Getter
@AllArgsConstructor
public enum {Domain}ErrorStatus implements BaseErrorCode {
    _{DOMAIN}_NOT_FOUND(HttpStatus.NOT_FOUND, "{DOMAIN}4041", "{도메인} 정보를 찾을 수 없습니다."),
    _{DOMAIN}_FORBIDDEN(HttpStatus.FORBIDDEN, "{DOMAIN}4031", "{도메인}에 대한 권한이 없습니다.");
    ...
}
```

---

## 생성 후 체크리스트

- [ ] Controller에 `@Tag`, `@Operation` 선언 확인
- [ ] `@AuthenticationPrincipal` 대신 `@CurrentUser` 사용 확인
- [ ] 응답이 `ApiResponse<T>` 래퍼로 감싸졌는지 확인
- [ ] 예외가 `GeneralException({Domain}ErrorStatus.XXX)` 형태인지 확인
- [ ] Repository가 `src/main/java/com/umc/linkyou/repository/{domain}Repository/` 하위에 위치하는지 확인
- [ ] 기본 통합 테스트 1개 이상 작성 (`testing` skill 참고)
