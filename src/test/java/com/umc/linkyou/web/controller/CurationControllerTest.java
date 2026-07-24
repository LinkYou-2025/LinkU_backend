package com.umc.linkyou.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.linkyou.apiPayload.code.status.curation.CurationErrorStatus;
import com.umc.linkyou.apiPayload.exception.GeneralException;
import com.umc.linkyou.config.common.WebConfig;
import com.umc.linkyou.jwt.AccessTokenBlackListManager;
import com.umc.linkyou.jwt.CurrentUserArgumentResolver;
import com.umc.linkyou.jwt.JwtTokenProvider;
import com.umc.linkyou.jwt.SecurityErrorResponseWriter;
import com.umc.linkyou.service.curation.CurationService;
import com.umc.linkyou.service.curation.recommend.CurationRecommendBuilderService;
import com.umc.linkyou.support.security.TestSecurityConfig;
import com.umc.linkyou.support.security.WithCustomUser;
import com.umc.linkyou.web.dto.curation.CurationDetailResponse;
import com.umc.linkyou.web.dto.curation.CurationLatestResponse;
import com.umc.linkyou.web.dto.curation.CurationListResponse;
import com.umc.linkyou.web.dto.curation.CurationSectionResponse;
import com.umc.linkyou.web.dto.curation.RecommendedLinkResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Optional;

import static com.umc.linkyou.support.util.ApiResponseTestUtils.readResult;
import static com.umc.linkyou.support.util.ApiResponseTestUtils.readResultList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CurationController.class)
@Import({WebConfig.class, CurrentUserArgumentResolver.class, TestSecurityConfig.class})
@DisplayName("CurationController 테스트")
class CurationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CurationService curationService;

    @MockitoBean
    private CurationRecommendBuilderService curationRecommendBuilderService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private AccessTokenBlackListManager accessTokenBlackListManager;

    @MockitoBean
    private SecurityErrorResponseWriter securityErrorResponseWriter;

    private static final Long CURATION_ID = 1L;
    private static final String MONTH = "2026-05";

    @Nested
    @DisplayName("GET /api/v1/curations/sections - 섹션 정보 조회")
    class GetSectionInfo {

        @Nested
        @DisplayName("성공")
        class Success {
            @Test
            @DisplayName("인증된 사용자가 month를 지정하면 해당 월 섹션 목록을 반환한다")
            @WithCustomUser
            void 월지정_시_섹션목록을_반환한다() throws Exception {
                CurationSectionResponse section = CurationSectionResponse.builder()
                        .section(1).title("제목").description("설명").imageUrl("image-url").build();

                given(curationService.getCurationSections(MONTH)).willReturn(List.of(section));

                MvcResult result = mockMvc.perform(get("/api/v1/curations/sections").param("month", MONTH))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.isSuccess").value(true))
                        .andExpect(jsonPath("$.code").value("COMMON200"))
                        .andReturn();

                List<CurationSectionResponse> sections = readResultList(result, objectMapper, CurationSectionResponse.class);
                assertThat(sections).hasSize(1);
                assertThat(sections.get(0).getSection()).isEqualTo(1);
                assertThat(sections.get(0).getTitle()).isEqualTo("제목");
            }
        }

        @Nested
        @DisplayName("실패")
        class Failure {
            @Test
            @DisplayName("인증되지 않은 요청은 401을 반환한다")
            void 인증되지_않으면_401을_반환한다() throws Exception {
                mockMvc.perform(get("/api/v1/curations/sections").param("month", MONTH))
                        .andExpect(status().isUnauthorized());
            }
        }
    }

    @Nested
    @DisplayName("GET /api/v1/curations/history - 연도별 큐레이션 히스토리 조회")
    class GetMyCurationList {

        @Nested
        @DisplayName("성공")
        class Success {
            @Test
            @DisplayName("인증된 사용자가 연도를 지정하면 큐레이션 목록을 반환한다")
            @WithCustomUser(userId = 1L)
            void 연도지정_시_큐레이션목록을_반환한다() throws Exception {
                CurationListResponse item = CurationListResponse.builder()
                        .curationId(CURATION_ID).month(MONTH).thumbnailUrl("url").build();

                given(curationService.getCurationList(eq(1L), eq(2026))).willReturn(List.of(item));

                MvcResult result = mockMvc.perform(get("/api/v1/curations/history").param("year", "2026"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.isSuccess").value(true))
                        .andReturn();

                List<CurationListResponse> curations = readResultList(result, objectMapper, CurationListResponse.class);
                assertThat(curations).hasSize(1);
                assertThat(curations.get(0).getCurationId()).isEqualTo(CURATION_ID);
                assertThat(curations.get(0).getMonth()).isEqualTo(MONTH);
            }
        }

        @Nested
        @DisplayName("실패")
        class Failure {
            @Test
            @DisplayName("2025년 미만이면 예외 응답을 반환한다")
            @WithCustomUser(userId = 1L)
            void 연도범위벗어나면_예외응답을_반환한다() throws Exception {
                given(curationService.getCurationList(eq(1L), eq(2024)))
                        .willThrow(new GeneralException(CurationErrorStatus._CURATION_INVALID_YEAR));

                mockMvc.perform(get("/api/v1/curations/history").param("year", "2024"))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.isSuccess").value(false))
                        .andExpect(jsonPath("$.code").value(CurationErrorStatus._CURATION_INVALID_YEAR.getCode()));
            }

            @Test
            @DisplayName("인증되지 않은 요청은 401을 반환한다")
            void 인증되지_않으면_401을_반환한다() throws Exception {
                mockMvc.perform(get("/api/v1/curations/history").param("year", "2026"))
                        .andExpect(status().isUnauthorized());
            }
        }
    }

    @Nested
    @DisplayName("GET /api/v1/curations/latest - 최근 큐레이션 조회")
    class GetLatestCuration {

        @Nested
        @DisplayName("성공")
        class Success {
            @Test
            @DisplayName("최근 큐레이션이 있으면 200과 정보를 반환한다")
            @WithCustomUser(userId = 1L)
            void 최근큐레이션존재_시_200을_반환한다() throws Exception {
                CurationLatestResponse response = CurationLatestResponse.builder()
                        .curationId(CURATION_ID).month(MONTH).thumbnailUrl("url").build();

                given(curationService.getLatestCuration(1L)).willReturn(Optional.of(response));

                MvcResult result = mockMvc.perform(get("/api/v1/curations/latest"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.isSuccess").value(true))
                        .andReturn();

                CurationLatestResponse curation = readResult(result, objectMapper, CurationLatestResponse.class);
                assertThat(curation.getCurationId()).isEqualTo(CURATION_ID);
                assertThat(curation.getMonth()).isEqualTo(MONTH);
            }

            @Test
            @DisplayName("최근 큐레이션이 없으면 204를 반환한다")
            @WithCustomUser(userId = 1L)
            void 최근큐레이션없음_시_204를_반환한다() throws Exception {
                given(curationService.getLatestCuration(1L)).willReturn(Optional.empty());

                mockMvc.perform(get("/api/v1/curations/latest"))
                        .andExpect(status().isNoContent());
            }
        }
    }

    @Nested
    @DisplayName("GET /api/v1/curations/detail/{curationId} - 큐레이션 상세 조회")
    class GetCurationDetail {

        @Nested
        @DisplayName("성공")
        class Success {
            @Test
            @DisplayName("본인 큐레이션이면 상세 정보를 반환한다")
            @WithCustomUser(userId = 1L)
            void 본인큐레이션_조회_시_상세정보를_반환한다() throws Exception {
                CurationDetailResponse response = CurationDetailResponse.builder()
                        .curationId(CURATION_ID).month(MONTH)
                        .headerMent("header").footerMent("footer").mentReady(true).build();

                given(curationService.getCurationDetail(1L, CURATION_ID)).willReturn(response);

                MvcResult result = mockMvc.perform(get("/api/v1/curations/detail/{curationId}", CURATION_ID))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.isSuccess").value(true))
                        .andReturn();

                CurationDetailResponse detail = readResult(result, objectMapper, CurationDetailResponse.class);
                assertThat(detail.getMonth()).isEqualTo(MONTH);
                assertThat(detail.isMentReady()).isTrue();
            }
        }

        @Nested
        @DisplayName("실패")
        class Failure {
            @Test
            @DisplayName("큐레이션이 없으면 404와 에러코드를 반환한다")
            @WithCustomUser(userId = 1L)
            void 큐레이션이_없으면_404를_반환한다() throws Exception {
                given(curationService.getCurationDetail(1L, CURATION_ID))
                        .willThrow(new GeneralException(CurationErrorStatus._CURATION_NOT_FOUND));

                mockMvc.perform(get("/api/v1/curations/detail/{curationId}", CURATION_ID))
                        .andExpect(status().isNotFound())
                        .andExpect(jsonPath("$.isSuccess").value(false))
                        .andExpect(jsonPath("$.code").value(CurationErrorStatus._CURATION_NOT_FOUND.getCode()));
            }

            @Test
            @DisplayName("타인의 큐레이션이면 403과 에러코드를 반환한다")
            @WithCustomUser(userId = 1L)
            void 타인의_큐레이션이면_403을_반환한다() throws Exception {
                given(curationService.getCurationDetail(1L, CURATION_ID))
                        .willThrow(new GeneralException(CurationErrorStatus._CURATION_FORBIDDEN));

                mockMvc.perform(get("/api/v1/curations/detail/{curationId}", CURATION_ID))
                        .andExpect(status().isForbidden())
                        .andExpect(jsonPath("$.isSuccess").value(false))
                        .andExpect(jsonPath("$.code").value(CurationErrorStatus._CURATION_FORBIDDEN.getCode()));
            }

            @Test
            @DisplayName("인증되지 않은 요청은 401을 반환한다")
            void 인증되지_않으면_401을_반환한다() throws Exception {
                mockMvc.perform(get("/api/v1/curations/detail/{curationId}", CURATION_ID))
                        .andExpect(status().isUnauthorized());
            }
        }
    }

    @Nested
    @DisplayName("GET /api/v1/curations/recommend-links - 큐레이션 기반 링크 추천")
    class GetRecommendedLinks {

        @Nested
        @DisplayName("성공")
        class Success {
            @Test
            @DisplayName("추천 링크가 있으면 목록을 반환한다")
            @WithCustomUser(userId = 1L)
            void 추천링크존재_시_목록을_반환한다() throws Exception {
                RecommendedLinkResponse link = RecommendedLinkResponse.builder()
                        .userLinkuId(10L).title("링크").url("https://example.com").build();

                given(curationRecommendBuilderService.buildRecommendedLinks(eq(1L), eq(CURATION_ID)))
                        .willReturn(List.of(link));

                MvcResult result = mockMvc.perform(get("/api/v1/curations/recommend-links").param("curationId", String.valueOf(CURATION_ID)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.isSuccess").value(true))
                        .andReturn();

                List<RecommendedLinkResponse> links = readResultList(result, objectMapper, RecommendedLinkResponse.class);
                assertThat(links).hasSize(1);
                assertThat(links.get(0).getUserLinkuId()).isEqualTo(10L);
                assertThat(links.get(0).getTitle()).isEqualTo("링크");
            }
        }

        @Nested
        @DisplayName("실패")
        class Failure {
            @Test
            @DisplayName("큐레이션이 없으면 404와 에러코드를 반환한다")
            @WithCustomUser(userId = 1L)
            void 큐레이션이_없으면_404를_반환한다() throws Exception {
                given(curationRecommendBuilderService.buildRecommendedLinks(eq(1L), eq(CURATION_ID)))
                        .willThrow(new GeneralException(CurationErrorStatus._CURATION_NOT_FOUND));

                mockMvc.perform(get("/api/v1/curations/recommend-links").param("curationId", String.valueOf(CURATION_ID)))
                        .andExpect(status().isNotFound())
                        .andExpect(jsonPath("$.isSuccess").value(false))
                        .andExpect(jsonPath("$.code").value(CurationErrorStatus._CURATION_NOT_FOUND.getCode()));
            }

            @Test
            @DisplayName("인증되지 않은 요청은 401을 반환한다")
            void 인증되지_않으면_401을_반환한다() throws Exception {
                mockMvc.perform(get("/api/v1/curations/recommend-links").param("curationId", String.valueOf(CURATION_ID)))
                        .andExpect(status().isUnauthorized());
            }
        }
    }
}
