package com.umc.linkyou.service.Linku;

import com.umc.linkyou.apiPayload.code.status.folder.FolderErrorStatus;
import com.umc.linkyou.apiPayload.code.status.linku.LinkuErrorStatus;
import com.umc.linkyou.apiPayload.exception.GeneralException;
import com.umc.linkyou.domain.Linku;
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

import java.util.List;
import java.util.Optional;

import static com.umc.linkyou.support.fixture.LinkuFixture.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
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
