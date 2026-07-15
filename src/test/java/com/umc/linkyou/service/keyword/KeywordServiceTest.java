package com.umc.linkyou.service.keyword;

import com.umc.linkyou.apiPayload.code.status.user.UserErrorStatus;
import com.umc.linkyou.apiPayload.exception.GeneralException;
import com.umc.linkyou.domain.Users;
import com.umc.linkyou.domain.classification.Job;
import com.umc.linkyou.repository.mapping.LinkuKeywordRepository;
import com.umc.linkyou.repository.userRepository.UserRepository;
import com.umc.linkyou.web.dto.keyword.JobKeywordRankResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("KeywordService 단위 테스트")
class KeywordServiceTest {

    private static final long USER_ID = 48L;
    private static final long UNKNOWN_USER_ID = 999L;
    private static final long JOB_ID = 7L;
    private static final YearMonth BASE_MONTH = YearMonth.parse("2026-03");
    private static final LocalDateTime MONTH_START = BASE_MONTH.atDay(1).atStartOfDay();
    private static final LocalDateTime MONTH_END = BASE_MONTH.plusMonths(1).atDay(1).atStartOfDay();

    @InjectMocks private KeywordServiceImpl keywordService;

    @Mock private LinkuKeywordRepository linkuKeywordRepository;
    @Mock private KeywordUpsertService keywordUpsertService;
    @Mock private UserRepository userRepository;

    @Nested
    @DisplayName("getJobTopKeywords")
    class GetJobTopKeywords {

        @Nested
        @DisplayName("성공")
        class Success {

            @Test
            @DisplayName("직업이_설정된_유저_조회_시_같은_직업군이_해당_월에_저장한_링크의_상위_키워드를_반환한다")
            void 직업이_설정된_유저_조회_시_같은_직업군이_해당_월에_저장한_링크의_상위_키워드를_반환한다() {
                Job job = Job.builder().id(JOB_ID).name("개발자").build();
                Users user = Users.builder().id(USER_ID).job(job).build();
                given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
                given(linkuKeywordRepository.findTopKeywordNamesByJobIdAndPeriod(eq(JOB_ID), eq(MONTH_START), eq(MONTH_END), any()))
                        .willReturn(List.<Object[]>of(new Object[]{"스프링", 15L}));

                List<JobKeywordRankResponse> result = keywordService.getJobTopKeywords(USER_ID, BASE_MONTH, 15);

                assertThat(result).hasSize(1);
                assertThat(result.get(0).getName()).isEqualTo("스프링");
                assertThat(result.get(0).getCount()).isEqualTo(15L);
            }

            @Test
            @DisplayName("같은_직업군이_해당_월에_저장한_링크의_키워드_집계가_없을_시_빈_목록을_반환한다")
            void 같은_직업군이_해당_월에_저장한_링크의_키워드_집계가_없을_시_빈_목록을_반환한다() {
                Job job = Job.builder().id(JOB_ID).name("개발자").build();
                Users user = Users.builder().id(USER_ID).job(job).build();
                given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
                given(linkuKeywordRepository.findTopKeywordNamesByJobIdAndPeriod(eq(JOB_ID), eq(MONTH_START), eq(MONTH_END), any()))
                        .willReturn(List.of());

                List<JobKeywordRankResponse> result = keywordService.getJobTopKeywords(USER_ID, BASE_MONTH, 15);

                assertThat(result).isEmpty();
            }
        }

        @Nested
        @DisplayName("실패")
        class Failure {

            @Test
            @DisplayName("존재하지_않는_userId로_호출_시_USER_NOT_FOUND_예외를_던진다")
            void 존재하지_않는_userId로_호출_시_USER_NOT_FOUND_예외를_던진다() {
                given(userRepository.findById(UNKNOWN_USER_ID)).willReturn(Optional.empty());

                assertThatThrownBy(() -> keywordService.getJobTopKeywords(UNKNOWN_USER_ID, BASE_MONTH, 15))
                        .isInstanceOf(GeneralException.class)
                        .satisfies(ex -> assertThat(((GeneralException) ex).getCode())
                                .isEqualTo(UserErrorStatus._USER_NOT_FOUND));
            }

            @Test
            @DisplayName("직업이_설정되지_않은_유저가_호출_시_JOB_NOT_SET_예외를_던진다")
            void 직업이_설정되지_않은_유저가_호출_시_JOB_NOT_SET_예외를_던진다() {
                given(userRepository.findById(USER_ID))
                        .willReturn(Optional.of(Users.builder().id(USER_ID).job(null).build()));

                assertThatThrownBy(() -> keywordService.getJobTopKeywords(USER_ID, BASE_MONTH, 15))
                        .isInstanceOf(GeneralException.class)
                        .satisfies(ex -> assertThat(((GeneralException) ex).getCode())
                                .isEqualTo(UserErrorStatus._JOB_NOT_SET));
            }
        }
    }
}
