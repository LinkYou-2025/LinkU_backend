package com.umc.linkyou.service.Linku;

import com.umc.linkyou.apiPayload.code.status.linku.LinkuErrorStatus;
import com.umc.linkyou.apiPayload.exception.GeneralException;
import com.umc.linkyou.awss3.AwsS3Service;
import com.umc.linkyou.domain.Linku;
import com.umc.linkyou.domain.classification.Category;
import com.umc.linkyou.domain.classification.Domain;
import com.umc.linkyou.domain.mapping.UsersLinku;
import com.umc.linkyou.infra.ai.dto.LinkuResultDTO;
import com.umc.linkyou.infra.gemini.service.GeminiLinkuService;
import com.umc.linkyou.infra.net.SafeUrlFetcher;
import com.umc.linkyou.infra.parser.LinkToImageService;
import com.umc.linkyou.repository.EmotionRepository;
import com.umc.linkyou.repository.UserLinkuRepository.UsersLinkuRepository;
import com.umc.linkyou.repository.aiArticleRepository.AiArticleRepository;
import com.umc.linkyou.repository.classification.CategoryRepository;
import com.umc.linkyou.repository.classification.SituationRepository;
import com.umc.linkyou.repository.classification.domainRepository.DomainRepository;
import com.umc.linkyou.repository.linkuRepository.LinkuRepository;
import com.umc.linkyou.repository.mapping.linkuFolderRepository.LinkuFolderRepository;
import com.umc.linkyou.repository.recommend.UserProfileRefreshQueueRepository;
import com.umc.linkyou.repository.userRepository.UserRepository;
import com.umc.linkyou.repository.usersFolderRepository.UsersFolderRepository;
import com.umc.linkyou.service.folder.FolderService;
import com.umc.linkyou.service.keyword.KeywordService;
import com.umc.linkyou.support.fixture.LinkuFixture;
import com.umc.linkyou.web.dto.linku.LinkuRequestDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

import static com.umc.linkyou.support.fixture.LinkuFixture.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.lenient;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("LinkuCreateService 테스트")
class LinkuCreateServiceTest {

    @InjectMocks
    private LinkuCreateService linkuCreateService;

    @Mock private LinkuRepository linkuRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private EmotionRepository emotionRepository;
    @Mock private DomainRepository domainRepository;
    @Mock private LinkuFolderRepository linkuFolderRepository;
    @Mock private UsersLinkuRepository usersLinkuRepository;
    @Mock private UserRepository userRepository;
    @Mock private AwsS3Service awsS3Service;
    @Mock private LinkToImageService linkToImageService;
    @Mock private SituationRepository situationRepository;
    @Mock private AiArticleRepository aiArticleRepository;
    @Mock private GeminiLinkuService geminiLinkuService;
    @Mock private FolderService folderService;
    @Mock private KeywordService keywordService;
    @Mock private UsersFolderRepository usersFolderRepository;
    @Mock private UserProfileRefreshQueueRepository userProfileRefreshQueueRepository;

    // 의존성 주입을 위한 Upsert 서비스 Mock 추가
    @Mock private LinkuUpsertService linkuUpsertService;
    // SSRF 검증용 SafeUrlFetcher - validUrl 필드 계산에만 쓰이고 이 테스트들은 그 값을 검증하지 않으므로 스텁 없이 기본값(false)만 사용
    @Mock private SafeUrlFetcher safeUrlFetcher;
    // DB 쓰기 구간만 감싸는 프로그래밍 방식 트랜잭션 - 목에서는 콜백을 그대로 실행해주기만 하면 된다.
    @Mock private TransactionTemplate transactionTemplate;

    @BeforeEach
    void setUpTransactionTemplate() {
        lenient().doAnswer(invocation -> {
                    TransactionCallback<?> callback = invocation.getArgument(0);
                    return callback.doInTransaction(null);
                })
                .when(transactionTemplate).execute(any());
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
                verify(linkuUpsertService).upsert(any(), any(), any(), any(), imgUrlCaptor.capture(), any(), any());
                assertEquals(CRAWLED_IMAGE_URL, imgUrlCaptor.getValue());

                ArgumentCaptor<UsersLinku> usersLinkuCaptor = ArgumentCaptor.forClass(UsersLinku.class);
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
                verify(linkuUpsertService).upsert(any(), any(), any(), any(), imgUrlCaptor.capture(), any(), any());
                assertEquals(CRAWLED_IMAGE_URL, imgUrlCaptor.getValue());

                ArgumentCaptor<UsersLinku> usersLinkuCaptor = ArgumentCaptor.forClass(UsersLinku.class);
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
                verify(linkuUpsertService).upsert(any(), any(), any(), any(), imgUrlCaptor.capture(), any(), any());
                assertNull(imgUrlCaptor.getValue());

                ArgumentCaptor<UsersLinku> usersLinkuCaptor = ArgumentCaptor.forClass(UsersLinku.class);
                verify(usersLinkuRepository).save(usersLinkuCaptor.capture());
                assertEquals(EMOTION_ID, usersLinkuCaptor.getValue().getEmotion().getEmotionId());
            }
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
                verify(linkuUpsertService, never()).upsert(any(), any(), any(), any(), any(), any(), any());
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
                LinkuRequestDTO.LinkuCreateDTO dto = LinkuRequestDTO.LinkuCreateDTO.builder()
                        .linku("https://www.youtube.com/watch?v=abc123")
                        .emotionId(EMOTION_ID)
                        .situationId(SITUATION_ID)
                        .build();

                GeneralException ex = assertThrows(GeneralException.class,
                        () -> linkuCreateService.createLinku(USER_ID, dto, null));
                assertEquals(LinkuErrorStatus._LINKU_VIDEO_NOT_ALLOWED, ex.getCode());
            }

            @Test
            @DisplayName("잘못된 URL 형식 입력 시 예외가 발생한다")
            void 잘못된_URL_형식_입력_시_예외가_발생한다() {
                LinkuRequestDTO.LinkuCreateDTO dto = LinkuRequestDTO.LinkuCreateDTO.builder()
                        .linku("not-a-valid-url")
                        .emotionId(EMOTION_ID)
                        .situationId(SITUATION_ID)
                        .build();

                GeneralException ex = assertThrows(GeneralException.class,
                        () -> linkuCreateService.createLinku(USER_ID, dto, null));
                assertEquals(LinkuErrorStatus._LINKU_INVALID_URL, ex.getCode());
            }
        }
    }

    @Nested
    @DisplayName("createUsersLinku - AI 요약 존재 여부 상속")
    class CreateUsersLinkuAiExistInheritance {

        @Test
        @DisplayName("본인이_이전에_같은_링크를_저장하며_AI_요약을_확인한_적_있으면_새_UsersLinku도_aiExist가_true로_생성된다")
        void 본인이_이전에_같은_링크를_저장하며_AI_요약을_확인한_적_있으면_새_UsersLinku도_aiExist가_true로_생성된다() {
            Linku linku = LinkuFixture.linku(null);
            UsersLinku previousSave = UsersLinku.builder()
                    .user(LinkuFixture.user()).linku(linku).aiExist(true).build();

            given(usersLinkuRepository.findByUser_IdAndLinku_LinkuId(LinkuFixture.USER_ID, linku.getLinkuId()))
                    .willReturn(List.of(previousSave));
            given(usersLinkuRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            UsersLinku result = linkuCreateService.createUsersLinku(
                    LinkuFixture.user(), linku, LinkuFixture.emotion(), LinkuFixture.situation(),
                    null, null, "2번째 저장", true, true);

            assertTrue(result.getAiExist());
        }

        @Test
        @DisplayName("본인의_이전_저장_이력이_없으면_UsersLinku는_aiExist가_false로_생성된다")
        void 본인의_이전_저장_이력이_없으면_UsersLinku는_aiExist가_false로_생성된다() {
            Linku linku = LinkuFixture.linku(null);

            given(usersLinkuRepository.findByUser_IdAndLinku_LinkuId(LinkuFixture.USER_ID, linku.getLinkuId()))
                    .willReturn(List.of());
            given(usersLinkuRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            UsersLinku result = linkuCreateService.createUsersLinku(
                    LinkuFixture.user(), linku, LinkuFixture.emotion(), LinkuFixture.situation(),
                    null, null, "1번째 저장", true, true);

            assertFalse(result.getAiExist());
        }

        @Test
        @DisplayName("다른_유저가_이미_이_링크를_요약해뒀어도_본인이_요청_조회한_적_없으면_aiExist가_false로_생성된다")
        void 다른_유저가_이미_이_링크를_요약해뒀어도_본인이_요청_조회한_적_없으면_aiExist가_false로_생성된다() {
            Linku linku = LinkuFixture.linku(null);
            // 다른 유저가 저장하며 이미 AI 요약을 확인해둔 상태 - 본인의 이력이 아니므로 조회 대상이 아니다.
            given(usersLinkuRepository.findByUser_IdAndLinku_LinkuId(LinkuFixture.USER_ID, linku.getLinkuId()))
                    .willReturn(List.of());
            given(usersLinkuRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            UsersLinku result = linkuCreateService.createUsersLinku(
                    LinkuFixture.user(), linku, LinkuFixture.emotion(), LinkuFixture.situation(),
                    null, null, "1번째 저장", true, true);

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
            given(domainRepository.findByDomainTail("blog.naver.com")).willReturn(Optional.of(exact));

            Domain result = linkuCreateService.resolveDomain(List.of("blog.naver.com", "naver.com"));

            assertEquals(exact, result);
            verify(domainRepository, never()).findByDomainTail("naver.com");
        }

        @Test
        @DisplayName("정확히 일치하는 도메인이 없으면 registry-suffix apex 도메인으로 폴백한다")
        void 정확히_일치하지_않으면_apex_도메인으로_폴백한다() {
            given(domainRepository.findByDomainTail("someuser.tistory.com")).willReturn(Optional.empty());
            Domain apex = Domain.builder().name("티스토리").domainTail("tistory.com").build();
            given(domainRepository.findByDomainTail("tistory.com")).willReturn(Optional.of(apex));

            Domain result = linkuCreateService.resolveDomain(List.of("someuser.tistory.com", "tistory.com"));

            assertEquals(apex, result);
        }

        @Test
        @DisplayName("아무 후보도 매칭되지 않으면 기본 도메인으로 폴백한다")
        void 매칭되는_후보가_없으면_기본_도메인을_사용한다() {
            given(domainRepository.findByDomainTail(any())).willReturn(Optional.empty());
            Domain defaultDomain = LinkuFixture.domain();
            given(domainRepository.findById(1L)).willReturn(Optional.of(defaultDomain));

            Domain result = linkuCreateService.resolveDomain(List.of("unknown.example"));

            assertEquals(defaultDomain, result);
        }

        @Test
        @DisplayName("후보 목록이 비어 있으면(호스트 파싱 실패) 기본 도메인으로 폴백한다")
        void 후보가_비어있으면_기본_도메인을_사용한다() {
            Domain defaultDomain = LinkuFixture.domain();
            given(domainRepository.findById(1L)).willReturn(Optional.of(defaultDomain));

            Domain result = linkuCreateService.resolveDomain(List.of());

            assertEquals(defaultDomain, result);
            verify(domainRepository, never()).findByDomainTail(any());
        }
    }

    private void setupNewLinkMocks(String crawledImageUrl) {
        lenient().when(linkuRepository.findByLinku(TEST_URL)).thenReturn(Optional.empty());

        // 가짜 AI 분석 결과 DTO 주입
        LinkuResultDTO mockAiResult = new LinkuResultDTO(
                1L,
                "테스트 제목",
                "키워드1, 키워드2",
                EMOTION_ID,
                SITUATION_ID
        );
        lenient().when(geminiLinkuService.analyzeByUrl(any(), any(), any(), any())).thenReturn(Optional.of(mockAiResult));

        lenient().when(categoryRepository.findById(any()))
                .thenAnswer(inv -> Optional.of(LinkuFixture.category()));

        lenient().when(domainRepository.findByDomainTail(any()))
                .thenAnswer(inv -> Optional.of(LinkuFixture.domain()));
        lenient().when(domainRepository.findById(anyLong()))
                .thenAnswer(inv -> Optional.of(LinkuFixture.domain()));

        lenient().when(linkToImageService.getRelatedImageFromUrl(eq(TEST_URL), any())).thenReturn(crawledImageUrl);

        // linkuUpsertService.upsert() 결과 모킹 추가
        Linku mockLinku = LinkuFixture.linku(crawledImageUrl);
        if (mockLinku.getEmotion() == null) mockLinku.updateEmotion(LinkuFixture.emotion());
        if (mockLinku.getSituation() == null) mockLinku.updateSituation(LinkuFixture.situation());

        lenient().when(linkuUpsertService.upsert(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(mockLinku);

        lenient().when(aiArticleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(userRepository.findById(USER_ID)).thenReturn(Optional.of(LinkuFixture.user()));

        lenient().when(emotionRepository.findById(anyLong()))
                .thenAnswer(inv -> Optional.of(LinkuFixture.emotion()));

        lenient().when(situationRepository.findById(anyLong()))
                .thenAnswer(inv -> Optional.of(LinkuFixture.situation()));

        lenient().when(usersFolderRepository.findFolderByUserIdAndCategory(eq(USER_ID), any(Category.class)))
                .thenReturn(Optional.of(mock(com.umc.linkyou.domain.folder.Folder.class)));

        lenient().when(usersLinkuRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(linkuFolderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private void setupExistingLinkMocks(Linku existingLinku) {
        lenient().when(linkuRepository.findByLinku(TEST_URL)).thenReturn(Optional.of(existingLinku));
        lenient().when(userRepository.findById(USER_ID)).thenReturn(Optional.of(LinkuFixture.user()));
        lenient().when(emotionRepository.findById(EMOTION_ID)).thenReturn(Optional.of(LinkuFixture.emotion()));
        lenient().when(emotionRepository.findById(anyLong())).thenReturn(Optional.of(LinkuFixture.emotion()));
        lenient().when(situationRepository.findById(SITUATION_ID)).thenReturn(Optional.of(LinkuFixture.situation()));
        lenient().when(situationRepository.findById(anyLong())).thenReturn(Optional.of(LinkuFixture.situation()));
        lenient().when(domainRepository.findByDomainTail("example.com")).thenReturn(Optional.of(LinkuFixture.domain()));
        lenient().when(domainRepository.findByDomainTail(any())).thenReturn(Optional.of(LinkuFixture.domain()));
        lenient().when(domainRepository.findById(anyLong())).thenReturn(Optional.of(LinkuFixture.domain()));

        lenient().when(usersFolderRepository.findFolderByUserIdAndCategory(eq(USER_ID), any(Category.class)))
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

    private LinkuRequestDTO.LinkuCreateDTO defaultDto() {
        return LinkuRequestDTO.LinkuCreateDTO.builder()
                .linku(TEST_URL)
                .emotionId(EMOTION_ID)
                .situationId(SITUATION_ID)
                .build();
    }
}
