package com.umc.linkyou.service;

import com.umc.linkyou.apiPayload.code.status.aiarticle.AiArticleErrorStatus;
import com.umc.linkyou.apiPayload.code.status.linku.LinkuErrorStatus;
import com.umc.linkyou.apiPayload.code.status.user.UserErrorStatus;
import com.umc.linkyou.apiPayload.code.status.ErrorStatus;
import com.umc.linkyou.apiPayload.exception.GeneralException;
import com.umc.linkyou.domain.AiArticle;
import com.umc.linkyou.domain.Linku;
import com.umc.linkyou.domain.Users;
import com.umc.linkyou.domain.enums.AiArticleStatus;
import com.umc.linkyou.domain.mapping.UsersLinku;
import com.umc.linkyou.repository.UserLinkuRepository.UsersLinkuRepository;
import com.umc.linkyou.repository.aiArticleRepository.AiArticleRepository;
import com.umc.linkyou.repository.linkuRepository.LinkuRepository;
import com.umc.linkyou.repository.userRepository.UserRepository;
import com.umc.linkyou.support.fixture.LinkuFixture;
import com.umc.linkyou.web.dto.AiArticleResponseDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AiArticleService 테스트")
class AiArticleServiceTest {

    @InjectMocks
    private AiArticleService aiArticleService;

    @Mock private UserRepository userRepository;
    @Mock private LinkuRepository linkuRepository;
    @Mock private AiArticleRepository aiArticleRepository;
    @Mock private UsersLinkuRepository usersLinkuRepository;
    @Mock private AiArticleGenerationWorker aiArticleGenerationWorker;

    private static final Long LINKU_ID = 100L;
    private static final Long USER_ID = 1L;
    private static final String SUMMARY = "테스트 AI 요약 내용";

    @Nested
    @DisplayName("createAiArticle() - AI 요약 생성 요청")
    class CreateAiArticle {

        @Nested
        @DisplayName("성공")
        class Success {

            @Test
            @DisplayName("AiArticle이 없으면 PENDING으로 새로 생성하고 비동기 워커를 호출한다")
            void AiArticle이_없으면_PENDING으로_생성하고_워커를_호출한다() {
                Linku linku = LinkuFixture.linku(null);
                Users user = LinkuFixture.user();
                UsersLinku usersLinku = buildUsersLinku(linku, user);

                given(linkuRepository.findById(LINKU_ID)).willReturn(Optional.of(linku));
                given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
                given(usersLinkuRepository.findByUser_IdAndLinku_LinkuId(USER_ID, LINKU_ID)).willReturn(List.of(usersLinku));
                given(aiArticleRepository.findByLinku(linku)).willReturn(Optional.empty());
                given(aiArticleRepository.save(any(AiArticle.class))).willAnswer(inv -> {
                    AiArticle saved = inv.getArgument(0);
                    org.springframework.test.util.ReflectionTestUtils.setField(saved, "id", 500L);
                    return saved;
                });

                AiArticleResponseDTO.AiArticleResultDTO result =
                        aiArticleService.createAiArticle(LINKU_ID, USER_ID);

                assertEquals("PENDING", result.getStatus());
                assertNull(result.getSummary());

                ArgumentCaptor<AiArticle> captor = ArgumentCaptor.forClass(AiArticle.class);
                verify(aiArticleRepository).save(captor.capture());
                assertEquals(AiArticleStatus.PENDING, captor.getValue().getStatus());

                verify(aiArticleGenerationWorker).generateAsync(500L, USER_ID);
            }

            @Test
            @DisplayName("이전 생성이 실패(FAILED)했던 링크는 재시도로 PENDING으로 되돌리고 워커를 다시 호출한다")
            void FAILED_상태면_재시도로_PENDING으로_되돌리고_워커를_호출한다() {
                Linku linku = LinkuFixture.linku(null);
                Users user = LinkuFixture.user();
                UsersLinku usersLinku = buildUsersLinku(linku, user);
                AiArticle failedArticle = AiArticle.builder()
                        .id(700L)
                        .linku(linku)
                        .status(AiArticleStatus.FAILED)
                        .failReason("CRAWLER4031")
                        .build();

                given(linkuRepository.findById(LINKU_ID)).willReturn(Optional.of(linku));
                given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
                given(usersLinkuRepository.findByUser_IdAndLinku_LinkuId(USER_ID, LINKU_ID)).willReturn(List.of(usersLinku));
                given(aiArticleRepository.findByLinku(linku)).willReturn(Optional.of(failedArticle));

                AiArticleResponseDTO.AiArticleResultDTO result =
                        aiArticleService.createAiArticle(LINKU_ID, USER_ID);

                assertEquals(AiArticleStatus.PENDING, failedArticle.getStatus());
                assertNull(failedArticle.getFailReason());
                assertEquals("PENDING", result.getStatus());
                verify(aiArticleRepository, never()).save(any());
                verify(aiArticleGenerationWorker).generateAsync(700L, USER_ID);
            }
        }

        @Nested
        @DisplayName("실패")
        class Failure {

            @Test
            @DisplayName("존재하지 않는 linkuId이면 예외가 발생한다")
            void 존재하지_않는_linkuId이면_예외가_발생한다() {
                given(linkuRepository.findById(LINKU_ID)).willReturn(Optional.empty());

                GeneralException ex = assertThrows(GeneralException.class,
                        () -> aiArticleService.createAiArticle(LINKU_ID, USER_ID));
                assertEquals(ErrorStatus._BAD_REQUEST, ex.getCode());
            }

            @Test
            @DisplayName("존재하지 않는 userId이면 예외가 발생한다")
            void 존재하지_않는_userId이면_예외가_발생한다() {
                Linku linku = LinkuFixture.linku(null);
                given(linkuRepository.findById(LINKU_ID)).willReturn(Optional.of(linku));
                given(userRepository.findById(USER_ID)).willReturn(Optional.empty());

                GeneralException ex = assertThrows(GeneralException.class,
                        () -> aiArticleService.createAiArticle(LINKU_ID, USER_ID));
                assertEquals(UserErrorStatus._USER_NOT_FOUND, ex.getCode());
            }

            @Test
            @DisplayName("해당 사용자의 UsersLinku가 없으면 예외가 발생한다")
            void UsersLinku가_없으면_예외가_발생한다() {
                Linku linku = LinkuFixture.linku(null);
                Users user = LinkuFixture.user();

                given(linkuRepository.findById(LINKU_ID)).willReturn(Optional.of(linku));
                given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
                given(usersLinkuRepository.findByUser_IdAndLinku_LinkuId(USER_ID, LINKU_ID)).willReturn(List.of());

                GeneralException ex = assertThrows(GeneralException.class,
                        () -> aiArticleService.createAiArticle(LINKU_ID, USER_ID));
                assertEquals(LinkuErrorStatus._USER_LINKU_NOT_FOUND, ex.getCode());
            }

            @Test
            @DisplayName("이미 생성 완료(DONE)된 링크면 409 예외가 발생하고 워커를 호출하지 않는다")
            void 이미_DONE이면_예외가_발생하고_워커를_호출하지_않는다() {
                Linku linku = LinkuFixture.linku(null);
                Users user = LinkuFixture.user();
                UsersLinku usersLinku = buildUsersLinku(linku, user);
                AiArticle doneArticle = LinkuFixture.aiArticle(linku, SUMMARY);

                given(linkuRepository.findById(LINKU_ID)).willReturn(Optional.of(linku));
                given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
                given(usersLinkuRepository.findByUser_IdAndLinku_LinkuId(USER_ID, LINKU_ID)).willReturn(List.of(usersLinku));
                given(aiArticleRepository.findByLinku(linku)).willReturn(Optional.of(doneArticle));

                GeneralException ex = assertThrows(GeneralException.class,
                        () -> aiArticleService.createAiArticle(LINKU_ID, USER_ID));
                assertEquals(AiArticleErrorStatus._DUPLICATE_AI_ARTICLE, ex.getCode());
                verify(aiArticleGenerationWorker, never()).generateAsync(any(), any());
            }

            @Test
            @DisplayName("이미 생성 진행 중(PENDING)인 링크면 409 예외가 발생하고 워커를 호출하지 않는다")
            void 이미_PENDING이면_예외가_발생하고_워커를_호출하지_않는다() {
                Linku linku = LinkuFixture.linku(null);
                Users user = LinkuFixture.user();
                UsersLinku usersLinku = buildUsersLinku(linku, user);
                AiArticle pendingArticle = AiArticle.builder()
                        .id(800L)
                        .linku(linku)
                        .status(AiArticleStatus.PENDING)
                        .build();

                given(linkuRepository.findById(LINKU_ID)).willReturn(Optional.of(linku));
                given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
                given(usersLinkuRepository.findByUser_IdAndLinku_LinkuId(USER_ID, LINKU_ID)).willReturn(List.of(usersLinku));
                given(aiArticleRepository.findByLinku(linku)).willReturn(Optional.of(pendingArticle));

                GeneralException ex = assertThrows(GeneralException.class,
                        () -> aiArticleService.createAiArticle(LINKU_ID, USER_ID));
                assertEquals(AiArticleErrorStatus._AI_ARTICLE_GENERATING, ex.getCode());
                verify(aiArticleGenerationWorker, never()).generateAsync(any(), any());
            }
        }
    }

    @Nested
    @DisplayName("getAiArticle() - AI 요약 조회")
    class GetAiArticle {

        @Nested
        @DisplayName("성공")
        class Success {

            @Test
            @DisplayName("status가 DONE이면 본인 소유 UsersLinku의 aiExist가 true로 표시된다")
            void status가_DONE이면_aiExist가_true로_표시된다() {
                Linku linku = LinkuFixture.linku(null);
                Users user = LinkuFixture.user();
                UsersLinku usersLinku = buildUsersLinku(linku, user);
                AiArticle article = LinkuFixture.aiArticle(linku, SUMMARY);

                given(linkuRepository.findById(LINKU_ID)).willReturn(Optional.of(linku));
                given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
                given(usersLinkuRepository.findByUser_IdAndLinku_LinkuId(USER_ID, LINKU_ID)).willReturn(List.of(usersLinku));
                given(aiArticleRepository.findByLinku(linku)).willReturn(Optional.of(article));

                assertFalse(usersLinku.getAiExist());

                AiArticleResponseDTO.AiArticleResultDTO result =
                        aiArticleService.getAiArticle(LINKU_ID, USER_ID);

                assertEquals("DONE", result.getStatus());
                assertEquals(SUMMARY, result.getSummary());
                assertTrue(usersLinku.getAiExist());
            }

            @Test
            @DisplayName("status가 PENDING이면 아직 aiExist를 표시하지 않는다")
            void status가_PENDING이면_aiExist를_표시하지_않는다() {
                Linku linku = LinkuFixture.linku(null);
                Users user = LinkuFixture.user();
                UsersLinku usersLinku = buildUsersLinku(linku, user);
                AiArticle article = AiArticle.builder()
                        .id(900L)
                        .linku(linku)
                        .status(AiArticleStatus.PENDING)
                        .build();

                given(linkuRepository.findById(LINKU_ID)).willReturn(Optional.of(linku));
                given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
                given(usersLinkuRepository.findByUser_IdAndLinku_LinkuId(USER_ID, LINKU_ID)).willReturn(List.of(usersLinku));
                given(aiArticleRepository.findByLinku(linku)).willReturn(Optional.of(article));

                AiArticleResponseDTO.AiArticleResultDTO result =
                        aiArticleService.getAiArticle(LINKU_ID, USER_ID);

                assertEquals("PENDING", result.getStatus());
                assertNull(result.getSummary());
                assertFalse(usersLinku.getAiExist());
            }
        }

        @Nested
        @DisplayName("실패")
        class Failure {

            @Test
            @DisplayName("AiArticle 레코드가 없으면 예외가 발생한다")
            void AiArticle이_없으면_예외가_발생한다() {
                Linku linku = LinkuFixture.linku(null);
                Users user = LinkuFixture.user();
                UsersLinku usersLinku = buildUsersLinku(linku, user);

                given(linkuRepository.findById(LINKU_ID)).willReturn(Optional.of(linku));
                given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
                given(usersLinkuRepository.findByUser_IdAndLinku_LinkuId(USER_ID, LINKU_ID)).willReturn(List.of(usersLinku));
                given(aiArticleRepository.findByLinku(linku)).willReturn(Optional.empty());

                GeneralException ex = assertThrows(GeneralException.class,
                        () -> aiArticleService.getAiArticle(LINKU_ID, USER_ID));
                assertEquals(AiArticleErrorStatus._AI_ARTICLE_NOT_FOUND, ex.getCode());
            }

            @Test
            @DisplayName("요청 유저가 이 linku를 저장한 적이 없으면 예외가 발생한다")
            void 소유권_없으면_예외가_발생한다() {
                Linku linku = LinkuFixture.linku(null);
                Users user = LinkuFixture.user();

                given(linkuRepository.findById(LINKU_ID)).willReturn(Optional.of(linku));
                given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
                given(usersLinkuRepository.findByUser_IdAndLinku_LinkuId(USER_ID, LINKU_ID)).willReturn(List.of());

                GeneralException ex = assertThrows(GeneralException.class,
                        () -> aiArticleService.getAiArticle(LINKU_ID, USER_ID));
                assertEquals(LinkuErrorStatus._USER_LINKU_NOT_FOUND, ex.getCode());
            }
        }
    }

    @Nested
    @DisplayName("getMyAiArticlesByCategory() - 카테고리별 AI 요약 링크 조회")
    class GetMyAiArticlesByCategory {

        private static final Long CURSOR = null;
        private static final int LIMIT = 10;

        @Test
        @DisplayName("categoryId가 null이면 카테고리 필터 없이 repository를 호출한다(전체 카테고리 조회)")
        void categoryId가_null이면_전체_카테고리로_조회한다() {
            Linku linku = LinkuFixture.linku(null);
            Users user = LinkuFixture.user();
            UsersLinku usersLinku = buildUsersLinku(linku, user);

            given(usersLinkuRepository.fetchAiArticlesByCategoryIdWithCursor(USER_ID, null, CURSOR, LIMIT))
                    .willReturn(List.of(usersLinku));

            var result = aiArticleService.getMyAiArticlesByCategory(USER_ID, null, CURSOR, LIMIT);

            verify(usersLinkuRepository).fetchAiArticlesByCategoryIdWithCursor(USER_ID, null, CURSOR, LIMIT);
            assertEquals(1, result.getLinkuList().size());
            assertFalse(result.getHasNext());
            // "전체" 탭에서는 여러 카테고리가 섞여 나오므로, 각 항목에 카테고리 정보가 실려 있어야 한다.
            assertEquals(LinkuFixture.CATEGORY_ID, result.getLinkuList().get(0).getCategoryId());
            assertEquals("기술", result.getLinkuList().get(0).getCategoryName());
        }

        @Test
        @DisplayName("categoryId가 있으면 해당 카테고리로 필터링해 repository를 호출한다")
        void categoryId가_있으면_해당_카테고리로_필터링한다() {
            Long categoryId = 3L;
            Linku linku = LinkuFixture.linku(null);
            Users user = LinkuFixture.user();
            UsersLinku usersLinku = buildUsersLinku(linku, user);

            given(usersLinkuRepository.fetchAiArticlesByCategoryIdWithCursor(USER_ID, categoryId, CURSOR, LIMIT))
                    .willReturn(List.of(usersLinku));

            var result = aiArticleService.getMyAiArticlesByCategory(USER_ID, categoryId, CURSOR, LIMIT);

            verify(usersLinkuRepository).fetchAiArticlesByCategoryIdWithCursor(USER_ID, categoryId, CURSOR, LIMIT);
            assertEquals(1, result.getLinkuList().size());
        }

        @Test
        @DisplayName("결과가 limit보다 많으면 hasNext=true와 다음 커서를 반환한다")
        void 결과가_limit보다_많으면_hasNext와_nextCursor를_반환한다() {
            Linku linku = LinkuFixture.linku(null);
            Users user = LinkuFixture.user();
            UsersLinku first = UsersLinku.builder()
                    .userLinkuId(1L).linku(linku).user(user)
                    .emotion(LinkuFixture.emotion()).emotionAi(true).situationAi(true).build();
            UsersLinku second = UsersLinku.builder()
                    .userLinkuId(2L).linku(linku).user(user)
                    .emotion(LinkuFixture.emotion()).emotionAi(true).situationAi(true).build();

            given(usersLinkuRepository.fetchAiArticlesByCategoryIdWithCursor(USER_ID, null, CURSOR, 1))
                    .willReturn(List.of(first, second)); // limit(1) + 1건 = 다음 페이지 있음

            var result = aiArticleService.getMyAiArticlesByCategory(USER_ID, null, CURSOR, 1);

            assertTrue(result.getHasNext());
            assertEquals("1", result.getNextCursor());
            assertEquals(1, result.getLinkuList().size());
        }
    }

    private UsersLinku buildUsersLinku(Linku linku, Users user) {
        return UsersLinku.builder()
                .userLinkuId(10L)
                .linku(linku)
                .user(user)
                .emotion(LinkuFixture.emotion())
                .emotionAi(true)
                .situationAi(true)
                .build();
    }
}
