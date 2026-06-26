package com.umc.linkyou.service;

import com.umc.linkyou.apiPayload.exception.GeneralException;
import com.umc.linkyou.domain.AiArticle;
import com.umc.linkyou.domain.Linku;
import com.umc.linkyou.domain.Users;
import com.umc.linkyou.domain.mapping.UsersLinku;
import com.umc.linkyou.infra.ai.AiArticleAnalyzer;
import com.umc.linkyou.infra.ai.dto.AiArticleResultDTO;
import com.umc.linkyou.repository.aiArticleRepository.AiArticleRepository;
import com.umc.linkyou.repository.linkuRepository.LinkuRepository;
import com.umc.linkyou.repository.UserLinkuRepository.UsersLinkuRepository;
import com.umc.linkyou.repository.userRepository.UserRepository;
import com.umc.linkyou.support.fixture.LinkuFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
                // given
                Linku linku = LinkuFixture.linku(null);
                Users user = LinkuFixture.user();
                UsersLinku usersLinku = buildUsersLinku(linku, user);
                AiArticleResultDTO result = new AiArticleResultDTO(SUMMARY);
                AiArticle savedArticle = LinkuFixture.aiArticle(linku, SUMMARY);

                given(linkuRepository.findById(LINKU_ID)).willReturn(Optional.of(linku));
                given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
                given(usersLinkuRepository.findByUserAndLinku(user, linku)).willReturn(Optional.of(usersLinku));
                given(aiArticleAnalyzer.analyzeByUrl(any())).willReturn(result);
                given(aiArticleRepository.findByLinku(linku)).willReturn(Optional.empty());
                given(aiArticleRepository.save(any())).willReturn(savedArticle);

                // when
                aiArticleService.saveAiArticle(LINKU_ID, USER_ID);

                // then: 새 AiArticle 저장, usersLinku.aiExist = true
                verify(aiArticleRepository).save(any(AiArticle.class));
                assertTrue(usersLinku.getAiExist());
            }

            @Test
            @DisplayName("AiArticle이 이미 있으면 summary만 업데이트하고 save를 다시 호출하지 않는다")
            void AiArticle이_있으면_summary_업데이트하고_save_재호출_안_한다() {
                // given
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

                // when
                aiArticleService.saveAiArticle(LINKU_ID, USER_ID);

                // then: 기존 summary 업데이트, 새로 save() 미호출
                assertEquals(SUMMARY, existingArticle.getSummary());
                verify(aiArticleRepository, never()).save(any());
                assertTrue(usersLinku.getAiExist());
            }

            @Test
            @DisplayName("AI 분석 후 linku.aiArticle 참조가 설정된다")
            void AI_분석_후_linku_aiArticle_참조가_설정된다() {
                // given
                Linku linku = LinkuFixture.linku(null); // aiArticle=null
                Users user = LinkuFixture.user();
                UsersLinku usersLinku = buildUsersLinku(linku, user);
                AiArticleResultDTO result = new AiArticleResultDTO(SUMMARY);
                AiArticle savedArticle = LinkuFixture.aiArticle(linku, SUMMARY);

                given(linkuRepository.findById(LINKU_ID)).willReturn(Optional.of(linku));
                given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
                given(usersLinkuRepository.findByUserAndLinku(user, linku)).willReturn(Optional.of(usersLinku));
                given(aiArticleAnalyzer.analyzeByUrl(any())).willReturn(result);
                given(aiArticleRepository.findByLinku(linku)).willReturn(Optional.empty());
                given(aiArticleRepository.save(any())).willReturn(savedArticle);

                // when
                aiArticleService.saveAiArticle(LINKU_ID, USER_ID);

                // then: linku.aiArticle이 savedArticle로 설정됨
                assertEquals(savedArticle, linku.getAiArticle());
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
                // given
                Linku linku = LinkuFixture.linku(null);
                Users user = LinkuFixture.user();
                UsersLinku usersLinku = buildUsersLinku(linku, user);
                AiArticleResultDTO result = new AiArticleResultDTO(SUMMARY);
                AiArticle savedArticle = LinkuFixture.aiArticle(linku, SUMMARY);

                given(linkuRepository.findById(LINKU_ID)).willReturn(Optional.of(linku));
                given(aiArticleRepository.findByLinku(linku)).willReturn(Optional.empty());
                // saveAiArticle 내부 재조회
                given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
                given(usersLinkuRepository.findByUserAndLinku(user, linku)).willReturn(Optional.of(usersLinku));
                given(aiArticleAnalyzer.analyzeByUrl(any())).willReturn(result);
                given(aiArticleRepository.save(any())).willReturn(savedArticle);

                // when
                aiArticleService.saveOrGetAiArticle(LINKU_ID, USER_ID);

                // then: AI 호출됨
                verify(aiArticleAnalyzer).analyzeByUrl(any());
            }

            @Test
            @DisplayName("AiArticle의 summary가 blank이면 AI를 재호출하여 업데이트한다")
            void AiArticle_summary가_blank이면_AI를_재호출하여_업데이트한다() {
                // given: AiArticle 있으나 summary가 blank
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

                // when
                aiArticleService.saveOrGetAiArticle(LINKU_ID, USER_ID);

                // then: AI 호출됨, summary 업데이트
                verify(aiArticleAnalyzer).analyzeByUrl(any());
                assertEquals(SUMMARY, existingArticle.getSummary());
            }

            @Test
            @DisplayName("AiArticle의 summary가 있으면 AI를 호출하지 않고 기존 데이터를 반환한다")
            void AiArticle_summary가_있으면_AI_미호출하고_기존_데이터를_반환한다() {
                // given: AiArticle 있고 summary 존재
                Linku linku = LinkuFixture.linku(null);
                Users user = LinkuFixture.user();
                UsersLinku usersLinku = buildUsersLinku(linku, user);
                AiArticle existingArticle = LinkuFixture.aiArticle(linku, SUMMARY);

                given(linkuRepository.findById(LINKU_ID)).willReturn(Optional.of(linku));
                given(aiArticleRepository.findByLinku(linku)).willReturn(Optional.of(existingArticle));
                // showAiArticle 내부
                given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
                given(usersLinkuRepository.findByUserAndLinku(user, linku)).willReturn(Optional.of(usersLinku));

                // when
                aiArticleService.saveOrGetAiArticle(LINKU_ID, USER_ID);

                // then: AI 미호출
                verify(aiArticleAnalyzer, never()).analyzeByUrl(any());
            }

            @Test
            @DisplayName("AiArticle의 summary가 null이면 AI를 호출하여 새로 생성한다")
            void AiArticle_summary가_null이면_AI를_호출하여_새로_생성한다() {
                // given
                Linku linku = LinkuFixture.linku(null);
                Users user = LinkuFixture.user();
                UsersLinku usersLinku = buildUsersLinku(linku, user);
                // summary=null은 toEntity에서 들어올 수 없으나 DB 직접 삽입 등 방어 케이스
                AiArticle articleWithNullSummary = AiArticle.builder()
                        .id(1L).linku(linku).title("테스트 제목").summary(null).build();
                AiArticleResultDTO result = new AiArticleResultDTO(SUMMARY);
                AiArticle savedArticle = LinkuFixture.aiArticle(linku, SUMMARY);

                given(linkuRepository.findById(LINKU_ID)).willReturn(Optional.of(linku));
                given(aiArticleRepository.findByLinku(linku)).willReturn(Optional.of(articleWithNullSummary));
                given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
                given(usersLinkuRepository.findByUserAndLinku(user, linku)).willReturn(Optional.of(usersLinku));
                given(aiArticleAnalyzer.analyzeByUrl(any())).willReturn(result);

                // when
                aiArticleService.saveOrGetAiArticle(LINKU_ID, USER_ID);

                // then: AI 호출됨
                verify(aiArticleAnalyzer).analyzeByUrl(any());
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
