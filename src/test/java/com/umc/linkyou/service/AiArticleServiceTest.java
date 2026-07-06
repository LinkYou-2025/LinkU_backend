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
                given(usersLinkuRepository.findByUserAndLinku(user, linku)).willReturn(Optional.of(usersLinku));
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
                given(usersLinkuRepository.findByUserAndLinku(user, linku)).willReturn(Optional.of(usersLinku));
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
                given(usersLinkuRepository.findByUserAndLinku(user, linku)).willReturn(Optional.of(usersLinku));
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
                given(usersLinkuRepository.findByUserAndLinku(user, linku)).willReturn(Optional.of(usersLinku));
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
                given(usersLinkuRepository.findByUserAndLinku(user, linku)).willReturn(Optional.of(usersLinku));
                given(aiArticleAnalyzer.analyzeByUrl(any())).willReturn(result);
                given(aiArticleRepository.findByLinku(linku)).willReturn(Optional.empty());
                given(aiArticleRepository.save(any(AiArticle.class))).willAnswer(inv -> inv.getArgument(0));

                aiArticleService.saveAiArticle(LINKU_ID, USER_ID);

                verify(alarmService).sendAlarm(USER_ID, new AlarmRequestDTO.AlarmSendRequestDTO(
                        AlarmType.LINK_SUMMARY_COMPLETE, LINKU_ID,
                        new AlarmPayload.LinkTitle(linku.getTitle())));
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
                given(usersLinkuRepository.findByUserAndLinku(user, linku)).willReturn(Optional.empty());

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
                given(usersLinkuRepository.findByUserAndLinku(user, linku)).willReturn(Optional.of(usersLinku));
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
                given(usersLinkuRepository.findByUserAndLinku(user, linku)).willReturn(Optional.of(usersLinku));
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
                given(usersLinkuRepository.findByUserAndLinku(user, linku)).willReturn(Optional.of(usersLinku));

                aiArticleService.saveOrGetAiArticle(LINKU_ID, USER_ID);

                verify(aiArticleAnalyzer, never()).analyzeByUrl(any());
                verify(aiArticleRepository, never()).save(any());
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
                        .title("테스트 제목")
                        .summary(null)
                        .build();
                AiArticleResultDTO result = new AiArticleResultDTO(SUMMARY);

                given(linkuRepository.findById(LINKU_ID)).willReturn(Optional.of(linku));
                given(aiArticleRepository.findByLinku(linku)).willReturn(Optional.of(articleWithNullSummary));
                given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
                given(usersLinkuRepository.findByUserAndLinku(user, linku)).willReturn(Optional.of(usersLinku));
                given(aiArticleAnalyzer.analyzeByUrl(any())).willReturn(result);

                aiArticleService.saveOrGetAiArticle(LINKU_ID, USER_ID);

                verify(aiArticleAnalyzer).analyzeByUrl(any());
                assertEquals(SUMMARY, articleWithNullSummary.getSummary());
            }
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
