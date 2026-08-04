package com.umc.linkyou.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.linkyou.apiPayload.code.status.user.UserErrorStatus;
import com.umc.linkyou.apiPayload.exception.GeneralException;
import com.umc.linkyou.config.common.WebConfig;
import com.umc.linkyou.jwt.AccessTokenBlackListManager;
import com.umc.linkyou.jwt.CurrentUserArgumentResolver;
import com.umc.linkyou.jwt.JwtTokenProvider;
import com.umc.linkyou.jwt.SecurityErrorResponseWriter;
import com.umc.linkyou.service.tag.TagService;
import com.umc.linkyou.support.security.TestSecurityConfig;
import com.umc.linkyou.support.security.WithCustomUser;
import com.umc.linkyou.web.dto.tag.MyTagRankResponse;
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

@WebMvcTest(TagController.class)
@Import({WebConfig.class, CurrentUserArgumentResolver.class, TestSecurityConfig.class})
@DisplayName("TagController 테스트")
class TagControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TagService tagService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private AccessTokenBlackListManager accessTokenBlackListManager;

    @MockitoBean
    private SecurityErrorResponseWriter securityErrorResponseWriter;

    private static final YearMonth MONTH = YearMonth.parse("2026-03");

    @Nested
    @DisplayName("GET /api/v1/tags/my - 내 월별 상위 태그 조회")
    class GetMyTopTags {

        @Nested
        @DisplayName("성공")
        class Success {
            @Test
            @DisplayName("정상 요청 시 상위 태그 목록을 반환한다")
            @WithCustomUser(userId = 1L)
            void 정상_요청_시_상위태그목록을_반환한다() throws Exception {
                MyTagRankResponse item = MyTagRankResponse.builder()
                        .name("즐거움").percent(50).build();

                given(tagService.getMyTopTags(1L, MONTH, 3)).willReturn(List.of(item));

                MvcResult result = mockMvc.perform(get("/api/v1/tags/my").param("month", "2026-03"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.isSuccess").value(true))
                        .andReturn();

                List<MyTagRankResponse> tags = readResultList(result, objectMapper, MyTagRankResponse.class);
                assertThat(tags).hasSize(1);
                assertThat(tags.get(0).getName()).isEqualTo("즐거움");
                assertThat(tags.get(0).getPercent()).isEqualTo(50);
            }
        }

        @Nested
        @DisplayName("실패")
        class Failure {
            @Test
            @DisplayName("존재하지 않는 유저면 404와 에러코드를 반환한다")
            @WithCustomUser(userId = 1L)
            void 유저가_없으면_404를_반환한다() throws Exception {
                given(tagService.getMyTopTags(1L, MONTH, 3))
                        .willThrow(new GeneralException(UserErrorStatus._USER_NOT_FOUND));

                mockMvc.perform(get("/api/v1/tags/my").param("month", "2026-03"))
                        .andExpect(status().isNotFound())
                        .andExpect(jsonPath("$.isSuccess").value(false))
                        .andExpect(jsonPath("$.code").value(UserErrorStatus._USER_NOT_FOUND.getCode()));
            }

            @Test
            @DisplayName("인증되지 않은 요청은 401을 반환한다")
            void 인증되지_않으면_401을_반환한다() throws Exception {
                mockMvc.perform(get("/api/v1/tags/my").param("month", "2026-03"))
                        .andExpect(status().isUnauthorized());
            }
        }
    }
}
