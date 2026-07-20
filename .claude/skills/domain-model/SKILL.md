---
name: domain-model
description: Use when adding or modifying JPA Entities, DTOs, Converters, ErrorStatus enums, or Repository interfaces. Covers Entity design conventions (BaseEntity, Lombok, JPA annotations), DTO/Record patterns, Converter class structure, and ErrorStatus code naming. Trigger when creating a new domain or touching domain/, web/dto/, converter/ packages.
---

# Domain Model Patterns

## Entity 설계

```java
@Entity
@Table(name = "folders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class Folder extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private Users user;

    // 상태 변경 메서드 — setter 직접 노출 금지
    public void updateName(String name) {
        this.name = name;
    }

    // 정적 팩터리 메서드 — 생성 의도를 명확히 할 때 @Builder 대신 사용
    public static Folder create(String name, String description, Users user) {
        return Folder.builder()
            .name(name)
            .description(description)
            .user(user)
            .build();
    }
}
```

### 정적 팩터리 메서드

`@Builder`는 필드가 많을 때 유용하지만, 필수 파라미터가 명확하거나 생성 로직이 있을 때는 정적 팩터리 메서드를 추가한다.

```java
// 컨버터에서 사용
Folder folder = Folder.create(request.name(), request.description(), user);

// 빌더와 병행 가능 — 둘 다 열어두되, 컨버터는 팩터리 메서드 통일 권장
```

### ON DELETE CASCADE

부모 엔티티 삭제 시 자식도 함께 삭제해야 하는 경우 두 가지 방법을 상황에 따라 선택한다.

**방법 A — JPA cascade (애플리케이션 레벨)**
```java
// 부모(Users) 쪽에 선언
@OneToMany(mappedBy = "user", cascade = CascadeType.REMOVE, orphanRemoval = true)
private List<Folder> folders = new ArrayList<>();
```
- JPA가 자식을 건별로 DELETE — 자식 수가 많으면 쿼리 수 증가
- `orphanRemoval = true`와 함께 쓰면 컬렉션에서 제거 시에도 삭제

**방법 B — DB 외래키 cascade (DB 레벨, 권장)**
```java
// 자식(Folder) 쪽 FK에 선언
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "user_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_folder_user"))
private Users user;
```
DDL에 `ON DELETE CASCADE` 직접 추가 (migration SQL):
```sql
ALTER TABLE folders
  ADD CONSTRAINT fk_folder_user
  FOREIGN KEY (user_id) REFERENCES users(id)
  ON DELETE CASCADE;
```
- DB가 단일 쿼리로 처리 → 대량 삭제 시 성능 우위
- JPA 영속성 컨텍스트는 모르므로, 삭제 후 같은 트랜잭션에서 자식 엔티티를 다시 조회하지 않도록 주의
```

### BaseEntity

모든 Entity는 `BaseEntity`를 상속한다:
```java
// createdAt, updatedAt 자동 관리
public abstract class BaseEntity {
    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
```

---

## DTO 패턴

### Request DTO

```java
// Record 선호 (Java 17+)
public record CreateFolderRequest(
    @NotBlank(message = "폴더 이름은 필수입니다.")
    @Size(max = 50, message = "이름은 50자 이내여야 합니다.")
    String name,

    @Size(max = 200)
    String description
) {}

// 또는 중첩 클래스 (기존 패턴과 일관성 유지 시)
public class FolderRequestDTO {
    @Getter
    public static class CreateFolderDTO {
        @NotBlank
        private String name;
    }
}
```

### Response DTO

```java
// 기존 프로젝트 패턴 (중첩 클래스)
public class FolderResponseDTO {

    @Builder
    @Getter
    @AllArgsConstructor
    public static class FolderResultDTO {
        Long folderId;
        String name;
        LocalDateTime createdAt;
    }

    @Builder
    @Getter
    @AllArgsConstructor
    public static class FolderListDTO {
        List<FolderResultDTO> folders;
        int totalCount;
    }
}
```

---

## Converter 패턴

Entity ↔ DTO 변환은 반드시 Converter 클래스에서 수행한다.
컨트롤러나 서비스에서 직접 변환하지 않는다.

```java
// converter/FolderConverter.java
public class FolderConverter {

    public static Folder toEntity(CreateFolderRequest request, Users user) {
        return Folder.builder()
            .name(request.name())
            .description(request.description())
            .user(user)
            .build();
    }

    public static FolderResponseDTO.FolderResultDTO toResultDTO(Folder folder) {
        return FolderResponseDTO.FolderResultDTO.builder()
            .folderId(folder.getId())
            .name(folder.getName())
            .createdAt(folder.getCreatedAt())
            .build();
    }

    public static FolderResponseDTO.FolderListDTO toListDTO(List<Folder> folders) {
        return FolderResponseDTO.FolderListDTO.builder()
            .folders(folders.stream().map(FolderConverter::toResultDTO).toList())
            .totalCount(folders.size())
            .build();
    }
}
```

---

## ErrorStatus 명명 규칙

도메인별로 별도 `ErrorStatus` enum을 만든다.

```java
// 파일 위치: apiPayload/code/status/{domain}/{Domain}ErrorStatus.java
@Getter
@AllArgsConstructor
public enum FolderErrorStatus implements BaseErrorCode {

    // 형식: _{DOMAIN}_{ERROR_TYPE}(HttpStatus, "DOMAIN코드번호", "메시지")
    _FOLDER_NOT_FOUND(HttpStatus.NOT_FOUND, "FOLDER4041", "폴더를 찾을 수 없습니다."),
    _FOLDER_FORBIDDEN(HttpStatus.FORBIDDEN, "FOLDER4031", "폴더에 대한 권한이 없습니다."),
    _FOLDER_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "FOLDER4001", "폴더는 최대 50개까지 생성할 수 있습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    @Override
    public ErrorReasonDTO getReason() {
        return ErrorReasonDTO.builder()
            .message(message).code(code).isSuccess(false).build();
    }

    @Override
    public ErrorReasonDTO getReasonHttpStatus() {
        return ErrorReasonDTO.builder()
            .message(message).code(code).isSuccess(false).httpStatus(httpStatus).build();
    }
}
```

### 코드 번호 패턴

| prefix | 도메인 |
|---|---|
| `FOLDER` | 폴더 |
| `LINKU` | 링크 |
| `USER` | 사용자 |
| `AUTH` / `OAUTH` | 인증 |
| `ALARM` | 알림 |
| `CURATION` | 큐레이션 |
| `S3` | AWS S3 |
| `COMMON` | 공통 |

번호: `4001`(400 Bad Request), `4031`(403), `4041`(404), `5001`(500)

---

## Repository

```java
// repository/{domain}/{Domain}Repository.java
public interface FolderRepository extends JpaRepository<Folder, Long> {

    List<Folder> findAllByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<Folder> findByIdAndUserId(Long folderId, Long userId);

    boolean existsByNameAndUserId(String name, Long userId);
}
```

복잡한 쿼리는 `@Query` 또는 별도 QueryDSL/JDSL 레포지토리로 분리.
