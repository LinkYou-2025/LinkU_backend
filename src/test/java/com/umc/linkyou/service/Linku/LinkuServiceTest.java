package com.umc.linkyou.service.Linku;

import com.umc.linkyou.apiPayload.code.status.ErrorStatus;
import com.umc.linkyou.apiPayload.code.status.category.CategoryErrorStatus;
import com.umc.linkyou.apiPayload.code.status.folder.FolderErrorStatus;
import com.umc.linkyou.apiPayload.code.status.linku.LinkuErrorStatus;
import com.umc.linkyou.apiPayload.exception.GeneralException;
import com.umc.linkyou.awss3.AwsS3Service;
import com.umc.linkyou.domain.Linku;
import com.umc.linkyou.domain.classification.Category;
import com.umc.linkyou.domain.classification.Domain;
import com.umc.linkyou.domain.classification.Emotion;
import com.umc.linkyou.domain.classification.Situation;
import com.umc.linkyou.domain.folder.Folder;
import com.umc.linkyou.domain.mapping.LinkuFolder;
import com.umc.linkyou.domain.mapping.UsersLinku;
import com.umc.linkyou.repository.EmotionRepository;
import com.umc.linkyou.repository.FolderRepository.FolderRepository;
import com.umc.linkyou.repository.aiArticleRepository.AiArticleRepository;
import com.umc.linkyou.repository.classification.CategoryRepository;
import com.umc.linkyou.repository.classification.SituationRepository;
import com.umc.linkyou.repository.classification.domainRepository.DomainRepository;
import com.umc.linkyou.repository.curationRepository.CurationLinkuRepository;
import com.umc.linkyou.repository.linkuRepository.LinkuRepository;
import com.umc.linkyou.repository.mapping.linkuFolderRepository.LinkuFolderRepository;
import com.umc.linkyou.repository.UserLinkuRepository.UsersLinkuRepository;
import com.umc.linkyou.repository.usersFolderRepository.UsersFolderRepository;
import com.umc.linkyou.support.fixture.LinkuFixture;
import com.umc.linkyou.web.dto.linku.LinkuRequestDTO;
import com.umc.linkyou.web.dto.linku.LinkuResponseDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

import static com.umc.linkyou.support.fixture.LinkuFixture.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("LinkuService 테스트")
class LinkuServiceTest {

    @InjectMocks
    private LinkuService linkuService;

    @Mock private LinkuRepository linkuRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private EmotionRepository emotionRepository;
    @Mock private SituationRepository situationRepository;
    @Mock private DomainRepository domainRepository;
    @Mock private LinkuFolderRepository linkuFolderRepository;
    @Mock private UsersLinkuRepository usersLinkuRepository;
    @Mock private FolderRepository folderRepository;
    @Mock private UsersFolderRepository usersFolderRepository;
    @Mock private AiArticleRepository aiArticleRepository;
    @Mock private CurationLinkuRepository curationLinkuRepository;
    @Mock private LinkuViewService linkuViewService;
    @Mock private AwsS3Service awsS3Service;

    private static final Long NEW_EMOTION_ID = 3L;
    private static final Long NEW_SITUATION_ID = 3L;

    @Nested
    @DisplayName("updateLinku() - 감정/상황 수정 및 AI 플래그")
    class UpdateLinkuEmotionSituation {

        @Nested
        @DisplayName("성공")
        class Success {

            @Test
            @DisplayName("emotionId 제공 시 emotion이 변경되고 emotionAi는 false가 된다")
            void emotionId_제공_시_emotion_변경되고_emotionAi는_false이다() {
                // given
                UsersLinku usersLinku = createDefaultUsersLinku(); // emotionAi=true
                Emotion newEmotion = Emotion.builder().emotionId(NEW_EMOTION_ID).name("불안").build();
                given(usersLinkuRepository.findByUser_IdAndLinku_LinkuId(USER_ID, 100L))
                        .willReturn(List.of(usersLinku));
                given(emotionRepository.findById(NEW_EMOTION_ID)).willReturn(Optional.of(newEmotion));
                given(linkuFolderRepository
                        .findFirstByUsersLinku_UserLinkuIdOrderByLinkuFolderIdDesc(any()))
                        .willReturn(Optional.empty());

                // when
                linkuService.updateLinku(USER_ID, 100L, LinkuRequestDTO.LinkuUpdateDTO.builder()
                        .emotionId(NEW_EMOTION_ID)
                        .build());

                // then: emotion 변경, emotionAi = false
                assertEquals(newEmotion, usersLinku.getEmotion());
                assertFalse(usersLinku.getEmotionAi());
                verify(usersLinkuRepository).save(usersLinku);

                // then: 공용 Linku의 emotion(AI 캐시용 필드)는 절대 안 건드림
                assertNull(usersLinku.getLinku().getEmotion());
                verify(linkuRepository, never()).save(any());
            }

            @Test
            @DisplayName("emotionId 미제공 시 emotion은 변경되지 않고 emotionAi는 기존 true를 유지한다")
            void emotionId_미제공_시_emotion_변경되지_않고_emotionAi는_true를_유지한다() {
                // given
                UsersLinku usersLinku = createDefaultUsersLinku(); // emotionAi=true, emotion=EMOTION_ID
                given(usersLinkuRepository.findByUser_IdAndLinku_LinkuId(USER_ID, 100L))
                        .willReturn(List.of(usersLinku));
                given(linkuFolderRepository
                        .findFirstByUsersLinku_UserLinkuIdOrderByLinkuFolderIdDesc(any()))
                        .willReturn(Optional.empty());

                // when: emotionId 없는 DTO
                linkuService.updateLinku(USER_ID, 100L, LinkuRequestDTO.LinkuUpdateDTO.builder().build());

                // then: emotion 변경 없음, emotionAi 여전히 true
                assertEquals(EMOTION_ID, usersLinku.getEmotion().getEmotionId());
                assertTrue(usersLinku.getEmotionAi());
            }

            @Test
            @DisplayName("situationId 제공 시 situation이 변경되고 situationAi는 false가 된다")
            void situationId_제공_시_situation_변경되고_situationAi는_false이다() {
                // given
                UsersLinku usersLinku = createDefaultUsersLinku(); // situationAi=true
                Situation newSituation = Situation.builder().id(NEW_SITUATION_ID).name("휴식").build();
                given(usersLinkuRepository.findByUser_IdAndLinku_LinkuId(USER_ID, 100L))
                        .willReturn(List.of(usersLinku));
                given(situationRepository.findById(NEW_SITUATION_ID)).willReturn(Optional.of(newSituation));
                given(linkuFolderRepository
                        .findFirstByUsersLinku_UserLinkuIdOrderByLinkuFolderIdDesc(any()))
                        .willReturn(Optional.empty());

                // when
                linkuService.updateLinku(USER_ID, 100L, LinkuRequestDTO.LinkuUpdateDTO.builder()
                        .situationId(NEW_SITUATION_ID)
                        .build());

                // then: situation 변경, situationAi = false
                assertEquals(newSituation, usersLinku.getSituation());
                assertFalse(usersLinku.getSituationAi());
                verify(usersLinkuRepository).save(usersLinku);

                // then: 공용 Linku의 situation(AI 캐시용 필드)는 절대 안 건드림
                assertNull(usersLinku.getLinku().getSituation());
                verify(linkuRepository, never()).save(any());
            }

            @Test
            @DisplayName("situationId 미제공 시 situation은 변경되지 않고 situationAi는 기존 true를 유지한다")
            void situationId_미제공_시_situation_변경되지_않고_situationAi는_true를_유지한다() {
                // given
                UsersLinku usersLinku = createDefaultUsersLinku(); // situationAi=true
                given(usersLinkuRepository.findByUser_IdAndLinku_LinkuId(USER_ID, 100L))
                        .willReturn(List.of(usersLinku));
                given(linkuFolderRepository
                        .findFirstByUsersLinku_UserLinkuIdOrderByLinkuFolderIdDesc(any()))
                        .willReturn(Optional.empty());

                // when
                linkuService.updateLinku(USER_ID, 100L, LinkuRequestDTO.LinkuUpdateDTO.builder().build());

                // then: situation 변경 없음, situationAi 여전히 true
                assertEquals(SITUATION_ID, usersLinku.getSituation().getId());
                assertTrue(usersLinku.getSituationAi());
            }

            @Test
            @DisplayName("emotionId + situationId 모두 제공 시 둘 다 변경되고 AI 플래그가 모두 false가 된다")
            void emotionId_situationId_모두_제공_시_둘_다_변경되고_AI_플래그_모두_false이다() {
                // given
                UsersLinku usersLinku = createDefaultUsersLinku(); // both AI=true
                Emotion newEmotion = Emotion.builder().emotionId(NEW_EMOTION_ID).name("불안").build();
                Situation newSituation = Situation.builder().id(NEW_SITUATION_ID).name("휴식").build();
                given(usersLinkuRepository.findByUser_IdAndLinku_LinkuId(USER_ID, 100L))
                        .willReturn(List.of(usersLinku));
                given(emotionRepository.findById(NEW_EMOTION_ID)).willReturn(Optional.of(newEmotion));
                given(situationRepository.findById(NEW_SITUATION_ID)).willReturn(Optional.of(newSituation));
                given(linkuFolderRepository
                        .findFirstByUsersLinku_UserLinkuIdOrderByLinkuFolderIdDesc(any()))
                        .willReturn(Optional.empty());

                // when
                linkuService.updateLinku(USER_ID, 100L, LinkuRequestDTO.LinkuUpdateDTO.builder()
                        .emotionId(NEW_EMOTION_ID)
                        .situationId(NEW_SITUATION_ID)
                        .build());

                // then: 둘 다 사용자값으로 변경, AI 플래그 모두 false
                assertEquals(newEmotion, usersLinku.getEmotion());
                assertEquals(newSituation, usersLinku.getSituation());
                assertFalse(usersLinku.getEmotionAi());
                assertFalse(usersLinku.getSituationAi());
            }

            @Test
            @DisplayName("AI 플래그가 이미 false인 상태에서 emotionId 재제공 시 false를 유지한다")
            void AI_플래그가_false인_상태에서_emotionId_재제공_시_false를_유지한다() {
                // given: 이미 사용자가 한 번 수정한 상태 (emotionAi=false)
                UsersLinku usersLinku = UsersLinku.builder()
                        .userLinkuId(10L)
                        .linku(LinkuFixture.linku(null))
                        .user(LinkuFixture.user())
                        .emotion(emotion())
                        .situation(situation())
                        .emotionAi(false) // 이미 사용자 수정됨
                        .situationAi(true)
                        .build();
                Emotion newEmotion = Emotion.builder().emotionId(NEW_EMOTION_ID).name("평온").build();
                given(usersLinkuRepository.findByUser_IdAndLinku_LinkuId(USER_ID, 100L))
                        .willReturn(List.of(usersLinku));
                given(emotionRepository.findById(NEW_EMOTION_ID)).willReturn(Optional.of(newEmotion));
                given(linkuFolderRepository
                        .findFirstByUsersLinku_UserLinkuIdOrderByLinkuFolderIdDesc(any()))
                        .willReturn(Optional.empty());

                // when
                linkuService.updateLinku(USER_ID, 100L, LinkuRequestDTO.LinkuUpdateDTO.builder()
                        .emotionId(NEW_EMOTION_ID)
                        .build());

                // then: 여전히 false (사용자 수정 상태 유지)
                assertFalse(usersLinku.getEmotionAi());
            }
        }

        @Nested
        @DisplayName("실패")
        class Failure {

            @Test
            @DisplayName("해당 사용자의 링크가 없으면 예외가 발생한다")
            void 해당_사용자의_링크가_없으면_예외가_발생한다() {
                // given
                given(usersLinkuRepository.findByUser_IdAndLinku_LinkuId(USER_ID, 100L))
                        .willReturn(List.of());

                // when & then
                assertThrows(GeneralException.class,
                        () -> linkuService.updateLinku(USER_ID, 100L,
                                LinkuRequestDTO.LinkuUpdateDTO.builder().build()));
            }

            @Test
            @DisplayName("존재하지 않는 emotionId면 예외가 발생한다")
            void 존재하지_않는_emotionId면_예외가_발생한다() {
                // given
                UsersLinku usersLinku = createDefaultUsersLinku();
                given(usersLinkuRepository.findByUser_IdAndLinku_LinkuId(USER_ID, 100L))
                        .willReturn(List.of(usersLinku));
                given(emotionRepository.findById(999L)).willReturn(Optional.empty());

                // when & then
                GeneralException ex = assertThrows(GeneralException.class,
                        () -> linkuService.updateLinku(USER_ID, 100L,
                                LinkuRequestDTO.LinkuUpdateDTO.builder().emotionId(999L).build()));
                assertEquals(ErrorStatus._EMOTION_NOT_FOUND, ex.getCode());
                verify(usersLinkuRepository, never()).save(any());
            }

            @Test
            @DisplayName("존재하지 않는 situationId면 예외가 발생한다")
            void 존재하지_않는_situationId면_예외가_발생한다() {
                // given
                UsersLinku usersLinku = createDefaultUsersLinku();
                given(usersLinkuRepository.findByUser_IdAndLinku_LinkuId(USER_ID, 100L))
                        .willReturn(List.of(usersLinku));
                given(situationRepository.findById(999L)).willReturn(Optional.empty());

                // when & then
                GeneralException ex = assertThrows(GeneralException.class,
                        () -> linkuService.updateLinku(USER_ID, 100L,
                                LinkuRequestDTO.LinkuUpdateDTO.builder().situationId(999L).build()));
                assertEquals(ErrorStatus._SITUATION_NOT_FOUND, ex.getCode());
                verify(usersLinkuRepository, never()).save(any());
            }
        }
    }

    @Nested
    @DisplayName("updateLinku() - 메모 수정")
    class UpdateLinkuMemo {

        @Nested
        @DisplayName("성공")
        class Success {

            @Test
            @DisplayName("memo 제공 시 UsersLinku.memo가 변경된다")
            void memo_제공_시_UsersLinku_memo가_변경된다() {
                // given
                UsersLinku usersLinku = createDefaultUsersLinku();
                given(usersLinkuRepository.findByUser_IdAndLinku_LinkuId(USER_ID, 100L))
                        .willReturn(List.of(usersLinku));
                given(linkuFolderRepository
                        .findFirstByUsersLinku_UserLinkuIdOrderByLinkuFolderIdDesc(any()))
                        .willReturn(Optional.empty());

                // when
                linkuService.updateLinku(USER_ID, 100L,
                        LinkuRequestDTO.LinkuUpdateDTO.builder().memo("새로운 메모").build());

                // then
                assertEquals("새로운 메모", usersLinku.getMemo());
                verify(usersLinkuRepository).save(usersLinku);
                verify(linkuRepository, never()).save(any());
            }
        }
    }

    @Nested
    @DisplayName("updateLinku() - 도메인 수정 (공용 Linku 변경, 아직 개인화 안 됨)")
    class UpdateLinkuDomain {

        @Nested
        @DisplayName("성공")
        class Success {

            @Test
            @DisplayName("domainId 제공 시 공용 Linku.domain이 변경되고 linkuRepository에 저장된다")
            void domainId_제공_시_공용_Linku_domain이_변경되고_저장된다() {
                // given
                UsersLinku usersLinku = createDefaultUsersLinku();
                Linku linku = usersLinku.getLinku();
                Domain newDomain = Domain.builder().domainId(2L).name("네이버").build();

                given(usersLinkuRepository.findByUser_IdAndLinku_LinkuId(USER_ID, 100L))
                        .willReturn(List.of(usersLinku));
                given(domainRepository.findById(2L)).willReturn(Optional.of(newDomain));
                given(linkuFolderRepository
                        .findFirstByUsersLinku_UserLinkuIdOrderByLinkuFolderIdDesc(any()))
                        .willReturn(Optional.empty());

                // when
                LinkuResponseDTO.LinkuResultDTO result = linkuService.updateLinku(USER_ID, 100L,
                        LinkuRequestDTO.LinkuUpdateDTO.builder().domainId(2L).build());

                // then: 공용 Linku가 실제로 변경됨 (아직 개인화되지 않은 필드라는 걸 명시적으로 확인)
                assertEquals(newDomain, linku.getDomain());
                assertEquals("네이버", result.getDomain());
                verify(linkuRepository).save(linku);
            }
        }

        @Nested
        @DisplayName("실패")
        class Failure {

            @Test
            @DisplayName("존재하지 않는 domainId면 예외가 발생한다")
            void 존재하지_않는_domainId면_예외가_발생한다() {
                // given
                UsersLinku usersLinku = createDefaultUsersLinku();
                given(usersLinkuRepository.findByUser_IdAndLinku_LinkuId(USER_ID, 100L))
                        .willReturn(List.of(usersLinku));
                given(domainRepository.findById(999L)).willReturn(Optional.empty());

                // when & then
                GeneralException ex = assertThrows(GeneralException.class,
                        () -> linkuService.updateLinku(USER_ID, 100L,
                                LinkuRequestDTO.LinkuUpdateDTO.builder().domainId(999L).build()));
                assertEquals(ErrorStatus._DOMAIN_NOT_FOUND, ex.getCode());
                verify(linkuRepository, never()).save(any());
            }
        }
    }

    @Nested
    @DisplayName("updateLinku() - title 개인화 (공용 Linku 미변경)")
    class UpdateLinkuTitle {

        @Nested
        @DisplayName("성공")
        class Success {

            @Test
            @DisplayName("title 변경 시 UsersLinku.title만 바뀌고 공용 Linku.title은 그대로다")
            void title_변경_시_UsersLinku_title만_바뀌고_공용_Linku는_그대로다() {
                // given
                UsersLinku usersLinku = createDefaultUsersLinku();
                Linku linku = usersLinku.getLinku();
                String originalTitle = linku.getTitle();
                String newTitle = "내가 정한 제목";

                given(usersLinkuRepository.findByUser_IdAndLinku_LinkuId(USER_ID, 100L))
                        .willReturn(List.of(usersLinku));
                given(linkuFolderRepository
                        .findFirstByUsersLinku_UserLinkuIdOrderByLinkuFolderIdDesc(any()))
                        .willReturn(Optional.empty());

                // when
                LinkuResponseDTO.LinkuResultDTO result = linkuService.updateLinku(USER_ID, 100L,
                        LinkuRequestDTO.LinkuUpdateDTO.builder().title(newTitle).build());

                // then: UsersLinku.title만 변경, 공용 Linku는 그대로
                assertEquals(newTitle, usersLinku.getTitle());
                assertEquals(originalTitle, linku.getTitle());
                assertEquals(newTitle, result.getTitle());
                verify(usersLinkuRepository).save(usersLinku);
                verify(linkuRepository, never()).save(any());
            }
        }
    }

    @Nested
    @DisplayName("updateLinku() - 대표 이미지 수정 (개인화, S3 교체)")
    class UpdateLinkuImage {

        @Nested
        @DisplayName("성공")
        class Success {

            @Test
            @DisplayName("기존 이미지가 있으면 S3에서 삭제한 뒤 새 이미지를 업로드하고 imageUrl을 교체한다")
            void 기존_이미지가_있으면_삭제_후_새_이미지로_교체한다() {
                // given
                UsersLinku usersLinku = createDefaultUsersLinku();
                usersLinku.updateImageUrl("https://cdn.example.com/old.jpg");

                MultipartFile image = mock(MultipartFile.class);
                given(image.isEmpty()).willReturn(false);
                given(awsS3Service.replaceFile("https://cdn.example.com/old.jpg", image, "linkucreate"))
                        .willReturn("https://cdn.example.com/new.jpg");

                given(usersLinkuRepository.findByUser_IdAndLinku_LinkuId(USER_ID, 100L))
                        .willReturn(List.of(usersLinku));
                given(linkuFolderRepository
                        .findFirstByUsersLinku_UserLinkuIdOrderByLinkuFolderIdDesc(any()))
                        .willReturn(Optional.empty());

                // when
                linkuService.updateLinku(USER_ID, 100L,
                        LinkuRequestDTO.LinkuUpdateDTO.builder().image(image).build());

                // then: 기존 imageUrl을 넘겨서 replaceFile 호출(삭제+업로드는 AwsS3Service 내부 책임), 새 URL로 교체
                verify(awsS3Service).replaceFile("https://cdn.example.com/old.jpg", image, "linkucreate");
                assertEquals("https://cdn.example.com/new.jpg", usersLinku.getImageUrl());
                verify(usersLinkuRepository).save(usersLinku);
            }

            @Test
            @DisplayName("기존 이미지가 없으면 oldFileUrl=null로 replaceFile을 호출한다")
            void 기존_이미지가_없으면_null을_넘겨_replaceFile을_호출한다() {
                // given
                UsersLinku usersLinku = createDefaultUsersLinku(); // imageUrl == null

                MultipartFile image = mock(MultipartFile.class);
                given(image.isEmpty()).willReturn(false);
                given(awsS3Service.replaceFile(null, image, "linkucreate"))
                        .willReturn("https://cdn.example.com/new.jpg");

                given(usersLinkuRepository.findByUser_IdAndLinku_LinkuId(USER_ID, 100L))
                        .willReturn(List.of(usersLinku));
                given(linkuFolderRepository
                        .findFirstByUsersLinku_UserLinkuIdOrderByLinkuFolderIdDesc(any()))
                        .willReturn(Optional.empty());

                // when
                linkuService.updateLinku(USER_ID, 100L,
                        LinkuRequestDTO.LinkuUpdateDTO.builder().image(image).build());

                // then
                assertEquals("https://cdn.example.com/new.jpg", usersLinku.getImageUrl());
            }

            @Test
            @DisplayName("이미지를 첨부하지 않으면 imageUrl은 그대로고 S3도 호출되지 않는다")
            void 이미지_미첨부_시_imageUrl_변경없고_S3_호출없다() {
                // given
                UsersLinku usersLinku = createDefaultUsersLinku();
                usersLinku.updateImageUrl("https://cdn.example.com/old.jpg");
                given(usersLinkuRepository.findByUser_IdAndLinku_LinkuId(USER_ID, 100L))
                        .willReturn(List.of(usersLinku));
                given(linkuFolderRepository
                        .findFirstByUsersLinku_UserLinkuIdOrderByLinkuFolderIdDesc(any()))
                        .willReturn(Optional.empty());

                // when: image 없는 DTO (memo만 변경)
                linkuService.updateLinku(USER_ID, 100L,
                        LinkuRequestDTO.LinkuUpdateDTO.builder().memo("메모만 변경").build());

                // then
                assertEquals("https://cdn.example.com/old.jpg", usersLinku.getImageUrl());
                verify(awsS3Service, never()).replaceFile(any(), any(), any());
            }
        }
    }

    @Nested
    @DisplayName("updateLinku() - 카테고리(중분류) 변경")
    class UpdateLinkuCategory {

        private static final Long NEW_CATEGORY_ID = 5L;

        private Category newCategory() {
            return Category.builder()
                    .categoryId(NEW_CATEGORY_ID)
                    .categoryName("여행")
                    .build();
        }

        private Folder newRootFolder(Category category) {
            return Folder.builder()
                    .folderId(40L)
                    .folderName("여행")
                    .category(category)
                    .build();
        }

        @Nested
        @DisplayName("성공")
        class Success {

            @Test
            @DisplayName("categoryId 제공 시 내 소유 중분류 폴더로 LinkuFolder 매핑이 이동하고, 공용 Linku는 변경되지 않는다")
            void categoryId_제공_시_내_중분류_폴더로_매핑이_이동한다() {
                // given
                UsersLinku usersLinku = createDefaultUsersLinku();
                Linku linku = usersLinku.getLinku();
                Category newCategory = newCategory();
                Folder oldFolder = LinkuFixture.folder();
                Folder targetFolder = newRootFolder(newCategory);
                LinkuFolder linkuFolder = LinkuFolder.builder()
                        .linkuFolderId(2000L)
                        .folder(oldFolder)
                        .usersLinku(usersLinku)
                        .build();

                given(usersLinkuRepository.findByUser_IdAndLinku_LinkuId(USER_ID, 100L))
                        .willReturn(List.of(usersLinku));
                given(linkuFolderRepository
                        .findFirstByUsersLinku_UserLinkuIdOrderByLinkuFolderIdDesc(usersLinku.getUserLinkuId()))
                        .willReturn(Optional.of(linkuFolder));
                given(categoryRepository.findById(NEW_CATEGORY_ID)).willReturn(Optional.of(newCategory));
                given(usersFolderRepository.findFolderByUserIdAndCategory(USER_ID, newCategory))
                        .willReturn(Optional.of(targetFolder));

                // when
                LinkuResponseDTO.LinkuResultDTO result = linkuService.updateLinku(USER_ID, 100L,
                        LinkuRequestDTO.LinkuUpdateDTO.builder().categoryId(NEW_CATEGORY_ID).build());

                // then: LinkuFolder의 folder만 교체되고 저장됨. 공유 Linku 엔티티는 건드리지 않음
                assertEquals(targetFolder, linkuFolder.getFolder());
                verify(linkuFolderRepository).save(linkuFolder);
                verify(linkuRepository, never()).save(any());
                assertNotEquals(NEW_CATEGORY_ID, linku.getCategory().getCategoryId());

                // then: 응답은 새로 이동한 폴더 기준 정보를 반영
                assertEquals(NEW_CATEGORY_ID, result.getCategoryId());
                assertEquals("여행", result.getFolderName());
            }

            @Test
            @DisplayName("categoryId 미제공 시 폴더 매핑은 변경되지 않는다")
            void categoryId_미제공_시_폴더_매핑_변경없다() {
                // given
                UsersLinku usersLinku = createDefaultUsersLinku();
                Folder oldFolder = LinkuFixture.folder();
                LinkuFolder linkuFolder = LinkuFolder.builder()
                        .folder(oldFolder)
                        .usersLinku(usersLinku)
                        .build();

                given(usersLinkuRepository.findByUser_IdAndLinku_LinkuId(USER_ID, 100L))
                        .willReturn(List.of(usersLinku));
                given(linkuFolderRepository
                        .findFirstByUsersLinku_UserLinkuIdOrderByLinkuFolderIdDesc(usersLinku.getUserLinkuId()))
                        .willReturn(Optional.of(linkuFolder));

                // when
                linkuService.updateLinku(USER_ID, 100L,
                        LinkuRequestDTO.LinkuUpdateDTO.builder().memo("메모만 변경").build());

                // then
                assertEquals(oldFolder, linkuFolder.getFolder());
                verify(linkuFolderRepository, never()).save(any());
                verify(categoryRepository, never()).findById(any());
            }
        }

        @Nested
        @DisplayName("실패")
        class Failure {

            @Test
            @DisplayName("존재하지 않는 categoryId면 예외가 발생한다")
            void 존재하지_않는_categoryId면_예외가_발생한다() {
                // given
                UsersLinku usersLinku = createDefaultUsersLinku();
                LinkuFolder linkuFolder = LinkuFolder.builder()
                        .folder(LinkuFixture.folder())
                        .usersLinku(usersLinku)
                        .build();

                given(usersLinkuRepository.findByUser_IdAndLinku_LinkuId(USER_ID, 100L))
                        .willReturn(List.of(usersLinku));
                given(linkuFolderRepository
                        .findFirstByUsersLinku_UserLinkuIdOrderByLinkuFolderIdDesc(usersLinku.getUserLinkuId()))
                        .willReturn(Optional.of(linkuFolder));
                given(categoryRepository.findById(999L)).willReturn(Optional.empty());

                // when & then
                GeneralException ex = assertThrows(GeneralException.class,
                        () -> linkuService.updateLinku(USER_ID, 100L,
                                LinkuRequestDTO.LinkuUpdateDTO.builder().categoryId(999L).build()));
                assertEquals(CategoryErrorStatus._CATEGORY_NOT_FOUND, ex.getCode());
                verify(linkuFolderRepository, never()).save(any());
            }

            @Test
            @DisplayName("해당 카테고리의 내 소유 중분류 폴더가 없으면 예외가 발생한다")
            void 카테고리에_해당하는_내_폴더가_없으면_예외가_발생한다() {
                // given
                UsersLinku usersLinku = createDefaultUsersLinku();
                Category newCategory = newCategory();
                LinkuFolder linkuFolder = LinkuFolder.builder()
                        .folder(LinkuFixture.folder())
                        .usersLinku(usersLinku)
                        .build();

                given(usersLinkuRepository.findByUser_IdAndLinku_LinkuId(USER_ID, 100L))
                        .willReturn(List.of(usersLinku));
                given(linkuFolderRepository
                        .findFirstByUsersLinku_UserLinkuIdOrderByLinkuFolderIdDesc(usersLinku.getUserLinkuId()))
                        .willReturn(Optional.of(linkuFolder));
                given(categoryRepository.findById(NEW_CATEGORY_ID)).willReturn(Optional.of(newCategory));
                given(usersFolderRepository.findFolderByUserIdAndCategory(USER_ID, newCategory))
                        .willReturn(Optional.empty());

                // when & then
                GeneralException ex = assertThrows(GeneralException.class,
                        () -> linkuService.updateLinku(USER_ID, 100L,
                                LinkuRequestDTO.LinkuUpdateDTO.builder().categoryId(NEW_CATEGORY_ID).build()));
                assertEquals(FolderErrorStatus._FOLDER_NOT_FOUND, ex.getCode());
                verify(linkuFolderRepository, never()).save(any());
            }

            @Test
            @DisplayName("이 사용자의 링크-폴더 매핑이 없으면 예외가 발생한다")
            void 링크_폴더_매핑이_없으면_예외가_발생한다() {
                // given
                UsersLinku usersLinku = createDefaultUsersLinku();
                Category newCategory = newCategory();
                Folder targetFolder = newRootFolder(newCategory);

                given(usersLinkuRepository.findByUser_IdAndLinku_LinkuId(USER_ID, 100L))
                        .willReturn(List.of(usersLinku));
                given(linkuFolderRepository
                        .findFirstByUsersLinku_UserLinkuIdOrderByLinkuFolderIdDesc(usersLinku.getUserLinkuId()))
                        .willReturn(Optional.empty());
                given(categoryRepository.findById(NEW_CATEGORY_ID)).willReturn(Optional.of(newCategory));
                given(usersFolderRepository.findFolderByUserIdAndCategory(USER_ID, newCategory))
                        .willReturn(Optional.of(targetFolder));

                // when & then
                GeneralException ex = assertThrows(GeneralException.class,
                        () -> linkuService.updateLinku(USER_ID, 100L,
                                LinkuRequestDTO.LinkuUpdateDTO.builder().categoryId(NEW_CATEGORY_ID).build()));
                assertEquals(LinkuErrorStatus._USER_LINKU_NOT_FOUND, ex.getCode());
            }
        }
    }

    @Nested
    @DisplayName("updateLinkuFolder() - 링크 폴더 이동")
    class UpdateLinkuFolder {

        private static final Long NEW_FOLDER_ID = 20L;

        private Folder newFolder() {
            return Folder.builder()
                    .folderId(NEW_FOLDER_ID)
                    .folderName("영어 공부")
                    .category(category())
                    .build();
        }

        @Nested
        @DisplayName("성공")
        class Success {

            @Test
            @DisplayName("폴더 이동 시 LinkuFolder의 폴더만 교체되고 linku는 변경되지 않는다")
            void 폴더_이동_시_LinkuFolder의_폴더만_교체되고_linku는_변경되지_않는다() {
                // given
                UsersLinku usersLinku = createDefaultUsersLinku();
                Linku linku = usersLinku.getLinku();
                Folder oldFolder = LinkuFixture.folder();
                Folder newFolder = newFolder();
                LinkuFolder linkuFolder = LinkuFolder.builder()
                        .linkuFolderId(1000L)
                        .folder(oldFolder)
                        .usersLinku(usersLinku)
                        .build();

                given(usersLinkuRepository.findByUser_IdAndLinku_LinkuId(USER_ID, 100L))
                        .willReturn(List.of(usersLinku));
                given(folderRepository.findById(NEW_FOLDER_ID)).willReturn(Optional.of(newFolder));
                given(usersFolderRepository.existsFolderOwnerOrWriter(USER_ID, NEW_FOLDER_ID)).willReturn(true);
                given(linkuFolderRepository
                        .findFirstByUsersLinku_UserLinkuIdOrderByLinkuFolderIdDesc(usersLinku.getUserLinkuId()))
                        .willReturn(Optional.of(linkuFolder));

                // when
                LinkuResponseDTO.LinkuFolderChangeResultDTO result = linkuService.updateLinkuFolder(
                        USER_ID, 100L,
                        LinkuRequestDTO.LinkuFolderUpdateDTO.builder().folderId(NEW_FOLDER_ID).build());

                // then: LinkuFolder의 folder만 교체, Linku 엔티티는 저장되지 않음(공유 엔티티라 건드리면 안 됨)
                assertEquals(newFolder, linkuFolder.getFolder());
                assertEquals(NEW_FOLDER_ID, result.getFolderId());
                assertEquals("영어 공부", result.getFolderName());
                assertEquals(linku.getLinkuId(), result.getLinkuId());
                verify(linkuFolderRepository).save(linkuFolder);
                verify(linkuRepository, never()).save(any());
            }

            @Test
            @DisplayName("이동할 폴더가 소분류(하위) 폴더여도 정상적으로 이동된다")
            void 이동할_폴더가_소분류_폴더여도_정상적으로_이동된다() {
                // given: 중분류(루트) 폴더 아래에 소분류(하위) 폴더를 둔다
                UsersLinku usersLinku = createDefaultUsersLinku();
                Category category = category();
                Folder rootFolder = Folder.builder()
                        .folderId(30L)
                        .folderName("영어")
                        .category(category)
                        .build();
                Folder subFolder = Folder.builder()
                        .folderId(31L)
                        .folderName("영어 회화")
                        .category(category)
                        .parentFolder(rootFolder)
                        .build();
                LinkuFolder linkuFolder = LinkuFolder.builder()
                        .linkuFolderId(1001L)
                        .folder(LinkuFixture.folder())
                        .usersLinku(usersLinku)
                        .build();

                given(usersLinkuRepository.findByUser_IdAndLinku_LinkuId(USER_ID, 100L))
                        .willReturn(List.of(usersLinku));
                given(folderRepository.findById(31L)).willReturn(Optional.of(subFolder));
                given(usersFolderRepository.existsFolderOwnerOrWriter(USER_ID, 31L)).willReturn(true);
                given(linkuFolderRepository
                        .findFirstByUsersLinku_UserLinkuIdOrderByLinkuFolderIdDesc(usersLinku.getUserLinkuId()))
                        .willReturn(Optional.of(linkuFolder));

                // when
                LinkuResponseDTO.LinkuFolderChangeResultDTO result = linkuService.updateLinkuFolder(
                        USER_ID, 100L,
                        LinkuRequestDTO.LinkuFolderUpdateDTO.builder().folderId(31L).build());

                // then: 중분류/소분류 구분 없이 동일하게 폴더가 교체된다
                assertEquals(subFolder, linkuFolder.getFolder());
                assertEquals(31L, result.getFolderId());
                assertEquals("영어 회화", result.getFolderName());
                verify(linkuFolderRepository).save(linkuFolder);
            }
        }

        @Nested
        @DisplayName("실패")
        class Failure {

            @Test
            @DisplayName("해당 사용자의 링크가 없으면 예외가 발생한다")
            void 해당_사용자의_링크가_없으면_예외가_발생한다() {
                // given
                given(usersLinkuRepository.findByUser_IdAndLinku_LinkuId(USER_ID, 100L))
                        .willReturn(List.of());

                // when & then
                GeneralException ex = assertThrows(GeneralException.class,
                        () -> linkuService.updateLinkuFolder(USER_ID, 100L,
                                LinkuRequestDTO.LinkuFolderUpdateDTO.builder().folderId(NEW_FOLDER_ID).build()));
                assertEquals(LinkuErrorStatus._USER_LINKU_NOT_FOUND, ex.getCode());
            }

            @Test
            @DisplayName("이동할 폴더가 존재하지 않으면 예외가 발생한다")
            void 이동할_폴더가_존재하지_않으면_예외가_발생한다() {
                // given
                UsersLinku usersLinku = createDefaultUsersLinku();
                given(usersLinkuRepository.findByUser_IdAndLinku_LinkuId(USER_ID, 100L))
                        .willReturn(List.of(usersLinku));
                given(folderRepository.findById(NEW_FOLDER_ID)).willReturn(Optional.empty());

                // when & then
                GeneralException ex = assertThrows(GeneralException.class,
                        () -> linkuService.updateLinkuFolder(USER_ID, 100L,
                                LinkuRequestDTO.LinkuFolderUpdateDTO.builder().folderId(NEW_FOLDER_ID).build()));
                assertEquals(FolderErrorStatus._FOLDER_NOT_FOUND, ex.getCode());
            }

            @Test
            @DisplayName("이동할 폴더에 대한 권한이 없으면 예외가 발생한다")
            void 이동할_폴더에_대한_권한이_없으면_예외가_발생한다() {
                // given
                UsersLinku usersLinku = createDefaultUsersLinku();
                Folder newFolder = newFolder();
                given(usersLinkuRepository.findByUser_IdAndLinku_LinkuId(USER_ID, 100L))
                        .willReturn(List.of(usersLinku));
                given(folderRepository.findById(NEW_FOLDER_ID)).willReturn(Optional.of(newFolder));
                given(usersFolderRepository.existsFolderOwnerOrWriter(USER_ID, NEW_FOLDER_ID)).willReturn(false);

                // when & then
                GeneralException ex = assertThrows(GeneralException.class,
                        () -> linkuService.updateLinkuFolder(USER_ID, 100L,
                                LinkuRequestDTO.LinkuFolderUpdateDTO.builder().folderId(NEW_FOLDER_ID).build()));
                assertEquals(FolderErrorStatus._FOLDER_ACCESS_FORBIDDEN, ex.getCode());
                verify(linkuFolderRepository, never()).save(any());
            }

            @Test
            @DisplayName("이 사용자의 링크-폴더 매핑이 없으면 예외가 발생한다")
            void 사용자의_링크_폴더_매핑이_없으면_예외가_발생한다() {
                // given
                UsersLinku usersLinku = createDefaultUsersLinku();
                Folder newFolder = newFolder();
                given(usersLinkuRepository.findByUser_IdAndLinku_LinkuId(USER_ID, 100L))
                        .willReturn(List.of(usersLinku));
                given(folderRepository.findById(NEW_FOLDER_ID)).willReturn(Optional.of(newFolder));
                given(usersFolderRepository.existsFolderOwnerOrWriter(USER_ID, NEW_FOLDER_ID)).willReturn(true);
                given(linkuFolderRepository
                        .findFirstByUsersLinku_UserLinkuIdOrderByLinkuFolderIdDesc(usersLinku.getUserLinkuId()))
                        .willReturn(Optional.empty());

                // when & then
                GeneralException ex = assertThrows(GeneralException.class,
                        () -> linkuService.updateLinkuFolder(USER_ID, 100L,
                                LinkuRequestDTO.LinkuFolderUpdateDTO.builder().folderId(NEW_FOLDER_ID).build()));
                assertEquals(LinkuErrorStatus._USER_LINKU_NOT_FOUND, ex.getCode());
            }
        }
    }

    private UsersLinku createDefaultUsersLinku() {
        Linku linku = LinkuFixture.linku(null);
        return UsersLinku.builder()
                .userLinkuId(10L)
                .linku(linku)
                .user(LinkuFixture.user())
                .emotion(emotion())      // emotionId=EMOTION_ID=1L
                .situation(situation())  // situationId=SITUATION_ID=1L
                .emotionAi(true)
                .situationAi(true)
                .build();
    }
}
