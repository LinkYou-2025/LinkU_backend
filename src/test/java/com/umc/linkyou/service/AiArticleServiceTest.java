package com.umc.linkyou.service;

import com.umc.linkyou.apiPayload.exception.GeneralException;
import com.umc.linkyou.domain.AiArticle;
import com.umc.linkyou.domain.AlarmPayload;
import com.umc.linkyou.domain.Linku;
import com.umc.linkyou.domain.Users;
import com.umc.linkyou.domain.enums.AlarmType;
import com.umc.linkyou.domain.mapping.UsersLinku;
import com.umc.linkyou.infra.ai.AiArticleAnalyzer;
import com.umc.linkyou.infra.ai.dto.AiArticleResultDTO;
import com.umc.linkyou.repository.UserLinkuRepository.UsersLinkuRepository;
import com.umc.linkyou.repository.aiArticleRepository.AiArticleRepository;
import com.umc.linkyou.repository.linkuRepository.LinkuRepository;
import com.umc.linkyou.repository.userRepository.UserRepository;
import com.umc.linkyou.service.alarm.AlarmService;
import com.umc.linkyou.support.fixture.LinkuFixture;
import com.umc.linkyou.web.dto.alarm.AlarmRequestDTO;
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
    @Mock private AiArticleAnalyzer aiArticleAnalyzer;
    @Mock private AlarmService alarmService;

    private static final Long LINKU_ID = 100L;
    private static final Long USER_ID = 1L;
    private static final String SUMMARY = "테스트 AI 요약 내용";

    @Nested
    @DisplayName("saveAiArticle() - AI 요약 저장")
    class SaveAiArticle {

        @Nested
        @DisplayName("성공")
        class Success {

            @Test
            @DisplayName("AiArticle이 없으면 새로 생성하고 usersLinku.aiExist가 true로 설정된다")
            void AiArticle이_없으면_새로_생성하고_aiExist가_true이다() {
                Linku linku = LinkuFixture.linku(null);
                Users user = LinkuFixture.user();
                UsersLinku usersLinku = buildUsersLinku(linku, user);
                AiArticleResultDTO result = new AiArticleResultDTO(SUMMARY);

                given(linkuRepository.findById(LINKU_ID)).willReturn(Optional.of(linku));
                given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
                given(usersLinkuRepository.findByUser_IdAndLinku_LinkuId(USER_ID, LINKU_ID)).willReturn(List.of(usersLinku));
                given(aiArticleAnalyzer.analyzeByUrl(any())).willReturn(result);
                given(aiArticleRepository.findByLinku(linku)).willReturn(Optional.empty());
                given(aiArticleRepository.save(any(AiArticle.class))).willAnswer(inv -> inv.getArgument(0));

                aiArticleService.saveAiArticle(LINKU_ID, USER_ID);

                ArgumentCaptor<AiArticle> captor = ArgumentCaptor.forClass(AiArticle.class);
                verify(aiArticleRepository).save(captor.capture());
                assertEquals(SUMMARY, captor.getValue().getSummary());
                assertTrue(usersLinku.getAiExist());
            }

            @Test
            @DisplayName("AiArticle이 이미 있으면 summary만 업데이트하고 save를 다시 호출하지 않는다")
            void AiArticle이_있으면_summary_업데이트하고_save_재호출_안_한다() {
                Linku linku = LinkuFixture.linku(null);
                Users user = LinkuFixture.user();
                UsersLinku usersLinku = buildUsersLinku(linku, user);
                AiArticle existingArticle = LinkuFixture.aiArticle(linku, "이전 요약");
                AiArticleResultDTO result = new AiArticleResultDTO(SUMMARY);

                given(linkuRepository.findById(LINKU_ID)).willReturn(Optional.of(linku));
                given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
                given(usersLinkuRepository.findByUser_IdAndLinku_LinkuId(USER_ID, LINKU_ID)).willReturn(List.of(usersLinku));
                given(aiArticleAnalyzer.analyzeByUrl(any())).willReturn(result);
                given(aiArticleRepository.findByLinku(linku)).willReturn(Optional.of(existingArticle));

                aiArticleService.saveAiArticle(LINKU_ID, USER_ID);

                assertEquals(SUMMARY, existingArticle.getSummary());
                verify(aiArticleRepository, never()).save(any());
                assertTrue(usersLinku.getAiExist());
            }

            @Test
            @DisplayName("AI 분석 결과가 저장될 때 summary 값이 반영된다")
            void AI_분석_결과가_저장될때_summary_값이_반영된다() {
                Linku linku = LinkuFixture.linku(null);
                Users user = LinkuFixture.user();
                UsersLinku usersLinku = buildUsersLinku(linku, user);
                AiArticleResultDTO result = new AiArticleResultDTO(SUMMARY);

                given(linkuRepository.findById(LINKU_ID)).willReturn(Optional.of(linku));
                given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
                given(usersLinkuRepository.findByUser_IdAndLinku_LinkuId(USER_ID, LINKU_ID)).willReturn(List.of(usersLinku));
                given(aiArticleAnalyzer.analyzeByUrl(any())).willReturn(result);
                given(aiArticleRepository.findByLinku(linku)).willReturn(Optional.empty());
                given(aiArticleRepository.save(any(AiArticle.class))).willAnswer(inv -> inv.getArgument(0));

                aiArticleService.saveAiArticle(LINKU_ID, USER_ID);

                ArgumentCaptor<AiArticle> captor = ArgumentCaptor.forClass(AiArticle.class);
                verify(aiArticleRepository).save(captor.capture());
                assertEquals(SUMMARY, captor.getValue().getSummary());
            }

            @Test
            @DisplayName("요약 완료 시 링크 요약 알림을 발송한다")
            void 요약완료시_링크알림_발송() {
                Linku linku = LinkuFixture.linku(null);
                Users user = LinkuFixture.user();
                UsersLinku usersLinku = UsersLinku.builder()
                        .userLinkuId(10L)
                        .linku(linku)
                        .user(user)
                        .emotion(LinkuFixture.emotion())
                        .emotionAi(true)
                        .situationAi(true)
                        .title("내가 지은 링크 제목")
                        .build();
                AiArticleResultDTO result = new AiArticleResultDTO(SUMMARY);

                given(linkuRepository.findById(LINKU_ID)).willReturn(Optional.of(linku));
                given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
                given(usersLinkuRepository.findByUser_IdAndLinku_LinkuId(USER_ID, LINKU_ID)).willReturn(List.of(usersLinku));
                given(aiArticleAnalyzer.analyzeByUrl(any())).willReturn(result);
                given(aiArticleRepository.findByLinku(linku)).willReturn(Optional.empty());
                given(aiArticleRepository.save(any(AiArticle.class))).willAnswer(inv -> inv.getArgument(0));

                aiArticleService.saveAiArticle(LINKU_ID, USER_ID);

                verify(alarmService).sendAlarm(USER_ID, new AlarmRequestDTO.AlarmSendRequestDTO(
                        AlarmType.LINK_SUMMARY_COMPLETE, LINKU_ID,
                        new AlarmPayload.LinkTitle("내가 지은 링크 제목")));
            }

            @Test
            @DisplayName("사용자 지정 제목이 없으면 링크 원제목으로 알림을 발송한다")
            void 제목없으면_링크원제목으로_알림발송() {
                Linku linku = LinkuFixture.linku(null);
                Users user = LinkuFixture.user();
                UsersLinku usersLinku = buildUsersLinku(linku, user); // title 미설정
                AiArticleResultDTO result = new AiArticleResultDTO(SUMMARY);

                given(linkuRepository.findById(LINKU_ID)).willReturn(Optional.of(linku));
                given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
                given(usersLinkuRepository.findByUser_IdAndLinku_LinkuId(USER_ID, LINKU_ID)).willReturn(List.of(usersLinku));
                given(aiArticleAnalyzer.analyzeByUrl(any())).willReturn(result);
                given(aiArticleRepository.findByLinku(linku)).willReturn(Optional.empty());
                given(aiArticleRepository.save(any(AiArticle.class))).willAnswer(inv -> inv.getArgument(0));

                aiArticleService.saveAiArticle(LINKU_ID, USER_ID);

                verify(alarmService).sendAlarm(USER_ID, new AlarmRequestDTO.AlarmSendRequestDTO(
                        AlarmType.LINK_SUMMARY_COMPLETE, LINKU_ID,
                        new AlarmPayload.LinkTitle(linku.getTitle())));
            }

            @Test
            @DisplayName("동일 (user, linku)로 저장된 UsersLinku가 여러 건이면 전부 aiExist가 true로 표시된다")
            void UsersLinku가_여러건이면_전부_aiExist가_true로_표시된다() {
                // 같은 링크를 두 번 저장한 상황(1번째 저장, 2번째 저장) - 요약은 linku 단위로 한 번만
                // 생성되므로, 몇 번째 저장 건에 대해 요청했든 이 링크를 저장한 모든 건에 표시가 남아야 한다.
                Linku linku = LinkuFixture.linku(null);
                Users user = LinkuFixture.user();
                UsersLinku older = UsersLinku.builder()
                        .userLinkuId(1L)
                        .linku(linku)
                        .user(user)
                        .emotion(LinkuFixture.emotion())
                        .emotionAi(true)
                        .situationAi(true)
                        .build();
                org.springframework.test.util.ReflectionTestUtils.setField(
                        older, "createdAt", java.time.LocalDateTime.now().minusDays(1));
                UsersLinku newer = UsersLinku.builder()
                        .userLinkuId(2L)
                        .linku(linku)
                        .user(user)
                        .emotion(LinkuFixture.emotion())
                        .emotionAi(true)
                        .situationAi(true)
                        .build();
                org.springframework.test.util.ReflectionTestUtils.setField(
                        newer, "createdAt", java.time.LocalDateTime.now());
                AiArticleResultDTO result = new AiArticleResultDTO(SUMMARY);

                given(linkuRepository.findById(LINKU_ID)).willReturn(Optional.of(linku));
                given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
                given(usersLinkuRepository.findByUser_IdAndLinku_LinkuId(USER_ID, LINKU_ID)).willReturn(List.of(older, newer));
                given(aiArticleAnalyzer.analyzeByUrl(any())).willReturn(result);
                given(aiArticleRepository.findByLinku(linku)).willReturn(Optional.empty());
                given(aiArticleRepository.save(any(AiArticle.class))).willAnswer(inv -> inv.getArgument(0));

                aiArticleService.saveAiArticle(LINKU_ID, USER_ID);

                assertTrue(newer.getAiExist());
                assertTrue(older.getAiExist());
            }
        }

        @Nested
        @DisplayName("실패")
        class Failure {

            @Test
            @DisplayName("존재하지 않는 linkuId이면 예외가 발생한다")
            void 존재하지_않는_linkuId이면_예외가_발생한다() {
                given(linkuRepository.findById(LINKU_ID)).willReturn(Optional.empty());

                assertThrows(GeneralException.class,
                        () -> aiArticleService.saveAiArticle(LINKU_ID, USER_ID));
            }

            @Test
            @DisplayName("존재하지 않는 userId이면 예외가 발생한다")
            void 존재하지_않는_userId이면_예외가_발생한다() {
                Linku linku = LinkuFixture.linku(null);
                given(linkuRepository.findById(LINKU_ID)).willReturn(Optional.of(linku));
                given(userRepository.findById(USER_ID)).willReturn(Optional.empty());

                assertThrows(GeneralException.class,
                        () -> aiArticleService.saveAiArticle(LINKU_ID, USER_ID));
            }

            @Test
            @DisplayName("해당 사용자의 UsersLinku가 없으면 예외가 발생한다")
            void UsersLinku가_없으면_예외가_발생한다() {
                Linku linku = LinkuFixture.linku(null);
                Users user = LinkuFixture.user();

                given(linkuRepository.findById(LINKU_ID)).willReturn(Optional.of(linku));
                given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
                given(usersLinkuRepository.findByUser_IdAndLinku_LinkuId(USER_ID, LINKU_ID)).willReturn(List.of());

                assertThrows(GeneralException.class,
                        () -> aiArticleService.saveAiArticle(LINKU_ID, USER_ID));
            }
        }
    }

    @Nested
    @DisplayName("saveOrGetAiArticle() - AI 요약 요청/조회 분기")
    class SaveOrGetAiArticle {

        @Nested
        @DisplayName("성공")
        class Success {

            @Test
            @DisplayName("AiArticle이 없으면 AI를 호출하여 새로 생성한다")
            void AiArticle이_없으면_AI를_호출하여_새로_생성한다() {
                Linku linku = LinkuFixture.linku(null);
                Users user = LinkuFixture.user();
                UsersLinku usersLinku = buildUsersLinku(linku, user);
                AiArticleResultDTO result = new AiArticleResultDTO(SUMMARY);

                given(linkuRepository.findById(LINKU_ID)).willReturn(Optional.of(linku));
                given(aiArticleRepository.findByLinku(linku)).willReturn(Optional.empty());
                given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
                given(usersLinkuRepository.findByUser_IdAndLinku_LinkuId(USER_ID, LINKU_ID)).willReturn(List.of(usersLinku));
                given(aiArticleAnalyzer.analyzeByUrl(any())).willReturn(result);
                given(aiArticleRepository.save(any(AiArticle.class))).willAnswer(inv -> inv.getArgument(0));

                aiArticleService.saveOrGetAiArticle(LINKU_ID, USER_ID);

                verify(aiArticleAnalyzer).analyzeByUrl(any());
                verify(aiArticleRepository).save(any(AiArticle.class));
            }

            @Test
            @DisplayName("AiArticle의 summary가 blank이면 AI를 재호출하여 업데이트한다")
            void AiArticle_summary가_blank이면_AI를_재호출하여_업데이트한다() {
                Linku linku = LinkuFixture.linku(null);
                Users user = LinkuFixture.user();
                UsersLinku usersLinku = buildUsersLinku(linku, user);
                AiArticle existingArticle = LinkuFixture.aiArticle(linku, "");
                AiArticleResultDTO result = new AiArticleResultDTO(SUMMARY);

                given(linkuRepository.findById(LINKU_ID)).willReturn(Optional.of(linku));
                given(aiArticleRepository.findByLinku(linku)).willReturn(Optional.of(existingArticle));
                given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
                given(usersLinkuRepository.findByUser_IdAndLinku_LinkuId(USER_ID, LINKU_ID)).willReturn(List.of(usersLinku));
                given(aiArticleAnalyzer.analyzeByUrl(any())).willReturn(result);

                aiArticleService.saveOrGetAiArticle(LINKU_ID, USER_ID);

                verify(aiArticleAnalyzer).analyzeByUrl(any());
                assertEquals(SUMMARY, existingArticle.getSummary());
            }

            @Test
            @DisplayName("AiArticle의 summary가 있으면 AI를 호출하지 않고 기존 데이터를 사용한다")
            void AiArticle_summary가_있으면_AI_미호출하고_기존_데이터를_사용한다() {
                Linku linku = LinkuFixture.linku(null);
                Users user = LinkuFixture.user();
                UsersLinku usersLinku = buildUsersLinku(linku, user);
                AiArticle existingArticle = LinkuFixture.aiArticle(linku, SUMMARY);

                given(linkuRepository.findById(LINKU_ID)).willReturn(Optional.of(linku));
                given(aiArticleRepository.findByLinku(linku)).willReturn(Optional.of(existingArticle));
                given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
                given(usersLinkuRepository.findByUser_IdAndLinku_LinkuId(USER_ID, LINKU_ID)).willReturn(List.of(usersLinku));

                aiArticleService.saveOrGetAiArticle(LINKU_ID, USER_ID);

                verify(aiArticleAnalyzer, never()).analyzeByUrl(any());
                verify(aiArticleRepository, never()).save(any());
                // 요약 생성을 직접 요청한 게 아니라 이미 있는 요약을 조회(showAiArticle)한 경우에도
                // 본인이 실제로 확인한 것이므로 aiExist는 true로 표시되어야 한다.
                assertTrue(usersLinku.getAiExist());
            }

            @Test
            @DisplayName("AiArticle의 summary가 null이면 AI를 재호출하여 업데이트한다")
            void AiArticle_summary가_null이면_AI를_재호출하여_업데이트한다() {
                Linku linku = LinkuFixture.linku(null);
                Users user = LinkuFixture.user();
                UsersLinku usersLinku = buildUsersLinku(linku, user);
                AiArticle articleWithNullSummary = AiArticle.builder()
                        .id(1L)
                        .linku(linku)
                        .summary(null)
                        .build();
                AiArticleResultDTO result = new AiArticleResultDTO(SUMMARY);

                given(linkuRepository.findById(LINKU_ID)).willReturn(Optional.of(linku));
                given(aiArticleRepository.findByLinku(linku)).willReturn(Optional.of(articleWithNullSummary));
                given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
                given(usersLinkuRepository.findByUser_IdAndLinku_LinkuId(USER_ID, LINKU_ID)).willReturn(List.of(usersLinku));
                given(aiArticleAnalyzer.analyzeByUrl(any())).willReturn(result);

                aiArticleService.saveOrGetAiArticle(LINKU_ID, USER_ID);

                verify(aiArticleAnalyzer).analyzeByUrl(any());
                assertEquals(SUMMARY, articleWithNullSummary.getSummary());
            }
        }
    }

    @Nested
    @DisplayName("showAiArticle() - AI 요약 조회")
    class ShowAiArticle {

        @Nested
        @DisplayName("성공")
        class Success {

            @Test
            @DisplayName("다른 유저가 먼저 만들어둔 요약이라도 본인이 조회하면 본인 소유 UsersLinku의 aiExist가 true로 표시된다")
            void 다른_유저가_만든_요약이라도_본인이_조회하면_aiExist가_true로_표시된다() {
                Linku linku = LinkuFixture.linku(null);
                Users user = LinkuFixture.user();
                // 저장 시점에는 본인이 요청/조회한 적이 없어 aiExist=false였던 상태 (다른 유저가 먼저 요약함)
                UsersLinku usersLinku = buildUsersLinku(linku, user);
                AiArticle existingArticle = LinkuFixture.aiArticle(linku, SUMMARY);

                given(linkuRepository.findById(LINKU_ID)).willReturn(Optional.of(linku));
                given(aiArticleRepository.findByLinku(linku)).willReturn(Optional.of(existingArticle));
                given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
                given(usersLinkuRepository.findByUser_IdAndLinku_LinkuId(USER_ID, LINKU_ID)).willReturn(List.of(usersLinku));

                assertFalse(usersLinku.getAiExist());

                aiArticleService.showAiArticle(LINKU_ID, USER_ID);

                assertTrue(usersLinku.getAiExist());
            }

            @Test
            @DisplayName("동일 (user, linku)로 저장된 UsersLinku가 여러 건이면 조회 시 전부 aiExist가 true로 표시된다")
            void 여러건이면_조회시_전부_aiExist가_true로_표시된다() {
                Linku linku = LinkuFixture.linku(null);
                Users user = LinkuFixture.user();
                UsersLinku older = buildUsersLinku(linku, user);
                org.springframework.test.util.ReflectionTestUtils.setField(
                        older, "createdAt", java.time.LocalDateTime.now().minusDays(1));
                UsersLinku newer = buildUsersLinku(linku, user);
                org.springframework.test.util.ReflectionTestUtils.setField(
                        newer, "createdAt", java.time.LocalDateTime.now());
                AiArticle existingArticle = LinkuFixture.aiArticle(linku, SUMMARY);

                given(linkuRepository.findById(LINKU_ID)).willReturn(Optional.of(linku));
                given(aiArticleRepository.findByLinku(linku)).willReturn(Optional.of(existingArticle));
                given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
                given(usersLinkuRepository.findByUser_IdAndLinku_LinkuId(USER_ID, LINKU_ID)).willReturn(List.of(older, newer));

                aiArticleService.showAiArticle(LINKU_ID, USER_ID);

                assertTrue(older.getAiExist());
                assertTrue(newer.getAiExist());
            }
        }

        @Nested
        @DisplayName("실패")
        class Failure {

            @Test
            @DisplayName("요청 유저가 이 linku를 저장한 적이 없으면 예외가 발생한다")
            void 소유권_없으면_예외가_발생한다() {
                Linku linku = LinkuFixture.linku(null);
                Users user = LinkuFixture.user();
                AiArticle existingArticle = LinkuFixture.aiArticle(linku, SUMMARY);

                given(linkuRepository.findById(LINKU_ID)).willReturn(Optional.of(linku));
                given(aiArticleRepository.findByLinku(linku)).willReturn(Optional.of(existingArticle));
                given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
                given(usersLinkuRepository.findByUser_IdAndLinku_LinkuId(USER_ID, LINKU_ID)).willReturn(List.of());

                assertThrows(GeneralException.class,
                        () -> aiArticleService.showAiArticle(LINKU_ID, USER_ID));
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
