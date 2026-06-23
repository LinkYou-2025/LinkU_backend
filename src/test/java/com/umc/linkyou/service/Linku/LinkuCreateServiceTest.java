package com.umc.linkyou.service.Linku;

import com.umc.linkyou.apiPayload.code.status.linku.LinkuErrorStatus;
import com.umc.linkyou.apiPayload.exception.GeneralException;
import com.umc.linkyou.awss3.AwsS3Service;
import com.umc.linkyou.domain.Linku;
import com.umc.linkyou.domain.mapping.UsersLinku;
import com.umc.linkyou.infra.gemini.service.GeminiLinkuService;
import com.umc.linkyou.infra.parser.LinkToImageService;
import com.umc.linkyou.repository.EmotionRepository;
import com.umc.linkyou.repository.aiArticleRepository.AiArticleRepository;
import com.umc.linkyou.repository.classification.CategoryRepository;
import com.umc.linkyou.repository.classification.SituationRepository;
import com.umc.linkyou.repository.classification.domainRepository.DomainRepository;
import com.umc.linkyou.repository.linkuRepository.LinkuRepository;
import com.umc.linkyou.repository.mapping.linkuFolderRepository.LinkuFolderRepository;
import com.umc.linkyou.repository.UserLinkuRepository.UsersLinkuRepository;
import com.umc.linkyou.repository.userRepository.UserRepository;
import com.umc.linkyou.service.keyword.KeywordService;
import com.umc.linkyou.service.folder.FolderService;
import com.umc.linkyou.support.fixture.LinkuFixture;
import com.umc.linkyou.web.dto.linku.LinkuRequestDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

import static com.umc.linkyou.support.fixture.LinkuFixture.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
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

                ArgumentCaptor<Linku> linkuCaptor = ArgumentCaptor.forClass(Linku.class);
                verify(linkuRepository).save(linkuCaptor.capture());
                assertEquals(CRAWLED_IMAGE_URL, linkuCaptor.getValue().getImgUrl());

                ArgumentCaptor<UsersLinku> usersLinkuCaptor = ArgumentCaptor.forClass(UsersLinku.class);
                verify(usersLinkuRepository).save(usersLinkuCaptor.capture());
                assertEquals(S3_IMAGE_URL, usersLinkuCaptor.getValue().getImageUrl());
            }

            @Test
            @DisplayName("이미지 미업로드 시 크롤링 이미지가 Linku에 저장되고 UsersLinku imageUrl은 null이다")
            void 이미지_미업로드_시_크롤링_이미지가_Linku에_저장되고_UsersLinku_imageUrl은_null이다() {
                setupNewLinkMocks(CRAWLED_IMAGE_URL);

                linkuCreateService.createLinku(USER_ID, defaultDto(), null);

                ArgumentCaptor<Linku> linkuCaptor = ArgumentCaptor.forClass(Linku.class);
                verify(linkuRepository).save(linkuCaptor.capture());
                assertEquals(CRAWLED_IMAGE_URL, linkuCaptor.getValue().getImgUrl());

                ArgumentCaptor<UsersLinku> usersLinkuCaptor = ArgumentCaptor.forClass(UsersLinku.class);
                verify(usersLinkuRepository).save(usersLinkuCaptor.capture());
                assertNull(usersLinkuCaptor.getValue().getImageUrl());
            }

            @Test
            @DisplayName("크롤링 이미지가 없을 시 Linku imgUrl은 null이다")
            void 크롤링_이미지가_없을_시_Linku_imgUrl은_null이다() {
                setupNewLinkMocks(null);

                linkuCreateService.createLinku(USER_ID, defaultDto(), null);

                ArgumentCaptor<Linku> linkuCaptor = ArgumentCaptor.forClass(Linku.class);
                verify(linkuRepository).save(linkuCaptor.capture());
                assertNull(linkuCaptor.getValue().getImgUrl());
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
                // given
                Linku existingLinku = LinkuFixture.linku(CRAWLED_IMAGE_URL);
                setupExistingLinkMocks(existingLinku);
                MultipartFile image = mock(MultipartFile.class);
                given(image.isEmpty()).willReturn(false);
                given(awsS3Service.uploadFile(image, "linkucreate")).willReturn(S3_IMAGE_URL);

                // when
                linkuCreateService.createLinku(USER_ID, defaultDto(), image);

                // then
                ArgumentCaptor<UsersLinku> captor = ArgumentCaptor.forClass(UsersLinku.class);
                verify(usersLinkuRepository).save(captor.capture());
                assertEquals(S3_IMAGE_URL, captor.getValue().getImageUrl());
            }

            @Test
            @DisplayName("이미지 미업로드 시 UsersLinku imageUrl은 null이고 Linku는 재저장되지 않는다")
            void 이미지_미업로드_시_UsersLinku_imageUrl은_null이고_Linku는_재저장되지_않는다() {
                // given
                Linku existingLinku = LinkuFixture.linku(CRAWLED_IMAGE_URL);
                setupExistingLinkMocks(existingLinku);

                // when
                linkuCreateService.createLinku(USER_ID, defaultDto(), null);

                // then
                ArgumentCaptor<UsersLinku> captor = ArgumentCaptor.forClass(UsersLinku.class);
                verify(usersLinkuRepository).save(captor.capture());
                assertNull(captor.getValue().getImageUrl());

                verify(linkuRepository, never()).save(any());
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
                // given
                LinkuRequestDTO.LinkuCreateDTO dto = LinkuRequestDTO.LinkuCreateDTO.builder()
                        .linku("https://www.youtube.com/watch?v=abc123")
                        .emotionId(EMOTION_ID)
                        .situationId(SITUATION_ID)
                        .build();

                // when & then
                GeneralException ex = assertThrows(GeneralException.class,
                        () -> linkuCreateService.createLinku(USER_ID, dto, null));
                assertEquals(LinkuErrorStatus._LINKU_VIDEO_NOT_ALLOWED, ex.getCode());
            }

            @Test
            @DisplayName("잘못된 URL 형식 입력 시 예외가 발생한다")
            void 잘못된_URL_형식_입력_시_예외가_발생한다() {
                // given
                LinkuRequestDTO.LinkuCreateDTO dto = LinkuRequestDTO.LinkuCreateDTO.builder()
                        .linku("not-a-valid-url")
                        .emotionId(EMOTION_ID)
                        .situationId(SITUATION_ID)
                        .build();

                // when & then
                GeneralException ex = assertThrows(GeneralException.class,
                        () -> linkuCreateService.createLinku(USER_ID, dto, null));
                assertEquals(LinkuErrorStatus._LINKU_INVALID_URL, ex.getCode());
            }
        }
    }

    // --- 공통 셋업 ---

    private void setupNewLinkMocks(String crawledImageUrl) {
        lenient().when(linkuRepository.findByLinku(TEST_URL)).thenReturn(Optional.empty());
        lenient().when(geminiLinkuService.analyzeByUrl(any(), any(), any(), any())).thenReturn(Optional.empty());
        lenient().when(categoryRepository.findById(Long.valueOf(16L))).thenReturn(Optional.of(LinkuFixture.category()));
        lenient().when(domainRepository.findByDomainTail("example.com")).thenReturn(Optional.of(LinkuFixture.domain()));
        lenient().when(linkToImageService.extractTitle(TEST_URL)).thenReturn("테스트 제목");
        lenient().when(linkToImageService.getRelatedImageFromUrl(eq(TEST_URL), any())).thenReturn(crawledImageUrl);
        lenient().when(linkuRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(aiArticleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(userRepository.findById(USER_ID)).thenReturn(Optional.of(LinkuFixture.user()));
        lenient().when(emotionRepository.findById(EMOTION_ID)).thenReturn(Optional.of(LinkuFixture.emotion()));
        lenient().when(situationRepository.findById(SITUATION_ID)).thenReturn(Optional.of(LinkuFixture.situation()));
        lenient().when(usersLinkuRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(folderService.findFolder(any(), any())).thenReturn(LinkuFixture.folder());
        lenient().when(linkuFolderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private void setupExistingLinkMocks(Linku existingLinku) {
        lenient().when(linkuRepository.findByLinku(TEST_URL)).thenReturn(Optional.of(existingLinku));
        lenient().when(userRepository.findById(USER_ID)).thenReturn(Optional.of(LinkuFixture.user()));
        lenient().when(emotionRepository.findById(EMOTION_ID)).thenReturn(Optional.of(LinkuFixture.emotion()));
        lenient().when(situationRepository.findById(SITUATION_ID)).thenReturn(Optional.of(LinkuFixture.situation()));
        lenient().when(usersLinkuRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(folderService.findFolder(any(), any())).thenReturn(LinkuFixture.folder());
        lenient().when(linkuFolderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private LinkuRequestDTO.LinkuCreateDTO defaultDto() {
        return LinkuRequestDTO.LinkuCreateDTO.builder()
                .linku(TEST_URL)
                .emotionId(EMOTION_ID)
                .situationId(SITUATION_ID)
                .build();
    }
}
