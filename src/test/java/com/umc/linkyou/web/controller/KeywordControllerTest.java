package com.umc.linkyou.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.linkyou.apiPayload.code.status.user.UserErrorStatus;
import com.umc.linkyou.apiPayload.exception.GeneralException;
import com.umc.linkyou.config.common.WebConfig;
import com.umc.linkyou.jwt.AccessTokenBlackListManager;
import com.umc.linkyou.jwt.CurrentUserArgumentResolver;
import com.umc.linkyou.jwt.JwtTokenProvider;
import com.umc.linkyou.jwt.SecurityErrorResponseWriter;
import com.umc.linkyou.service.keyword.KeywordService;
import com.umc.linkyou.support.security.TestSecurityConfig;
import com.umc.linkyou.support.security.WithCustomUser;
import com.umc.linkyou.web.dto.keyword.JobKeywordRankResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.YearMonth;
import java.util.List;

import static com.umc.linkyou.support.util.ApiResponseTestUtils.readResultList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(KeywordController.class)
@Import({WebConfig.class, CurrentUserArgumentResolver.class, TestSecurityConfig.class})
@DisplayName("KeywordController 테스트")
class KeywordControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private KeywordService keywordService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private AccessTokenBlackListManager accessTokenBlackListManager;

    @MockitoBean
    private SecurityErrorResponseWriter securityErrorResponseWriter;

    private static final YearMonth MONTH = YearMonth.parse("2026-03");

    @Nested
    @DisplayName("GET /api/v1/keywords/job - 같은 직업군 상위 키워드 조회")
    class GetJobTopKeywords {

        @Nested
        @DisplayName("성공")
        class Success {
            @Test
            @DisplayName("정상 요청 시 상위 키워드 목록을 반환한다")
            @WithCustomUser(userId = 1L)
            void 정상_요청_시_상위키워드목록을_반환한다() throws Exception {
                JobKeywordRankResponse item = JobKeywordRankResponse.builder()
                        .name("스프링").count(15L).build();

                given(keywordService.getJobTopKeywords(1L, MONTH, 10)).willReturn(List.of(item));

                MvcResult result = mockMvc.perform(get("/api/v1/keywords/job").param("month", "2026-03"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.isSuccess").value(true))
                        .andReturn();

                List<JobKeywordRankResponse> keywords = readResultList(result, objectMapper, JobKeywordRankResponse.class);
                assertThat(keywords).hasSize(1);
                assertThat(keywords.get(0).getName()).isEqualTo("스프링");
                assertThat(keywords.get(0).getCount()).isEqualTo(15L);
            }
        }

        @Nested
        @DisplayName("실패")
        class Failure {
            @Test
            @DisplayName("존재하지 않는 유저면 404와 에러코드를 반환한다")
            @WithCustomUser(userId = 1L)
            void 유저가_없으면_404를_반환한다() throws Exception {
                given(keywordService.getJobTopKeywords(1L, MONTH, 10))
                        .willThrow(new GeneralException(UserErrorStatus._USER_NOT_FOUND));

                mockMvc.perform(get("/api/v1/keywords/job").param("month", "2026-03"))
                        .andExpect(status().isNotFound())
                        .andExpect(jsonPath("$.isSuccess").value(false))
                        .andExpect(jsonPath("$.code").value(UserErrorStatus._USER_NOT_FOUND.getCode()));
            }

            @Test
            @DisplayName("직업이 설정되지 않았으면 400과 에러코드를 반환한다")
            @WithCustomUser(userId = 1L)
            void 직업이_없으면_400을_반환한다() throws Exception {
                given(keywordService.getJobTopKeywords(1L, MONTH, 10))
                        .willThrow(new GeneralException(UserErrorStatus._JOB_NOT_SET));

                mockMvc.perform(get("/api/v1/keywords/job").param("month", "2026-03"))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.isSuccess").value(false))
                        .andExpect(jsonPath("$.code").value(UserErrorStatus._JOB_NOT_SET.getCode()));
            }

            @Test
            @DisplayName("인증되지 않은 요청은 401을 반환한다")
            void 인증되지_않으면_401을_반환한다() throws Exception {
                mockMvc.perform(get("/api/v1/keywords/job").param("month", "2026-03"))
                        .andExpect(status().isUnauthorized());
            }
        }
    }
}
