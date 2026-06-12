# LinkU 백엔드 테스트 작성 가이드

PR #298에서 추가된 테스트 인프라(`@WithCustomUser`, `TestSecurityConfig`, `application-test.yml`)를 활용한 CRUD API 테스트 작성 방법 가이드입니다.

---

## 목차

1. [테스트 인프라 구성 요소](#1-테스트-인프라-구성-요소)
2. [어노테이션 방식 — `@WithCustomUser`](#2-어노테이션-방식--withcustomuser)
3. [통합 테스트 — `@SpringBootTest`](#3-통합-테스트--springboottest)
4. [슬라이스 테스트 — `@WebMvcTest`](#4-슬라이스-테스트--webmvctest)
5. [서비스 단위 테스트 — `@ExtendWith(MockitoExtension)`](#5-서비스-단위-테스트--extendwithmockitoextension)
6. [테스트 유형별 선택 기준](#6-테스트-유형별-선택-기준)

---

## 1. 테스트 인프라 구성 요소

| 파일 | 위치 | 역할 |
|------|------|------|
| `@WithCustomUser` | `support/security/` | 테스트용 인증 유저 주입 어노테이션 |
| `WithCustomUserSecurityContextFactory` | `support/security/` | `@WithCustomUser` 구현체 |
| `TestSecurityConfig` | `support/security/` | JWT 필터 없이 인증 처리 |
| `application-test.yml` | `src/test/resources/` | H2 DB 공통 설정 |

### `@WithCustomUser` 파라미터

```java
@WithCustomUser(
    userId   = 1L,          // 기본값: 1L
    nickName = "linku-user", // 기본값: "linku-user"
    role     = Role.USER,   // 기본값: Role.USER
    provider = "kakao"      // 기본값: "kakao"
)
```

---

## 2. 어노테이션 방식 — `@WithCustomUser`

### 기존 방식 vs 개선 방식

```java
// ❌ 기존 — 매 테스트마다 반복 작성
@BeforeEach
void setUp() {
    CustomUserDetails userDetails = new CustomUserDetails(user, "kakao");
    UsernamePasswordAuthenticationToken auth =
        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
    SecurityContextHolder.getContext().setAuthentication(auth);
}

@AfterEach
void tearDown() {
    SecurityContextHolder.clearContext();
}

// ✅ 개선 — 어노테이션 하나로 끝
@WithCustomUser(userId = 1L, provider = "kakao")
@Test
void someTest() { ... }
```

---

## 3. 통합 테스트 — `@SpringBootTest`

실제 Spring 컨텍스트를 띄우고 H2 인메모리 DB로 전체 레이어를 테스트합니다.  
`@Import(TestSecurityConfig.class)`로 JWT 필터를 제거하고, `authentication()` PostProcessor로 인증을 주입합니다.

### 예시: 폴더 CRUD 통합 테스트

```java
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestSecurityConfig.class)   // JWT 필터 제거
@Transactional                      // 테스트 후 자동 롤백
class FolderApiIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private FolderRepository folderRepository;

    // ──────────────────────────────────────────
    // [CREATE] POST /api/v1/folders
    // ──────────────────────────────────────────
    @Test
    @DisplayName("폴더 생성 - 정상 생성된다")
    void createFolder_success() throws Exception {
        Users user = createUser("folder_user_1");

        FolderRequestDTO.CreateDTO request = new FolderRequestDTO.CreateDTO();
        request.setName("내 폴더");

        mockMvc.perform(post("/api/v1/folders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(authentication(authFor(user))))  // 인증 주입
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("FOLDER2011"))
                .andExpect(jsonPath("$.result.name").value("내 폴더"));
    }

    // ──────────────────────────────────────────
    // [READ] GET /api/v1/folders/{folderId}
    // ──────────────────────────────────────────
    @Test
    @DisplayName("폴더 조회 - 존재하는 폴더를 정상 조회한다")
    void getFolder_success() throws Exception {
        Users user = createUser("folder_user_2");
        Folder folder = createFolder(user, "조회용 폴더");

        mockMvc.perform(get("/api/v1/folders/{folderId}", folder.getId())
                        .with(authentication(authFor(user))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.name").value("조회용 폴더"));
    }

    // ──────────────────────────────────────────
    // [UPDATE] PATCH /api/v1/folders/{folderId}
    // ──────────────────────────────────────────
    @Test
    @DisplayName("폴더 수정 - 이름이 정상적으로 변경된다")
    void updateFolder_success() throws Exception {
        Users user = createUser("folder_user_3");
        Folder folder = createFolder(user, "수정 전 이름");

        FolderRequestDTO.UpdateDTO request = new FolderRequestDTO.UpdateDTO();
        request.setName("수정 후 이름");

        mockMvc.perform(patch("/api/v1/folders/{folderId}", folder.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(authentication(authFor(user))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.name").value("수정 후 이름"));
    }

    // ──────────────────────────────────────────
    // [DELETE] DELETE /api/v1/folders/{folderId}
    // ──────────────────────────────────────────
    @Test
    @DisplayName("폴더 삭제 - 정상 삭제된다")
    void deleteFolder_success() throws Exception {
        Users user = createUser("folder_user_4");
        Folder folder = createFolder(user, "삭제할 폴더");

        mockMvc.perform(delete("/api/v1/folders/{folderId}", folder.getId())
                        .with(authentication(authFor(user))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true));

        assertFalse(folderRepository.existsById(folder.getId()));
    }

    // ──────────────────────────────────────────
    // 권한 검증 테스트
    // ──────────────────────────────────────────
    @Test
    @DisplayName("폴더 삭제 - 다른 유저는 삭제할 수 없다")
    void deleteFolder_forbidden() throws Exception {
        Users owner = createUser("owner_user");
        Users other = createUser("other_user");
        Folder folder = createFolder(owner, "남의 폴더");

        mockMvc.perform(delete("/api/v1/folders/{folderId}", folder.getId())
                        .with(authentication(authFor(other))))  // 다른 유저로 요청
                .andExpect(status().isForbidden());
    }

    // ──────────────────────────────────────────
    // Helper Methods
    // ──────────────────────────────────────────
    private Users createUser(String nickName) {
        return userRepository.save(Users.builder()
                .nickName(nickName)
                .password("password")
                .role(Role.USER)
                .build());
    }

    private Folder createFolder(Users user, String name) {
        return folderRepository.save(Folder.builder()
                .user(user)
                .name(name)
                .build());
    }

    /**
     * Users → Authentication 변환 헬퍼
     * TestSecurityConfig와 함께 사용
     */
    private Authentication authFor(Users user) {
        CustomUserDetails principal = new CustomUserDetails(user, "kakao");
        return new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities()
        );
    }
}
```

---

## 4. 슬라이스 테스트 — `@WebMvcTest`

Controller 레이어만 띄웁니다. 서비스는 `@MockitoBean`으로 Mock 처리합니다.  
`@WithCustomUser`를 메서드 혹은 클래스 레벨에 붙여서 인증을 주입합니다.

### 예시: 폴더 컨트롤러 슬라이스 테스트

```java
@WebMvcTest(FolderController.class)
@Import(TestSecurityConfig.class)   // JWT 필터 제거
class FolderControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private FolderService folderService;
    @MockitoBean private JwtTokenProvider jwtTokenProvider;
    @MockitoBean private AccessTokenBlackListManager accessTokenBlackListManager;

    // ──────────────────────────────────────────
    // [CREATE]
    // ──────────────────────────────────────────
    @Test
    @DisplayName("폴더 생성 성공")
    @WithCustomUser(userId = 1L)    // 클래스 대신 메서드 레벨에 붙여도 됨
    void createFolder_success() throws Exception {
        FolderResponseDTO.CreateResultDTO mockResult =
                FolderResponseDTO.CreateResultDTO.builder()
                        .folderId(10L)
                        .name("내 폴더")
                        .build();

        given(folderService.createFolder(eq(1L), any()))
                .willReturn(mockResult);

        FolderRequestDTO.CreateDTO request = new FolderRequestDTO.CreateDTO();
        request.setName("내 폴더");

        mockMvc.perform(post("/api/v1/folders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.folderId").value(10L))
                .andExpect(jsonPath("$.result.name").value("내 폴더"));
    }

    // ──────────────────────────────────────────
    // [READ]
    // ──────────────────────────────────────────
    @Test
    @DisplayName("폴더 조회 성공")
    @WithCustomUser(userId = 1L)
    void getFolder_success() throws Exception {
        FolderResponseDTO.FolderDetailDTO mockDetail =
                FolderResponseDTO.FolderDetailDTO.builder()
                        .folderId(10L)
                        .name("내 폴더")
                        .build();

        given(folderService.getFolder(eq(1L), eq(10L)))
                .willReturn(mockDetail);

        mockMvc.perform(get("/api/v1/folders/{folderId}", 10L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.name").value("내 폴더"));
    }

    // ──────────────────────────────────────────
    // [UPDATE]
    // ──────────────────────────────────────────
    @Test
    @DisplayName("폴더 수정 성공")
    @WithCustomUser(userId = 1L)
    void updateFolder_success() throws Exception {
        FolderResponseDTO.FolderDetailDTO mockResult =
                FolderResponseDTO.FolderDetailDTO.builder()
                        .folderId(10L)
                        .name("수정된 이름")
                        .build();

        given(folderService.updateFolder(eq(1L), eq(10L), any()))
                .willReturn(mockResult);

        FolderRequestDTO.UpdateDTO request = new FolderRequestDTO.UpdateDTO();
        request.setName("수정된 이름");

        mockMvc.perform(patch("/api/v1/folders/{folderId}", 10L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.name").value("수정된 이름"));
    }

    // ──────────────────────────────────────────
    // [DELETE]
    // ──────────────────────────────────────────
    @Test
    @DisplayName("폴더 삭제 성공")
    @WithCustomUser(userId = 1L)
    void deleteFolder_success() throws Exception {
        willDoNothing().given(folderService).deleteFolder(eq(1L), eq(10L));

        mockMvc.perform(delete("/api/v1/folders/{folderId}", 10L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true));
    }

    // ──────────────────────────────────────────
    // 인증 없는 요청 → 401
    // ──────────────────────────────────────────
    @Test
    @DisplayName("비인증 요청 - 401 반환")
    void createFolder_unauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/folders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }
}
```

---

## 5. 서비스 단위 테스트 — `@ExtendWith(MockitoExtension)`

Spring 컨텍스트 없이 순수 Mockito로 서비스 로직만 검증합니다.  
`UsersUtils`가 삭제됐으므로 `userId`를 직접 파라미터로 넘기는 방식으로 작성합니다.

### 예시: 폴더 서비스 단위 테스트

```java
@ExtendWith(MockitoExtension.class)
class FolderServiceTest {

    @InjectMocks private FolderService folderService;
    @Mock private FolderRepository folderRepository;
    @Mock private UserRepository userRepository;
    @Mock private UserStatusValidator userStatusValidator;

    // ──────────────────────────────────────────
    // [CREATE]
    // ──────────────────────────────────────────
    @Test
    @DisplayName("폴더 생성 - 정상 생성된다")
    void createFolder_success() {
        // given
        Long userId = 1L;
        Users user = Users.builder().id(userId).role(Role.USER).build();

        FolderRequestDTO.CreateDTO request = new FolderRequestDTO.CreateDTO();
        request.setName("내 폴더");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(folderRepository.save(any(Folder.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // when — UsersUtils 없이 userId 직접 전달
        FolderResponseDTO.CreateResultDTO result = folderService.createFolder(userId, request);

        // then
        assertEquals("내 폴더", result.getName());
        verify(folderRepository).save(any(Folder.class));
    }

    // ──────────────────────────────────────────
    // [READ]
    // ──────────────────────────────────────────
    @Test
    @DisplayName("폴더 조회 - 존재하지 않는 폴더는 예외 발생")
    void getFolder_notFound() {
        // given
        when(folderRepository.findById(99L)).thenReturn(Optional.empty());

        // when & then
        assertThrows(GeneralException.class,
                () -> folderService.getFolder(1L, 99L));
    }

    // ──────────────────────────────────────────
    // [UPDATE]
    // ──────────────────────────────────────────
    @Test
    @DisplayName("폴더 수정 - 소유자가 아니면 예외 발생")
    void updateFolder_forbidden() {
        // given
        Long ownerId = 1L;
        Long requesterId = 2L;  // 다른 유저

        Folder folder = Folder.builder()
                .id(10L)
                .user(Users.builder().id(ownerId).build())
                .name("원래 이름")
                .build();

        when(folderRepository.findById(10L)).thenReturn(Optional.of(folder));

        FolderRequestDTO.UpdateDTO request = new FolderRequestDTO.UpdateDTO();
        request.setName("바꾸려는 이름");

        // when & then
        assertThrows(GeneralException.class,
                () -> folderService.updateFolder(requesterId, 10L, request));
    }

    // ──────────────────────────────────────────
    // [DELETE]
    // ──────────────────────────────────────────
    @Test
    @DisplayName("폴더 삭제 - 정상 삭제된다")
    void deleteFolder_success() {
        // given
        Long userId = 1L;
        Folder folder = Folder.builder()
                .id(10L)
                .user(Users.builder().id(userId).build())
                .build();

        when(folderRepository.findById(10L)).thenReturn(Optional.of(folder));
        willDoNothing().given(folderRepository).delete(any());

        // when
        folderService.deleteFolder(userId, 10L);

        // then
        verify(folderRepository).delete(folder);
    }
}
```

---

## 6. 테스트 유형별 선택 기준

| 상황 | 사용할 방식 | 어노테이션 |
|------|------------|-----------|
| 전체 API 흐름 검증 (DB 포함) | 통합 테스트 | `@SpringBootTest` + `@Import(TestSecurityConfig.class)` + `authFor()` |
| 컨트롤러 응답 포맷/라우팅 검증 | 슬라이스 테스트 | `@WebMvcTest` + `@WithCustomUser` |
| 비즈니스 로직 단독 검증 | 단위 테스트 | `@ExtendWith(MockitoExtension.class)` |
| 개발자 권한으로 테스트 (`ADMIN`) | 슬라이스/통합 | `@WithCustomUser(role = Role.ADMIN)` |
| 관리자 인증 주입 (`MANAGER` 권한으로 테스트) | 슬라이스/통합 | `@WithCustomUser(role = Role.MANAGER)` |

### 인증 주입 방법 비교

| 방법 | 사용 상황 | 코드 |
|------|----------|------|
| `@WithCustomUser` | `@WebMvcTest` 또는 `@SpringBootTest`에서 메서드/클래스 레벨 인증 주입 | `@WithCustomUser(userId = 1L)` |
| `authentication()` PostProcessor | `@SpringBootTest` + 실제 Users 엔티티 필요 시 | `.with(authentication(authFor(user)))` |
### `application-test.yml` 활용

각 테스트 클래스에서 `@TestPropertySource`로 중복 선언하는 대신, `application-test.yml`을 공통으로 사용합니다.

```java
// ❌ 기존 — 클래스마다 중복
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb;MODE=MySQL",
    "spring.datasource.driver-class-name=org.h2.Driver",
    ...
})

// ✅ 개선 — yml 하나로 모든 테스트에 자동 적용
// application-test.yml이 src/test/resources/에 있으면 별도 설정 불필요
@SpringBootTest  // 이것만 있으면 됨
```

---

## 7. 테스트 클래스 구조 가이드

하나의 기능에 대해 테스트가 여러 개라면 `@Nested`를 사용해 `성공 케이스`, `실패 케이스`, `예외 케이스`로 나누어 작성합니다.  
이렇게 하면 테스트 목적이 명확해지고, 같은 기능의 시나리오를 한눈에 확인할 수 있습니다.

```java
@Nested
@DisplayName("성공 케이스")
class SuccessCase { ... }

@Nested
@DisplayName("실패 케이스")
class FailureCase { ... }
```
