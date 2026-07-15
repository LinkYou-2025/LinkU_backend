---
name: api-patterns
description: Use when writing or modifying Spring MVC controllers and REST endpoints. Covers ApiResponse<T> response wrapper, GeneralException + ErrorStatus for errors, @Operation/@Tag Swagger docs (mandatory on every endpoint), @CurrentUser for auth, request DTO validation. Trigger on any file ending in Controller.java or *Api.java, or when adding a new API route.
---

# API Patterns

## Response Wrapper

모든 엔드포인트는 `ApiResponse<T>`로 응답을 감싸야 한다.

```java
// com.umc.linkyou.apiPayload.ApiResponse

// 데이터 있을 때 (기본 SUCCESS 코드)
return ApiResponse.onSuccess(result);

// 커스텀 성공 코드 + 데이터
return ApiResponse.onSuccess(SuccessStatus._OK, result);

// 데이터 없을 때 (빈 Map 반환)
return ApiResponse.onSuccess(SuccessStatus._OK);

// 실패 응답 (ExceptionAdvice에서 주로 사용)
return ApiResponse.onFailure(ErrorStatus._BAD_REQUEST, null);
```

## Exception Handling

```java
// 도메인별 ErrorStatus enum 사용
throw new GeneralException(UserErrorStatus._USER_NOT_FOUND);
throw new GeneralException(ErrorStatus._BAD_REQUEST);

// 도메인별 ErrorStatus 정의 패턴
// com.umc.linkyou.apiPayload.code.status.{domain}.{Domain}ErrorStatus
@Getter
@AllArgsConstructor
public enum FolderErrorStatus implements BaseErrorCode {
    _FOLDER_NOT_FOUND(HttpStatus.NOT_FOUND, "FOLDER4041", "폴더를 찾을 수 없습니다."),
    _FOLDER_FORBIDDEN(HttpStatus.FORBIDDEN, "FOLDER4031", "폴더에 대한 권한이 없습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    @Override
    public ErrorReasonDTO getReason() { ... }

    @Override
    public ErrorReasonDTO getReasonHttpStatus() { ... }
}
```

## Controller 구조

두 가지 패턴 중 선택:

**패턴 A — 인터페이스 분리 (권장, Swagger 분리)**
```java
// web/api/FolderApi.java — Swagger 어노테이션만
@Tag(name = "folder-controller", description = "폴더 관련 API")
@RequestMapping("/v1/folders")
public interface FolderApi {

    @Operation(summary = "폴더 목록 조회", description = "내 폴더 전체를 반환합니다.")
    @GetMapping
    ResponseEntity<ApiResponse<FolderListResponseDTO>> getFolders(
        @CurrentUser CustomUserDetails userDetails
    );
}

// web/controller/FolderController.java — 구현만
@RestController
@RequiredArgsConstructor
public class FolderController implements FolderApi {

    private final FolderService folderService;

    @Override
    public ResponseEntity<ApiResponse<FolderListResponseDTO>> getFolders(
        @CurrentUser CustomUserDetails userDetails
    ) {
        Long userId = userDetails.getUserId();
        return ResponseEntity.ok(ApiResponse.onSuccess(folderService.getFolders(userId)));
    }
}
```

**패턴 B — 단순 컨트롤러 (소규모 도메인)**
```java
@Tag(name = "alarm-controller", description = "알림 관련 API")
@ApiV1
@RestController
@RequestMapping("/alarms")
@RequiredArgsConstructor
public class AlarmController {

    private final AlarmService alarmService;

    @Operation(summary = "알림 목록 조회", description = "내 알림을 최신순으로 반환합니다.")
    @GetMapping
    public ApiResponse<List<AlarmResponseDTO>> getAlarms(
        @CurrentUser CustomUserDetails userDetails
    ) {
        return ApiResponse.onSuccess(alarmService.getAlarms(userDetails.getUserId()));
    }
}
```

## 인증 파라미터

```java
// 인증 필수
@CurrentUser CustomUserDetails userDetails

// userId 추출
Long userId = userDetails.getUserId();

// @AuthenticationPrincipal 직접 사용 금지 — @CurrentUser 사용
```

## Request DTO 검증

```java
public record CreateFolderRequest(
    @NotBlank(message = "폴더 이름은 필수입니다.")
    String name,

    @Size(max = 200, message = "설명은 200자 이내여야 합니다.")
    String description
) {}
```

컨트롤러에서 `@Valid` 선언:
```java
public ApiResponse<FolderResponseDTO> createFolder(
    @CurrentUser CustomUserDetails userDetails,
    @Valid @RequestBody CreateFolderRequest request
) { ... }
```

## URL 규칙

- 버전 prefix: `@ApiV1` 어노테이션 (= `/v1`) 또는 직접 `/v1/` 명시
- 복수형 명사: `/v1/folders`, `/v1/alarms`
- 계층 관계: `/v1/folders/{folderId}/links`
- 액션: `/v1/auth/login`, `/v1/auth/logout`
