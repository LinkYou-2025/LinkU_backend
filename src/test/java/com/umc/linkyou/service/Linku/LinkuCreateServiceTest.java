package com.umc.linkyou.service.Linku;

import static com.umc.linkyou.support.fixture.LinkuFixture.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.lenient;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import com.umc.linkyou.apiPayload.code.status.linku.LinkuErrorStatus;
import com.umc.linkyou.apiPayload.exception.GeneralException;
import com.umc.linkyou.awss3.AwsS3Service;
import com.umc.linkyou.domain.Linku;
import com.umc.linkyou.domain.classification.Category;
import com.umc.linkyou.domain.classification.Domain;
import com.umc.linkyou.domain.enums.PageType;
import com.umc.linkyou.domain.mapping.UsersLinku;
import com.umc.linkyou.infra.ai.dto.LinkuResultDTO;
import com.umc.linkyou.infra.gemini.service.GeminiLinkuService;
import com.umc.linkyou.infra.net.SafeUrlFetcher;
import com.umc.linkyou.infra.parser.LinkToImageService;
import com.umc.linkyou.infra.parser.RobotsTxtChecker;
import com.umc.linkyou.repository.EmotionRepository;
import com.umc.linkyou.repository.UserLinkuRepository.UsersLinkuRepository;
import com.umc.linkyou.repository.aiArticleRepository.AiArticleRepository;
import com.umc.linkyou.repository.classification.CategoryRepository;
import com.umc.linkyou.repository.classification.SituationRepository;
import com.umc.linkyou.repository.classification.domainRepository.DomainRepository;
import com.umc.linkyou.repository.linkuRepository.LinkuRepository;
import com.umc.linkyou.repository.mapping.linkuFolderRepository.LinkuFolderRepository;
import com.umc.linkyou.repository.userRepository.UserRepository;
import com.umc.linkyou.repository.usersFolderRepository.UsersFolderRepository;
import com.umc.linkyou.service.Linku.LinkuMetrics.ResolveOutcome;
import com.umc.linkyou.service.Linku.LinkuMetrics.Stage;
import com.umc.linkyou.service.domain.DomainAutoRegisterService;
import com.umc.linkyou.service.folder.FolderService;
import com.umc.linkyou.service.keyword.KeywordService;
import com.umc.linkyou.support.fixture.LinkuFixture;
import com.umc.linkyou.web.dto.linku.LinkuRequestDTO;
import com.umc.linkyou.web.dto.linku.LinkuResponseDTO;

@ExtendWith(MockitoExtension.class)
@DisplayName("LinkuCreateService 테스트")
class LinkuCreateServiceTest {

    private LinkuCreateService linkuCreateService;

    @Spy @InjectMocks private LinkResolver linkResolver;
    @Spy @InjectMocks private LinkuClassificationResolver classificationResolver;
    private LinkuPersister linkuPersister;

    @Mock private LinkuRepository linkuRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private EmotionRepository emotionRepository;
    @Mock private DomainRepository domainRepository;
    @Mock private LinkuFolderRepository linkuFolderRepository;
    @Mock private UsersLinkuRepository usersLinkuRepository;
    @Mock private UserRepository userRepository;
    @Mock private AwsS3Service awsS3Service;
    @Mock private LinkToImageService linkToImageService;
    @Mock private RobotsTxtChecker robotsTxtChecker;
    @Mock private SituationRepository situationRepository;
    @Mock private AiArticleRepository aiArticleRepository;
    @Mock private GeminiLinkuService geminiLinkuService;
    @Mock private FolderService folderService;
    @Mock private KeywordService keywordService;
    @Mock private UsersFolderRepository usersFolderRepository;

    // 의존성 주입을 위한 Upsert 서비스 Mock 추가
    @Mock private LinkuUpsertService linkuUpsertService;
    // SSRF 검증용 SafeUrlFetcher - fetchPage 성공 여부로 validUrl 계산 + 대표 URL 결정 (setupNewLinkMocks에서 기본
    // 성공)
    @Mock private SafeUrlFetcher safeUrlFetcher;
    // DB 쓰기 구간만 감싸는 프로그래밍 방식 트랜잭션 - 목에서는 콜백을 그대로 실행해주기만 하면 된다.
    @Mock private TransactionTemplate transactionTemplate;
    // 처음 보는 사이트 도메인 자동 등록. 기본 mock은 Optional.empty() → 기존처럼 resolveDomain으로 폴백한다.
    @Mock private DomainAutoRegisterService domainAutoRegisterService;
    // 링크 저장 지표. 목에서는 아무것도 하지 않고, 필요한 테스트에서만 기록된 단계를 검증한다.
    @Mock private LinkuMetrics linkuMetrics;

    private static final String SHORT_URL = "https://bit.ly/abc";

    @BeforeEach
    void setUpTransactionTemplate() {
        linkuPersister =
                new LinkuPersister(
                        linkuRepository,
                        domainRepository,
                        linkuFolderRepository,
                        usersLinkuRepository,
                        userRepository,
                        usersFolderRepository,
                        keywordService,
                        linkuUpsertService,
                        classificationResolver,
                        transactionTemplate);
        linkuCreateService =
                new LinkuCreateService(
                        linkResolver,
                        classificationResolver,
                        domainAutoRegisterService,
                        awsS3Service,
                        linkuPersister,
                        linkuMetrics);
        lenient()
                .doAnswer(
                        invocation -> {
                            TransactionCallback<?> callback = invocation.getArgument(0);
                            return callback.doInTransaction(null);
                        })
                .when(transactionTemplate)
                .execute(any());
    }

    @Nested
    @DisplayName("신규 링크 등록 - 이미지 저장 분기")
    class NewLinkImageRouting {

        @Nested
        @DisplayName("성공")
        class Success {

            @Test
            @DisplayName("사용자 이미지 업로드 시 S3 URL이 UsersLinku에 저장되고 Linku에는 크롤링 이미지가 저장된다")
            void 사용자_이미지_업로드_시_S3_URL이_UsersLinku에_저장되고_Linku에는_크롤링_이미지가_저장된다() {
                setupNewLinkMocks(CRAWLED_IMAGE_URL);
                MultipartFile image = mock(MultipartFile.class);
                doReturn(false).when(image).isEmpty();
                doReturn(S3_IMAGE_URL).when(awsS3Service).uploadFile(image, "linkucreate");

                linkuCreateService.createLinku(USER_ID, defaultDto(), image);

                // String 캡처러를 사용하여 5번째 파라미터(imgUrl) 검증
                ArgumentCaptor<String> imgUrlCaptor = ArgumentCaptor.forClass(String.class);
                verify(linkuUpsertService)
                        .upsert(any(), any(), any(), any(), imgUrlCaptor.capture(), any(), any());
                assertEquals(CRAWLED_IMAGE_URL, imgUrlCaptor.getValue());

                ArgumentCaptor<UsersLinku> usersLinkuCaptor =
                        ArgumentCaptor.forClass(UsersLinku.class);
                verify(usersLinkuRepository).save(usersLinkuCaptor.capture());
                assertEquals(S3_IMAGE_URL, usersLinkuCaptor.getValue().getImageUrl());
                assertEquals(EMOTION_ID, usersLinkuCaptor.getValue().getEmotion().getEmotionId());
            }

            @Test
            @DisplayName("이미지 미업로드 시 크롤링 이미지가 Linku에 저장되고 UsersLinku imageUrl은 null이다")
            void 이미지_미업로드_시_크롤링_이미지가_Linku에_저장되고_UsersLinku_imageUrl은_null이다() {
                setupNewLinkMocks(CRAWLED_IMAGE_URL);

                linkuCreateService.createLinku(USER_ID, defaultDto(), null);

                // String 캡처러를 사용하여 5번째 파라미터(imgUrl) 검증
                ArgumentCaptor<String> imgUrlCaptor = ArgumentCaptor.forClass(String.class);
                verify(linkuUpsertService)
                        .upsert(any(), any(), any(), any(), imgUrlCaptor.capture(), any(), any());
                assertEquals(CRAWLED_IMAGE_URL, imgUrlCaptor.getValue());

                ArgumentCaptor<UsersLinku> usersLinkuCaptor =
                        ArgumentCaptor.forClass(UsersLinku.class);
                verify(usersLinkuRepository).save(usersLinkuCaptor.capture());
                assertNull(usersLinkuCaptor.getValue().getImageUrl());
                assertEquals(EMOTION_ID, usersLinkuCaptor.getValue().getEmotion().getEmotionId());
            }

            @Test
            @DisplayName("크롤링 이미지가 없을 시 Linku imgUrl은 null이다")
            void 크롤링_이미지가_없을_시_Linku_imgUrl은_null이다() {
                setupNewLinkMocks(null);

                linkuCreateService.createLinku(USER_ID, defaultDto(), null);

                // String 캡처러를 사용하여 5번째 파라미터(imgUrl) 검증
                ArgumentCaptor<String> imgUrlCaptor = ArgumentCaptor.forClass(String.class);
                verify(linkuUpsertService)
                        .upsert(any(), any(), any(), any(), imgUrlCaptor.capture(), any(), any());
                assertNull(imgUrlCaptor.getValue());

                ArgumentCaptor<UsersLinku> usersLinkuCaptor =
                        ArgumentCaptor.forClass(UsersLinku.class);
                verify(usersLinkuRepository).save(usersLinkuCaptor.capture());
                assertEquals(EMOTION_ID, usersLinkuCaptor.getValue().getEmotion().getEmotionId());
            }
        }
    }

    @Nested
    @DisplayName("신규 링크 등록 - 접속 확인 분기")
    class NewLinkReachability {

        @Test
        @DisplayName("정상 응답 시 AI 분석을 수행하고 validUrl은 true이다")
        void 정상_응답_시_AI_분석을_수행하고_validUrl은_true이다() {
            setupNewLinkMocks(CRAWLED_IMAGE_URL);

            LinkuResponseDTO.LinkuCreateResult result =
                    linkuCreateService.createLinku(USER_ID, defaultDto(), null);

            verify(geminiLinkuService).analyzeByUrl(any(), any(), any(), any(), any());
            assertTrue(result.validUrl());
            verify(linkuMetrics).recordCreate(eq(ResolveOutcome.NEW_LINK), eq(true), anyLong());
            // 새 링크 경로의 외부 I/O 단계가 각각 기록된다
            verify(linkuMetrics).recordStage(eq(Stage.FETCH), anyLong());
            verify(linkuMetrics).recordStage(eq(Stage.AI), anyLong());
            verify(linkuMetrics).recordStage(eq(Stage.IMAGE), anyLong());
            verify(linkuMetrics).recordStage(eq(Stage.PERSIST), anyLong());
        }

        @Test
        @DisplayName("접속 불가 시 AI 분석과 크롤링 이미지 조회를 건너뛴다")
        void 접속_불가_시_AI_분석과_크롤링_이미지_조회를_건너뛴다() throws Exception {
            setupNewLinkMocks(CRAWLED_IMAGE_URL);
            given(safeUrlFetcher.fetchPage(any(), any(), anyInt()))
                    .willThrow(new java.io.IOException("timeout"));

            LinkuResponseDTO.LinkuCreateResult result =
                    linkuCreateService.createLinku(USER_ID, defaultDto(), null);

            verify(geminiLinkuService, never()).analyzeByUrl(any(), any(), any(), any(), any());
            verify(linkToImageService, never()).getRelatedImageFromUrl(any(), any(), any());
            assertFalse(result.validUrl());
            // 접속이 안 돼도 기본 행이 아니라 실제 사이트 행에 연결한다 (네트워크 없이 최소 행)
            verify(domainAutoRegisterService).resolveOrRegister(TEST_URL);
        }
    }

    @Nested
    @DisplayName("기존 링크 재사용 - 이미지 저장 분기")
    class ExistingLinkImageRouting {

        @Nested
        @DisplayName("성공")
        class Success {

            @Test
            @DisplayName("사용자 이미지 업로드 시 S3 URL이 UsersLinku에 저장된다")
            void 사용자_이미지_업로드_시_S3_URL이_UsersLinku에_저장된다() {
                Linku existingLinku = LinkuFixture.linku(CRAWLED_IMAGE_URL);
                ensureExistingLinkuClassification(existingLinku);
                setupExistingLinkMocks(existingLinku);

                MultipartFile image = mock(MultipartFile.class);
                given(image.isEmpty()).willReturn(false);
                given(awsS3Service.uploadFile(image, "linkucreate")).willReturn(S3_IMAGE_URL);

                linkuCreateService.createLinku(USER_ID, defaultDto(), image);

                ArgumentCaptor<UsersLinku> captor = ArgumentCaptor.forClass(UsersLinku.class);
                verify(usersLinkuRepository).save(captor.capture());
                assertEquals(S3_IMAGE_URL, captor.getValue().getImageUrl());
                assertEquals(EMOTION_ID, captor.getValue().getEmotion().getEmotionId());
            }

            @Test
            @DisplayName("이미지 미업로드 시 UsersLinku imageUrl은 null이고 Linku는 재저장되지 않는다")
            void 이미지_미업로드_시_UsersLinku_imageUrl은_null이고_Linku는_재저장되지_않는다() {
                Linku existingLinku = LinkuFixture.linku(CRAWLED_IMAGE_URL);
                ensureExistingLinkuClassification(existingLinku);
                setupExistingLinkMocks(existingLinku);

                linkuCreateService.createLinku(USER_ID, defaultDto(), null);

                ArgumentCaptor<UsersLinku> captor = ArgumentCaptor.forClass(UsersLinku.class);
                verify(usersLinkuRepository).save(captor.capture());
                assertNull(captor.getValue().getImageUrl());
                assertEquals(EMOTION_ID, captor.getValue().getEmotion().getEmotionId());

                // 기존 링크가 존재하면 upsert 로직이 수행되지 않아야 함
                verify(linkuUpsertService, never())
                        .upsert(any(), any(), any(), any(), any(), any(), any());
            }
        }
    }

    @Nested
    @DisplayName("대표 URL 결정")
    class CanonicalUrl {

        @Nested
        @DisplayName("성공")
        class Success {

            @Test
            @DisplayName("이미 저장된 링크 입력 시 페이지를 요청하지 않고 validUrl은 true이다")
            void 이미_저장된_링크_입력_시_페이지를_요청하지_않고_validUrl은_true이다() throws Exception {
                Linku existingLinku = LinkuFixture.linku(CRAWLED_IMAGE_URL);
                ensureExistingLinkuClassification(existingLinku);
                setupExistingLinkMocks(existingLinku);

                LinkuResponseDTO.LinkuCreateResult result =
                        linkuCreateService.createLinku(USER_ID, defaultDto(), null);

                verify(safeUrlFetcher, never()).fetchPage(any(), any(), anyInt());
                verify(safeUrlFetcher, never()).probe(any());
                assertTrue(result.validUrl());
                verify(linkuMetrics)
                        .recordCreate(eq(ResolveOutcome.INPUT_HIT), eq(true), anyLong());
            }

            @Test
            @DisplayName("단축 URL의 최종 URL이 이미 저장된 링크면 AI 분석 없이 재사용한다")
            void 단축_URL의_최종_URL이_이미_저장된_링크면_AI_분석_없이_재사용한다() throws Exception {
                Linku existingLinku = LinkuFixture.linku(CRAWLED_IMAGE_URL);
                ensureExistingLinkuClassification(existingLinku);
                setupExistingLinkMocks(existingLinku);
                given(robotsTxtChecker.isAllowed(any(), any())).willReturn(true);
                given(safeUrlFetcher.fetchPage(eq(SHORT_URL), any(), anyInt()))
                        .willReturn(
                                page(
                                        TEST_URL,
                                        "",
                                        List.of(
                                                new SafeUrlFetcher.RedirectHop(
                                                        SHORT_URL, 302, TEST_URL))));

                linkuCreateService.createLinku(USER_ID, dtoWithUrl(SHORT_URL), null);

                verify(geminiLinkuService, never()).analyzeByUrl(any(), any(), any(), any(), any());
                verify(linkuUpsertService, never())
                        .upsert(any(), any(), any(), any(), any(), any(), any());
            }

            @Test
            @DisplayName("처음 보는 단축 URL 입력 시 최종 URL로 링크를 저장한다")
            void 처음_보는_단축_URL_입력_시_최종_URL로_링크를_저장한다() throws Exception {
                setupNewLinkMocks(CRAWLED_IMAGE_URL);
                given(safeUrlFetcher.fetchPage(eq(SHORT_URL), any(), anyInt()))
                        .willReturn(
                                page(
                                        TEST_URL,
                                        "",
                                        List.of(
                                                new SafeUrlFetcher.RedirectHop(
                                                        SHORT_URL, 302, TEST_URL))));

                LinkuResponseDTO.LinkuCreateResult result =
                        linkuCreateService.createLinku(USER_ID, dtoWithUrl(SHORT_URL), null);

                ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
                verify(linkuUpsertService)
                        .upsert(urlCaptor.capture(), any(), any(), any(), any(), any(), any());
                assertEquals(TEST_URL, urlCaptor.getValue());
                assertEquals(SHORT_URL, result.data().linku());
            }

            @Test
            @DisplayName("페이지가 밝힌 canonical이 이미 저장된 링크면 AI 분석 없이 재사용한다")
            void 페이지가_밝힌_canonical이_이미_저장된_링크면_AI_분석_없이_재사용한다() throws Exception {
                Linku existingLinku = LinkuFixture.linku(CRAWLED_IMAGE_URL);
                ensureExistingLinkuClassification(existingLinku);
                setupExistingLinkMocks(existingLinku);
                String sharedUrl = TEST_URL + "?from=share";
                given(linkuRepository.findByLinku(sharedUrl)).willReturn(Optional.empty());
                given(robotsTxtChecker.isAllowed(any(), any())).willReturn(true);
                given(safeUrlFetcher.fetchPage(eq(sharedUrl), any(), anyInt()))
                        .willReturn(
                                page(
                                        sharedUrl,
                                        "<link rel=\"canonical\" href=\"" + TEST_URL + "\">",
                                        List.of()));

                linkuCreateService.createLinku(USER_ID, dtoWithUrl(sharedUrl), null);

                verify(geminiLinkuService, never()).analyzeByUrl(any(), any(), any(), any(), any());
                verify(linkuUpsertService, never())
                        .upsert(any(), any(), any(), any(), any(), any(), any());
            }

            @Test
            @DisplayName("직접 접속한 로그인 화면의 canonical은 대표 URL로 승격하지 않는다")
            void 직접_접속한_로그인_화면의_canonical은_대표_URL로_승격하지_않는다() throws Exception {
                setupNewLinkMocks(CRAWLED_IMAGE_URL);
                String loginUrl = "https://example.com/login";
                given(safeUrlFetcher.fetchPage(eq(TEST_URL), any(), anyInt()))
                        .willReturn(
                                page(
                                        TEST_URL,
                                        "<link rel=\"canonical\" href=\""
                                                + loginUrl
                                                + "\"><form><input type=\"password\" autocomplete=\"current-password\"></form>",
                                        List.of()));

                linkuCreateService.createLinku(USER_ID, defaultDto(), null);

                ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
                verify(linkuUpsertService)
                        .upsert(urlCaptor.capture(), any(), any(), any(), any(), any(), any());
                assertEquals(TEST_URL, urlCaptor.getValue());
            }

            @Test
            @DisplayName("네이버 블로그 PC 주소는 iframe을 한 번만 받아 AI 분석과 이미지 추출에 넘긴다")
            void 네이버_블로그_PC_주소는_iframe을_한_번만_받아_AI_분석과_이미지_추출에_넘긴다() throws Exception {
                setupNewLinkMocks(CRAWLED_IMAGE_URL);
                String blogUrl = "https://blog.naver.com/someuser/12345";
                String iframeUrl =
                        "https://blog.naver.com/PostView.naver?blogId=someuser&logNo=12345";
                Document outerDoc =
                        Jsoup.parse(
                                "<html><body><iframe id=\"mainFrame\" src=\"/PostView.naver?blogId=someuser&logNo=12345\"></iframe></body></html>",
                                blogUrl);
                Document iframeDoc =
                        Jsoup.parse(
                                "<html><head><meta property=\"og:url\" content=\""
                                        + blogUrl
                                        + "\"></head><body>본문</body></html>",
                                iframeUrl);
                given(safeUrlFetcher.fetchPage(eq(blogUrl), any(), anyInt()))
                        .willReturn(new SafeUrlFetcher.FetchedPage(outerDoc, blogUrl, List.of()));
                given(safeUrlFetcher.fetchDocument(eq(iframeUrl), any(), anyInt()))
                        .willReturn(iframeDoc);

                linkuCreateService.createLinku(USER_ID, dtoWithUrl(blogUrl), null);

                verify(safeUrlFetcher, times(1)).fetchDocument(eq(iframeUrl), any(), anyInt());
                verify(geminiLinkuService)
                        .analyzeByUrl(eq(iframeUrl), eq(iframeDoc), any(), any(), any());
                verify(linkToImageService)
                        .getRelatedImageFromUrl(eq(iframeUrl), any(), eq(iframeDoc));
            }

            @Test
            @DisplayName("네이버 블로그가 아닌 사이트는 mainFrame iframe이 있어도 추가로 요청하지 않는다")
            void 네이버_블로그가_아닌_사이트는_mainFrame_iframe이_있어도_추가로_요청하지_않는다() throws Exception {
                setupNewLinkMocks(CRAWLED_IMAGE_URL);
                given(safeUrlFetcher.fetchPage(eq(TEST_URL), any(), anyInt()))
                        .willReturn(
                                page(
                                        TEST_URL,
                                        "<iframe id=\"mainFrame\" src=\"https://ads.example.net/widget\"></iframe>",
                                        List.of()));

                linkuCreateService.createLinku(USER_ID, defaultDto(), null);

                verify(safeUrlFetcher, never()).fetchDocument(any(), any(), anyInt());
                verify(geminiLinkuService).analyzeByUrl(eq(TEST_URL), any(), any(), any(), any());
            }

            @Test
            @DisplayName("로그인 페이지로 도착해도 리다이렉트 규칙으로 대표 URL을 정하고 AI 결과를 쓰지 않는다")
            void 로그인_페이지로_도착해도_리다이렉트_규칙으로_대표_URL을_정하고_AI_결과를_쓰지_않는다() throws Exception {
                setupNewLinkMocks(CRAWLED_IMAGE_URL);
                String loginUrl = "https://example.com/member/signin";
                given(safeUrlFetcher.fetchPage(eq(TEST_URL), any(), anyInt()))
                        .willReturn(
                                page(
                                        loginUrl,
                                        "",
                                        List.of(
                                                new SafeUrlFetcher.RedirectHop(
                                                        TEST_URL, 302, loginUrl))));
                given(geminiLinkuService.analyzeByUrl(any(), any(), any(), any(), any()))
                        .willReturn(
                                Optional.of(
                                        new LinkuResultDTO(
                                                1L,
                                                "#로그인",
                                                "로그인",
                                                SITUATION_ID,
                                                EMOTION_ID,
                                                "LOGIN")));

                linkuCreateService.createLinku(USER_ID, defaultDto(), null);

                ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
                ArgumentCaptor<String> titleCaptor = ArgumentCaptor.forClass(String.class);
                verify(linkuUpsertService)
                        .upsert(
                                urlCaptor.capture(),
                                any(),
                                any(),
                                titleCaptor.capture(),
                                any(),
                                any(),
                                any());
                assertEquals(TEST_URL, urlCaptor.getValue());
                assertEquals("example.com/article", titleCaptor.getValue());
            }

            @Test
            @DisplayName("새 링크 저장 시 AI가 판별한 페이지 종류를 링크에 남긴다")
            void 새_링크_저장_시_AI가_판별한_페이지_종류를_링크에_남긴다() throws Exception {
                setupNewLinkMocks(CRAWLED_IMAGE_URL);
                Linku savedLinku = LinkuFixture.linku(null);
                ensureExistingLinkuClassification(savedLinku);
                given(linkuUpsertService.upsert(any(), any(), any(), any(), any(), any(), any()))
                        .willReturn(savedLinku);
                given(geminiLinkuService.analyzeByUrl(any(), any(), any(), any(), any()))
                        .willReturn(
                                Optional.of(
                                        new LinkuResultDTO(
                                                1L,
                                                "#로그인",
                                                "로그인",
                                                SITUATION_ID,
                                                EMOTION_ID,
                                                "LOGIN")));

                linkuCreateService.createLinku(USER_ID, defaultDto(), null);

                assertEquals(PageType.LOGIN, savedLinku.getPageType());
            }

            @Test
            @DisplayName("오류 페이지로 영구 이동하면 AI 판별과 무관하게 영구 이동 URL로 저장한다")
            void 오류_페이지로_영구_이동하면_AI_판별과_무관하게_영구_이동_URL로_저장한다() throws Exception {
                setupNewLinkMocks(CRAWLED_IMAGE_URL);
                String errorUrl = "https://example.com/notice/deleted";
                given(safeUrlFetcher.fetchPage(eq(TEST_URL), any(), anyInt()))
                        .willReturn(
                                page(
                                        errorUrl,
                                        "",
                                        List.of(
                                                new SafeUrlFetcher.RedirectHop(
                                                        TEST_URL, 301, errorUrl))));
                given(geminiLinkuService.analyzeByUrl(any(), any(), any(), any(), any()))
                        .willReturn(
                                Optional.of(
                                        new LinkuResultDTO(
                                                1L,
                                                "#오류",
                                                "삭제된 글",
                                                SITUATION_ID,
                                                EMOTION_ID,
                                                "ERROR")));

                linkuCreateService.createLinku(USER_ID, defaultDto(), null);

                ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
                verify(linkuUpsertService)
                        .upsert(urlCaptor.capture(), any(), any(), any(), any(), any(), any());
                assertEquals(errorUrl, urlCaptor.getValue());
            }

            @Test
            @DisplayName("robots.txt가 막은 사이트는 본문을 받지 않고 AI 분석을 하지 않는다")
            void robots_txt가_막은_사이트는_본문을_받지_않고_AI_분석을_하지_않는다() throws Exception {
                setupNewLinkMocks(CRAWLED_IMAGE_URL);
                given(robotsTxtChecker.isAllowed(any(), any())).willReturn(false);
                given(safeUrlFetcher.probe(TEST_URL))
                        .willReturn(
                                Optional.of(new SafeUrlFetcher.ProbeResult(TEST_URL, List.of())));

                LinkuResponseDTO.LinkuCreateResult result =
                        linkuCreateService.createLinku(USER_ID, defaultDto(), null);

                verify(safeUrlFetcher, never()).fetchPage(any(), any(), anyInt());
                verify(geminiLinkuService, never()).analyzeByUrl(any(), any(), any(), any(), any());
                assertTrue(result.validUrl());
            }

            @Test
            @DisplayName("새 링크 저장 시 자동 등록된 도메인으로 링크를 저장한다")
            void 새_링크_저장_시_자동_등록된_도메인으로_링크를_저장한다() {
                setupNewLinkMocks(CRAWLED_IMAGE_URL);
                Domain autoDomain =
                        Domain.builder()
                                .domainId(77L)
                                .name("example")
                                .domainTail("example.com")
                                .build();
                given(domainAutoRegisterService.resolveOrRegister(TEST_URL))
                        .willReturn(Optional.of(autoDomain));

                linkuCreateService.createLinku(USER_ID, defaultDto(), null);

                verify(linkuUpsertService)
                        .upsert(any(), any(), eq(autoDomain), any(), any(), any(), any());
            }

            @Test
            @DisplayName("이미 저장된 링크는 도메인 자동 등록을 하지 않는다")
            void 이미_저장된_링크는_도메인_자동_등록을_하지_않는다() {
                Linku existingLinku = LinkuFixture.linku(CRAWLED_IMAGE_URL);
                ensureExistingLinkuClassification(existingLinku);
                setupExistingLinkMocks(existingLinku);

                linkuCreateService.createLinku(USER_ID, defaultDto(), null);

                verify(domainAutoRegisterService, never()).resolveOrRegister(any());
            }

            @Test
            @DisplayName("AI 분석 중 예외가 나도 링크는 저장된다")
            void AI_분석_중_예외가_나도_링크는_저장된다() {
                setupNewLinkMocks(CRAWLED_IMAGE_URL);
                given(geminiLinkuService.analyzeByUrl(any(), any(), any(), any(), any()))
                        .willThrow(new RuntimeException("gemini timeout"));

                LinkuResponseDTO.LinkuCreateResult result =
                        linkuCreateService.createLinku(USER_ID, defaultDto(), null);

                verify(linkuUpsertService)
                        .upsert(eq(TEST_URL), any(), any(), any(), any(), any(), any());
                assertTrue(result.validUrl());
            }
        }

        @Nested
        @DisplayName("실패")
        class Failure {

            @Test
            @DisplayName("단축 URL이 영상 링크로 풀리면 예외가 발생한다")
            void 단축_URL이_영상_링크로_풀리면_예외가_발생한다() throws Exception {
                String videoUrl = "https://www.youtube.com/watch?v=abc123";
                given(robotsTxtChecker.isAllowed(any(), any())).willReturn(true);
                given(safeUrlFetcher.fetchPage(eq(SHORT_URL), any(), anyInt()))
                        .willReturn(
                                page(
                                        videoUrl,
                                        "",
                                        List.of(
                                                new SafeUrlFetcher.RedirectHop(
                                                        SHORT_URL, 301, videoUrl))));

                GeneralException ex =
                        assertThrows(
                                GeneralException.class,
                                () ->
                                        linkuCreateService.createLinku(
                                                USER_ID, dtoWithUrl(SHORT_URL), null));
                assertEquals(LinkuErrorStatus._LINKU_VIDEO_NOT_ALLOWED, ex.getCode());
            }
        }
    }

    @Nested
    @DisplayName("URL 검증")
    class UrlValidation {

        @Nested
        @DisplayName("실패")
        class Failure {

            @Test
            @DisplayName("비디오 링크 입력 시 예외가 발생한다")
            void 비디오_링크_입력_시_예외가_발생한다() {
                LinkuRequestDTO.LinkuCreateDTO dto =
                        LinkuRequestDTO.LinkuCreateDTO.builder()
                                .linku("https://www.youtube.com/watch?v=abc123")
                                .emotionId(EMOTION_ID)
                                .situationId(SITUATION_ID)
                                .build();

                GeneralException ex =
                        assertThrows(
                                GeneralException.class,
                                () -> linkuCreateService.createLinku(USER_ID, dto, null));
                assertEquals(LinkuErrorStatus._LINKU_VIDEO_NOT_ALLOWED, ex.getCode());
            }

            @Test
            @DisplayName("잘못된 URL 형식 입력 시 예외가 발생한다")
            void 잘못된_URL_형식_입력_시_예외가_발생한다() {
                LinkuRequestDTO.LinkuCreateDTO dto =
                        LinkuRequestDTO.LinkuCreateDTO.builder()
                                .linku("not-a-valid-url")
                                .emotionId(EMOTION_ID)
                                .situationId(SITUATION_ID)
                                .build();

                GeneralException ex =
                        assertThrows(
                                GeneralException.class,
                                () -> linkuCreateService.createLinku(USER_ID, dto, null));
                assertEquals(LinkuErrorStatus._LINKU_INVALID_URL, ex.getCode());
            }
        }
    }

    @Nested
    @DisplayName("saved_url alias 재사용")
    class SavedUrlAliasReuse {

        @Test
        @DisplayName("linkus에 없고 saved_url alias가 있으면 외부 요청 없이 기존 링크를 재사용한다")
        void linkus에_없고_saved_url_alias가_있으면_외부_요청_없이_기존_링크를_재사용한다() {
            Linku existingLinku = LinkuFixture.linku(CRAWLED_IMAGE_URL);
            given(linkuRepository.findByLinku(TEST_URL)).willReturn(Optional.empty());
            given(usersLinkuRepository.findLatestLinkuIdBySavedUrl(TEST_URL))
                    .willReturn(Optional.of(42L));
            given(linkuRepository.findById(42L)).willReturn(Optional.of(existingLinku));

            LinkResolution resolution = linkResolver.resolve(TEST_URL);

            assertEquals(existingLinku, resolution.existing().orElseThrow());
            verify(safeUrlFetcher, never()).fetchPage(any(), any(), anyInt());
            verify(robotsTxtChecker, never()).isAllowed(any(), any());
            verify(geminiLinkuService, never()).analyzeByUrl(any(), any(), any(), any(), any());
            verify(linkuMetrics).resolved(ResolveOutcome.ALIAS_HIT);
        }

        @Test
        @DisplayName("linkus 직접 hit면 saved_url alias를 조회하지 않는다")
        void linkus_직접_hit면_saved_url_alias를_조회하지_않는다() {
            Linku existingLinku = LinkuFixture.linku(CRAWLED_IMAGE_URL);
            given(linkuRepository.findByLinku(TEST_URL)).willReturn(Optional.of(existingLinku));

            LinkResolution resolution = linkResolver.resolve(TEST_URL);

            assertEquals(existingLinku, resolution.existing().orElseThrow());
            verify(usersLinkuRepository, never()).findLatestLinkuIdBySavedUrl(any());
            verify(linkuMetrics).resolved(ResolveOutcome.INPUT_HIT);
        }

        @Test
        @DisplayName("saved_url alias도 없으면 기존 신규 링크 해석 경로로 진행한다")
        void saved_url_alias도_없으면_기존_신규_링크_해석_경로로_진행한다() {
            setupNewLinkMocks(CRAWLED_IMAGE_URL);
            given(usersLinkuRepository.findLatestLinkuIdBySavedUrl(TEST_URL))
                    .willReturn(Optional.empty());

            linkResolver.resolve(TEST_URL);

            verify(usersLinkuRepository).findLatestLinkuIdBySavedUrl(TEST_URL);
            verify(safeUrlFetcher).fetchPage(eq(TEST_URL), any(), anyInt());
            verify(linkuMetrics).resolved(ResolveOutcome.NEW_LINK);
        }
    }

    @Nested
    @DisplayName("createUsersLinku - AI 요약 존재 여부 상속")
    class CreateUsersLinkuAiExistInheritance {

        @Test
        @DisplayName("본인이_이전에_같은_링크를_저장하며_AI_요약을_확인한_적_있으면_새_UsersLinku도_aiExist가_true로_생성된다")
        void 본인이_이전에_같은_링크를_저장하며_AI_요약을_확인한_적_있으면_새_UsersLinku도_aiExist가_true로_생성된다() {
            Linku linku = LinkuFixture.linku(null);
            UsersLinku previousSave =
                    UsersLinku.builder()
                            .user(LinkuFixture.user())
                            .linku(linku)
                            .aiExist(true)
                            .build();

            given(
                            usersLinkuRepository.findByUser_IdAndLinku_LinkuId(
                                    LinkuFixture.USER_ID, linku.getLinkuId()))
                    .willReturn(List.of(previousSave));
            given(usersLinkuRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            UsersLinku result =
                    linkuPersister.createUsersLinku(
                            LinkuFixture.user(),
                            linku,
                            LinkuFixture.emotion(),
                            LinkuFixture.situation(),
                            linku.getLinkuUrl(),
                            null,
                            null,
                            "2번째 저장",
                            true,
                            true);

            assertTrue(result.getAiExist());
        }

        @Test
        @DisplayName("본인의_이전_저장_이력이_없으면_UsersLinku는_aiExist가_false로_생성된다")
        void 본인의_이전_저장_이력이_없으면_UsersLinku는_aiExist가_false로_생성된다() {
            Linku linku = LinkuFixture.linku(null);

            given(
                            usersLinkuRepository.findByUser_IdAndLinku_LinkuId(
                                    LinkuFixture.USER_ID, linku.getLinkuId()))
                    .willReturn(List.of());
            given(usersLinkuRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            UsersLinku result =
                    linkuPersister.createUsersLinku(
                            LinkuFixture.user(),
                            linku,
                            LinkuFixture.emotion(),
                            LinkuFixture.situation(),
                            linku.getLinkuUrl(),
                            null,
                            null,
                            "1번째 저장",
                            true,
                            true);

            assertFalse(result.getAiExist());
        }

        @Test
        @DisplayName("다른_유저가_이미_이_링크를_요약해뒀어도_본인이_요청_조회한_적_없으면_aiExist가_false로_생성된다")
        void 다른_유저가_이미_이_링크를_요약해뒀어도_본인이_요청_조회한_적_없으면_aiExist가_false로_생성된다() {
            Linku linku = LinkuFixture.linku(null);
            // 다른 유저가 저장하며 이미 AI 요약을 확인해둔 상태 - 본인의 이력이 아니므로 조회 대상이 아니다.
            given(
                            usersLinkuRepository.findByUser_IdAndLinku_LinkuId(
                                    LinkuFixture.USER_ID, linku.getLinkuId()))
                    .willReturn(List.of());
            given(usersLinkuRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            UsersLinku result =
                    linkuPersister.createUsersLinku(
                            LinkuFixture.user(),
                            linku,
                            LinkuFixture.emotion(),
                            LinkuFixture.situation(),
                            linku.getLinkuUrl(),
                            null,
                            null,
                            "1번째 저장",
                            true,
                            true);

            assertFalse(result.getAiExist());
        }
    }

    @Nested
    @DisplayName("resolveDomain - 도메인 tail 계층 매칭")
    class ResolveDomainHierarchy {

        @Test
        @DisplayName("정확히 일치하는 도메인이 있으면 apex 후보는 조회하지 않고 그대로 사용한다")
        void 정확히_일치하면_해당_도메인을_우선_사용한다() {
            Domain exact = Domain.builder().name("네이버 블로그").domainTail("blog.naver.com").build();
            given(domainRepository.findByDomainTail("blog.naver.com"))
                    .willReturn(Optional.of(exact));

            Domain result = linkuPersister.resolveDomain(List.of("blog.naver.com", "naver.com"));

            assertEquals(exact, result);
            verify(domainRepository, never()).findByDomainTail("naver.com");
        }

        @Test
        @DisplayName("정확히 일치하는 도메인이 없으면 registry-suffix apex 도메인으로 폴백한다")
        void 정확히_일치하지_않으면_apex_도메인으로_폴백한다() {
            given(domainRepository.findByDomainTail("someuser.tistory.com"))
                    .willReturn(Optional.empty());
            Domain apex = Domain.builder().name("티스토리").domainTail("tistory.com").build();
            given(domainRepository.findByDomainTail("tistory.com")).willReturn(Optional.of(apex));

            Domain result =
                    linkuPersister.resolveDomain(List.of("someuser.tistory.com", "tistory.com"));

            assertEquals(apex, result);
        }

        @Test
        @DisplayName("아무 후보도 매칭되지 않으면 기본 도메인으로 폴백한다")
        void 매칭되는_후보가_없으면_기본_도메인을_사용한다() {
            given(domainRepository.findByDomainTail(any())).willReturn(Optional.empty());
            Domain defaultDomain = LinkuFixture.domain();
            given(domainRepository.findById(1L)).willReturn(Optional.of(defaultDomain));

            Domain result = linkuPersister.resolveDomain(List.of("unknown.example"));

            assertEquals(defaultDomain, result);
        }

        @Test
        @DisplayName("후보 목록이 비어 있으면(호스트 파싱 실패) 기본 도메인으로 폴백한다")
        void 후보가_비어있으면_기본_도메인을_사용한다() {
            Domain defaultDomain = LinkuFixture.domain();
            given(domainRepository.findById(1L)).willReturn(Optional.of(defaultDomain));

            Domain result = linkuPersister.resolveDomain(List.of());

            assertEquals(defaultDomain, result);
            verify(domainRepository, never()).findByDomainTail(any());
        }
    }

    private void setupNewLinkMocks(String crawledImageUrl) {
        lenient().when(linkuRepository.findByLinku(TEST_URL)).thenReturn(Optional.empty());
        // 기본: robots 허용 + 요청한 URL 그대로 200 (리다이렉트/canonical 없음)
        lenient().when(robotsTxtChecker.isAllowed(any(), any())).thenReturn(true);
        try {
            lenient()
                    .when(safeUrlFetcher.fetchPage(any(), any(), anyInt()))
                    .thenAnswer(
                            inv -> {
                                String url = inv.getArgument(0, String.class);
                                // 이후 특정 URL stub을 등록할 때 Mockito matcher가 null을 전달해도 기본 stub이 예외를 내지
                                // 않게 한다.
                                return page(url != null ? url : TEST_URL, "", List.of());
                            });
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }

        // 가짜 AI 분석 결과 DTO 주입
        LinkuResultDTO mockAiResult =
                new LinkuResultDTO(1L, "테스트 제목", "키워드1, 키워드2", EMOTION_ID, SITUATION_ID);
        lenient()
                .when(geminiLinkuService.analyzeByUrl(any(), any(), any(), any(), any()))
                .thenReturn(Optional.of(mockAiResult));

        lenient()
                .when(categoryRepository.findById(any()))
                .thenAnswer(inv -> Optional.of(LinkuFixture.category()));

        lenient()
                .when(domainRepository.findByDomainTail(any()))
                .thenAnswer(inv -> Optional.of(LinkuFixture.domain()));
        lenient()
                .when(domainRepository.findById(anyLong()))
                .thenAnswer(inv -> Optional.of(LinkuFixture.domain()));

        lenient()
                .when(linkToImageService.getRelatedImageFromUrl(eq(TEST_URL), any(), any()))
                .thenReturn(crawledImageUrl);

        // linkuUpsertService.upsert() 결과 모킹 추가
        Linku mockLinku = LinkuFixture.linku(crawledImageUrl);
        if (mockLinku.getEmotion() == null) mockLinku.updateEmotion(LinkuFixture.emotion());
        if (mockLinku.getSituation() == null) mockLinku.updateSituation(LinkuFixture.situation());

        lenient()
                .when(linkuUpsertService.upsert(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(mockLinku);

        lenient().when(aiArticleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient()
                .when(userRepository.findById(USER_ID))
                .thenReturn(Optional.of(LinkuFixture.user()));

        lenient()
                .when(emotionRepository.findById(anyLong()))
                .thenAnswer(inv -> Optional.of(LinkuFixture.emotion()));

        lenient()
                .when(situationRepository.findById(anyLong()))
                .thenAnswer(inv -> Optional.of(LinkuFixture.situation()));

        lenient()
                .when(
                        usersFolderRepository.findFolderByUserIdAndCategory(
                                eq(USER_ID), any(Category.class)))
                .thenReturn(Optional.of(mock(com.umc.linkyou.domain.folder.Folder.class)));

        lenient().when(usersLinkuRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(linkuFolderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private void setupExistingLinkMocks(Linku existingLinku) {
        lenient()
                .when(linkuRepository.findByLinku(TEST_URL))
                .thenReturn(Optional.of(existingLinku));
        lenient()
                .when(userRepository.findById(USER_ID))
                .thenReturn(Optional.of(LinkuFixture.user()));
        lenient()
                .when(emotionRepository.findById(EMOTION_ID))
                .thenReturn(Optional.of(LinkuFixture.emotion()));
        lenient()
                .when(emotionRepository.findById(anyLong()))
                .thenReturn(Optional.of(LinkuFixture.emotion()));
        lenient()
                .when(situationRepository.findById(SITUATION_ID))
                .thenReturn(Optional.of(LinkuFixture.situation()));
        lenient()
                .when(situationRepository.findById(anyLong()))
                .thenReturn(Optional.of(LinkuFixture.situation()));
        lenient()
                .when(domainRepository.findByDomainTail("example.com"))
                .thenReturn(Optional.of(LinkuFixture.domain()));
        lenient()
                .when(domainRepository.findByDomainTail(any()))
                .thenReturn(Optional.of(LinkuFixture.domain()));
        lenient()
                .when(domainRepository.findById(anyLong()))
                .thenReturn(Optional.of(LinkuFixture.domain()));

        lenient()
                .when(
                        usersFolderRepository.findFolderByUserIdAndCategory(
                                eq(USER_ID), any(Category.class)))
                .thenReturn(Optional.of(mock(com.umc.linkyou.domain.folder.Folder.class)));

        lenient().when(usersLinkuRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(linkuFolderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private void ensureExistingLinkuClassification(Linku existingLinku) {
        if (existingLinku.getEmotion() == null) {
            existingLinku.updateEmotion(LinkuFixture.emotion());
        }
        if (existingLinku.getSituation() == null) {
            existingLinku.updateSituation(LinkuFixture.situation());
        }
    }

    // finalUrl에 도착한 페이지. head에는 canonical/og:url 같은 태그를 넣는다.
    private static SafeUrlFetcher.FetchedPage page(
            String finalUrl, String head, List<SafeUrlFetcher.RedirectHop> hops) {
        Document doc =
                Jsoup.parse("<html><head>" + head + "</head><body>본문</body></html>", finalUrl);
        return new SafeUrlFetcher.FetchedPage(doc, finalUrl, hops);
    }

    private LinkuRequestDTO.LinkuCreateDTO dtoWithUrl(String url) {
        return LinkuRequestDTO.LinkuCreateDTO.builder()
                .linku(url)
                .emotionId(EMOTION_ID)
                .situationId(SITUATION_ID)
                .build();
    }

    private LinkuRequestDTO.LinkuCreateDTO defaultDto() {
        return LinkuRequestDTO.LinkuCreateDTO.builder()
                .linku(TEST_URL)
                .emotionId(EMOTION_ID)
                .situationId(SITUATION_ID)
                .build();
    }
}
