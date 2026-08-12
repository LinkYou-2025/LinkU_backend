package com.umc.linkyou.service;

import com.umc.linkyou.apiPayload.code.status.aiarticle.AiArticleErrorStatus;
import com.umc.linkyou.apiPayload.exception.GeneralException;
import com.umc.linkyou.domain.AiArticle;
import com.umc.linkyou.domain.AlarmPayload;
import com.umc.linkyou.domain.Linku;
import com.umc.linkyou.domain.Users;
import com.umc.linkyou.domain.enums.AiArticleStatus;
import com.umc.linkyou.domain.enums.AlarmType;
import com.umc.linkyou.domain.mapping.UsersLinku;
import com.umc.linkyou.infra.ai.AiArticleAnalyzer;
import com.umc.linkyou.infra.ai.dto.AiArticleResultDTO;
import com.umc.linkyou.repository.UserLinkuRepository.UsersLinkuRepository;
import com.umc.linkyou.repository.aiArticleRepository.AiArticleRepository;
import com.umc.linkyou.service.alarm.AlarmService;
import com.umc.linkyou.support.fixture.LinkuFixture;
import com.umc.linkyou.web.dto.alarm.AlarmRequestDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("AiArticleGenerationWorker 테스트")
class AiArticleGenerationWorkerTest {

    @InjectMocks
    private AiArticleGenerationWorker worker;

    @Mock private AiArticleRepository aiArticleRepository;
    @Mock private UsersLinkuRepository usersLinkuRepository;
    @Mock private AiArticleAnalyzer aiArticleAnalyzer;
    @Mock private AlarmService alarmService;

    private static final Long ARTICLE_ID = 700L;
    private static final Long LINKU_ID = 100L;
    private static final Long USER_ID = 1L;
    private static final String SUMMARY = "테스트 AI 요약 내용";

    @Nested
    @DisplayName("generateAsync()")
    class GenerateAsync {

        @Nested
        @DisplayName("성공")
        class Success {

            @Test
            @DisplayName("분석에 성공하면 status가 DONE으로 바뀌고 aiExist 표시와 알림 발송이 이루어진다")
            void 분석_성공시_DONE으로_바뀌고_aiExist_표시와_알림이_발송된다() {
                Linku linku = LinkuFixture.linku(null);
                Users user = LinkuFixture.user();
                AiArticle article = AiArticle.builder()
                        .id(ARTICLE_ID)
                        .linku(linku)
                        .status(AiArticleStatus.PENDING)
                        .build();
                UsersLinku usersLinku = UsersLinku.builder()
                        .userLinkuId(10L)
                        .linku(linku)
                        .user(user)
                        .emotion(LinkuFixture.emotion())
                        .emotionAi(true)
                        .situationAi(true)
                        .title("내가 지은 제목")
                        .build();

                given(aiArticleRepository.findById(ARTICLE_ID)).willReturn(Optional.of(article));
                given(aiArticleAnalyzer.analyzeByUrl(linku.getLinkuUrl())).willReturn(new AiArticleResultDTO(SUMMARY));
                given(usersLinkuRepository.findByUser_IdAndLinku_LinkuId(USER_ID, LINKU_ID)).willReturn(List.of(usersLinku));

                worker.generateAsync(ARTICLE_ID, USER_ID);

                assertEquals(AiArticleStatus.DONE, article.getStatus());
                assertEquals(SUMMARY, article.getSummary());
                assertNull(article.getFailReason());
                assertTrue(usersLinku.getAiExist());

                verify(alarmService).sendAlarm(USER_ID, new AlarmRequestDTO.AlarmSendRequestDTO(
                        AlarmType.LINK_SUMMARY_COMPLETE, LINKU_ID, new AlarmPayload.LinkTitle("내가 지은 제목")));
            }

            @Test
            @DisplayName("AiArticle 레코드를 찾을 수 없으면 아무 것도 하지 않고 조용히 종료한다")
            void AiArticle을_찾을_수_없으면_아무것도_하지_않는다() {
                given(aiArticleRepository.findById(ARTICLE_ID)).willReturn(Optional.empty());

                worker.generateAsync(ARTICLE_ID, USER_ID);

                verify(aiArticleAnalyzer, never()).analyzeByUrl(any());
                verify(alarmService, never()).sendAlarm(any(), any());
            }
        }

        @Nested
        @DisplayName("실패")
        class Failure {

            @Test
            @DisplayName("robots.txt 차단 등 GeneralException이면 status가 FAILED로 바뀌고 실패 사유 코드가 남는다")
            void GeneralException이면_FAILED로_바뀌고_실패사유가_남는다() {
                Linku linku = LinkuFixture.linku(null);
                AiArticle article = AiArticle.builder()
                        .id(ARTICLE_ID)
                        .linku(linku)
                        .status(AiArticleStatus.PENDING)
                        .build();

                given(aiArticleRepository.findById(ARTICLE_ID)).willReturn(Optional.of(article));
                given(aiArticleAnalyzer.analyzeByUrl(linku.getLinkuUrl()))
                        .willThrow(new GeneralException(AiArticleErrorStatus._CONTENT_EXTRACTION_PROHIBITED));

                worker.generateAsync(ARTICLE_ID, USER_ID);

                assertEquals(AiArticleStatus.FAILED, article.getStatus());
                assertEquals("CRAWLER4031", article.getFailReason());
                assertNull(article.getSummary());
                verify(alarmService, never()).sendAlarm(any(), any());
            }

            @Test
            @DisplayName("예상치 못한 예외면 status가 FAILED로 바뀌고 실패 사유가 UNKNOWN으로 남는다")
            void 알수없는_예외면_FAILED로_바뀌고_UNKNOWN이_남는다() {
                Linku linku = LinkuFixture.linku(null);
                AiArticle article = AiArticle.builder()
                        .id(ARTICLE_ID)
                        .linku(linku)
                        .status(AiArticleStatus.PENDING)
                        .build();

                given(aiArticleRepository.findById(ARTICLE_ID)).willReturn(Optional.of(article));
                given(aiArticleAnalyzer.analyzeByUrl(linku.getLinkuUrl()))
                        .willThrow(new RuntimeException("네트워크 오류"));

                worker.generateAsync(ARTICLE_ID, USER_ID);

                assertEquals(AiArticleStatus.FAILED, article.getStatus());
                assertEquals("UNKNOWN", article.getFailReason());
                verify(alarmService, never()).sendAlarm(any(), any());
            }
        }
    }
}
