---
name: testing
description: Use when writing or modifying tests — Controller (@WebMvcTest), Service (@ExtendWith(MockitoExtension)), Integration (@SpringBootTest), fixture patterns, @WithCustomUser, TestSecurityConfig. Covers naming conventions (한국어 언더스코어), @Nested grouping, Given-When-Then, ApiResponse format verification, and ErrorStatus code assertions. Trigger on any file under src/test/, or when adding a new test for a new feature.
---

# Testing Guide

테스트 개수보다 회귀 방지, 계약 검증, 분기 커버 확보를 우선합니다.

---

## 1. 공통 원칙

- 테스트 클래스는 기능 단위로 `@Nested` 그룹을 나눈다.
- 테스트 메서드 이름은 **"~시 ~한다"** 형태의 한국어 언더스코어 문장으로 작성한다. `@DisplayName`도 동일 형태로 작성한다.
- 각 테스트는 Given-When-Then 흐름을 유지한다.
- 하나의 테스트 = 하나의 핵심 검증. 테스트가 길면 안 된다.
- 실패 케이스는 조건별로 분리한다. 한 테스트에 몰지 않는다.
- `assertNotNull` 대신 `assertEquals`로 구체적인 값을 검증한다.
- 예외는 `UserHandler` / `GeneralException` + `ErrorStatus` 에러코드 기준으로 검증한다.
- 응답은 실제 운영 포맷인 `ApiResponse` 기준(`$.isSuccess`, `$.code`, `$.result`)으로 검증한다.

---

## 2. 메서드 이름 규칙

| ❌ 나쁜 이름 | ✅ 좋은 이름 |
|---|---|
| `test1()` | `로그인_성공_시_액세스_토큰이_반환된다()` |
| `loginTest()` | `잘못된_비밀번호로_로그인_시_예외가_발생한다()` |
| `checkStatus()` | `소셜_가입_프로필_완성_시_상태가_ACTIVE로_변경된다()` |

---

## 3. 작성 템플릿

### 3.1 Controller Test

```java
@WebMvcTest({XxxController.class})
@AutoConfigureRestDocs
@ExtendWith(RestDocumentationExtension.class)
@Import({WebConfig.class, CurrentUserArgumentResolver.class, TestSecurityConfig.class})
class XxxControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockitoBean XxxService xxxService;
    @MockitoBean JwtTokenProvider jwtTokenProvider;
    @MockitoBean AccessTokenBlackListManager accessTokenBlackListManager;

    @Nested
    @DisplayName("엔드포인트 이름")
    class XxxEndpoint {

        @Nested
        @DisplayName("성공")
        class Success {
            @Test
            @DisplayName("정상 입력 시 생성 결과를 반환한다")
            @WithCustomUser(userId = 1L)
            void 정상_입력_시_생성_결과를_반환한다() throws Exception {
                // given
                given(xxxService.create(any(), any())).willReturn(response);

                // when & then — 상태값(isSuccess/code)은 JsonPath, result는 ObjectMapper로 역직렬화
                MvcResult result = mockMvc.perform(post("/api/v1/...")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                                .with(csrf()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.isSuccess").value(true))
                        .andExpect(jsonPath("$.code").value("DOMAIN201"))
                        .andReturn();

                XxxResponseDTO created = readResult(result, objectMapper, XxxResponseDTO.class);
                assertThat(created.getId()).isEqualTo(1L);
            }
        }

        @Nested
        @DisplayName("실패")
        class Failure {
            @Test
            @DisplayName("권한이 없을 시 예외 응답을 반환한다")
            @WithCustomUser(userId = 1L)
            void 권한이_없을_시_예외_응답을_반환한다() throws Exception {
                given(xxxService.method(any(), any()))
                        .willThrow(new UserHandler(UserErrorStatus._NOT_AUTHORIZED));

                mockMvc.perform(...)
                        .andExpect(status().isForbidden())
                        .andExpect(jsonPath("$.isSuccess").value(false))
                        .andExpect(jsonPath("$.code").value(UserErrorStatus._NOT_AUTHORIZED.getCode()));
            }
        }
    }
}
```

**반드시 검증할 항목**
- HTTP status
- `$.isSuccess`
- `$.code` — 실제 `SuccessStatus` / `ErrorStatus` 코드값
- `$.result` 주요 필드
- 인증 API는 `@WithCustomUser`로 인증 성공 시나리오를 반드시 포함

**`$.result` 검증 방식 — ObjectMapper 역직렬화 우선, JsonPath는 예외적으로만**

응답 DTO(또는 `List<DTO>`)가 있는 성공 케이스는 `MvcResult`를 받아 `com.umc.linkyou.support.util.ApiResponseTestUtils`로 `result`를 역직렬화하고, AssertJ getter로 검증한다:

```java
import static com.umc.linkyou.support.util.ApiResponseTestUtils.readResult;
import static com.umc.linkyou.support.util.ApiResponseTestUtils.readResultList;

MvcResult result = mockMvc.perform(get("/api/v1/xxx"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.isSuccess").value(true))
        .andReturn();

// 단건
XxxResponseDTO dto = readResult(result, objectMapper, XxxResponseDTO.class);
assertThat(dto.getName()).isEqualTo("...");

// 목록
List<XxxResponseDTO> list = readResultList(result, objectMapper, XxxResponseDTO.class);
assertThat(list).hasSize(1);
```

JsonPath는 다음 경우에만 사용한다:
- **상태값 확인**: `$.isSuccess`, `$.code`, `$.message` — 항상 JsonPath
- **단순 단건 값**: `result`가 DTO가 아니라 `String`/`Long` 같은 단일 값일 때 (예: 초대 링크 토큰) → `jsonPath("$.result").value(...)`
- **DTO를 만들기 애매한 응답**: `result`가 없는 삭제/비활성화류(`ApiResponse<Object>`, 빈 map) → `$.result` 자체를 검증하지 않음

**응답 DTO를 ObjectMapper로 역직렬화하려면 `@NoArgsConstructor` + (`@Setter` 또는 필드 접근 가능한 생성자)가 필요하다.** `@Getter @Builder`만 있는 DTO는 Jackson이 기본 생성자를 못 찾아 역직렬화에 실패하거나 필드가 채워지지 않은 채 조용히 통과할 수 있다 — 새 응답 DTO를 추가할 때는 `@NoArgsConstructor` + `@AllArgsConstructor`(빌더와 병행 시)를 함께 선언한다.

**`@WebMvcTest` 필수 MockitoBean**

인증 필터(`JwtAuthenticationFilter`)가 `JwtTokenProvider`와 `AccessTokenBlackListManager`를 요구하므로 항상 선언:

```java
@MockitoBean JwtTokenProvider jwtTokenProvider;
@MockitoBean AccessTokenBlackListManager accessTokenBlackListManager;
```

---

### 3.2 Service Test

```java
@ExtendWith(MockitoExtension.class)
class XxxServiceTest {

    @InjectMocks XxxService xxxService;
    @Mock XxxRepository xxxRepository;
    @Mock SomeDependency someDependency;

    @Test
    @DisplayName("정상 조건 시 상태가 변경된다")
    void 정상_조건_시_상태가_변경된다() {
        // given
        given(xxxRepository.findById(1L)).willReturn(Optional.of(entity));

        // when
        xxxService.doSomething(1L, request);

        // then
        assertEquals(ExpectedStatus.ACTIVE, entity.getStatus());
        verify(xxxRepository).save(entity);
    }

    @Test
    @DisplayName("존재하지 않는 대상 접근 시 예외를 던진다")
    void 존재하지_않는_대상_접근_시_예외를_던진다() {
        // given
        given(xxxRepository.findById(99L)).willReturn(Optional.empty());

        // when & then
        UserHandler ex = assertThrows(UserHandler.class,
                () -> xxxService.doSomething(99L, request));
        assertEquals(UserErrorStatus._NOT_FOUND, ex.getCode());
    }
}
```

**반드시 검증할 항목**
- 상태 변경 (`assertEquals(ExpectedStatus, entity.getStatus())`)
- 협력 객체 호출 여부 (`verify(repository).save(any())`)
- 예외 타입 + **에러코드** (`assertEquals(UserErrorStatus._XXX, ex.getCode())`)
- `assertNotNull` 대신 `assertEquals`로 구체적 값 확인

**분기 검증 우선순위**
1. 권한 분기 (`_NOT_AUTHORIZED`)
2. 존재 검증 (`_NOT_FOUND`, `_USER_NOT_FOUND`)
3. 중복/상태 충돌 (`_DUPLICATE_*`, `_ALREADY_*`)
4. 요청 검증 실패 (`_BAD_REQUEST`, `_INVALID_*`)

---

### 3.3 통합 테스트

**일반 케이스 — 서비스 로직 + DB 상태 검증**

```java
@SpringBootTest
@Transactional
class XxxIntegrationTest {

    @Autowired XxxService xxxService;
    @Autowired XxxRepository xxxRepository;
    @PersistenceContext EntityManager em;

    @BeforeEach
    void setUp() {
        // fixture 기반 테스트 데이터 세팅
        em.flush();
        em.clear();
    }

    @Test
    @DisplayName("특정 흐름 완료 시 DB 상태가 반영된다")
    void 특정_흐름_완료_시_DB_상태가_반영된다() {
        // given / when / then
    }
}
```

트랜잭션 경계, 롤백, 연관관계 삭제, 복합 흐름 검증에 사용한다. 단위 테스트로 충분한 분기는 반복하지 않는다.

**외부 연동 케이스 — 실제 호출 검증 (예: 이메일 발송)**

```java
@SpringBootTest(properties = "SERVER_BASE_URL=http://localhost:8080")
class EmailSendIntegrationTest {

    @Autowired
    private EmailService emailService;

    @MockitoBean
    private Client geminiClient;  // 불필요한 외부 의존성만 mock

    private static final String TEST_RECIPIENT = "ljw56b@gmail.com";

    @Test
    void 비밀번호_재설정_메일_발송() {
        emailService.sendPasswordResetEmail(
                TEST_RECIPIENT,
                "테스트 유저",
                "http://localhost:8080/password/reset?token=test-token-abc123",
                10
        );
    }
}
```

mock 검증보다는 실제 서비스 호출을 수행하고, 결과가 잘 나오는지를 검증하는 목적이다.

---

## 4. 인증/보안

- `@WithCustomUser(userId = 1L)` — 기본 USER 권한
- `@WithCustomUser(userId = 1L, role = "ADMIN")` — ADMIN 권한
- `TestSecurityConfig` — JWT 필터 없이 컨트롤러 로직만 테스트
- 인증 필요 API는 인증 성공 시나리오를 반드시 포함. 비인증 실패만 두고 끝내지 않는다.

---

## 5. 금지 사항

- `ErrorCode` 검증 없이 status만 확인하는 테스트 금지
- `assertNotNull` 단독 사용 금지 → `assertEquals`로 교체
- 서비스 테스트에 불필요하게 `@SpringBootTest` 사용 금지
- 실패 케이스 여러 개를 하나의 테스트에 몰기 금지

---

## 6. 테스트 데이터 (Fixture)

중복 로직 방지를 위해 테스트는 fixture 기반으로 진행한다.
공통 데이터는 fixture로 관리하고, 케이스별 차이만 덮어써서 검증한다.
`src/test/java/.../fixture/` 패키지에 도메인별로 관리한다.

```java
// 사용 방법
var user = UserFixture.activeUser();
var token = TokenFixture.validToken(user.getId());

// UserFixture 예시
public final class UserFixture {
    private UserFixture() {}

    public static Users activeUser() {
        return Users.builder()
                .nickName("testUser")
                .password("encoded-password")
                .role(Role.USER)
                .status(UserStatus.ACTIVE)
                .build();
    }

    public static Users tempUser() {
        return Users.builder()
                .nickName("tempUser")
                .password("encoded-password")
                .role(Role.USER)
                .status(UserStatus.TEMP)
                .build();
    }

    public static Users activeUser(String nickName) {
        return Users.builder()
                .nickName(nickName)
                .password("encoded-password")
                .role(Role.USER)
                .status(UserStatus.ACTIVE)
                .build();
    }
}
```

---

## 체크리스트

- [ ] 메서드명과 `@DisplayName`이 "~시 ~한다" 형태인가?
- [ ] 성공/실패 `@Nested` 그룹이 분리되어 있는가?
- [ ] `$.code`까지 에러코드를 검증하는가?
- [ ] 인증 API에 `@WithCustomUser` 인증 성공 케이스가 있는가?
- [ ] `assertNotNull` 대신 `assertEquals`를 쓰는가?
- [ ] 하나의 테스트가 하나의 핵심 동작만 검증하는가?
- [ ] 컨트롤러 테스트의 `$.result`(DTO/목록)는 JsonPath 체이닝 대신 `ApiResponseTestUtils`로 역직렬화해 AssertJ로 검증하는가? (상태값·단순값·DTO 없는 응답은 예외)
