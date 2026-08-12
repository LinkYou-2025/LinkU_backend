package com.umc.linkyou.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.linkyou.apiPayload.code.status.CommonErrorStatus;
import com.umc.linkyou.apiPayload.code.status.ErrorStatus;
import com.umc.linkyou.apiPayload.code.status.aiarticle.AiArticleErrorStatus;
import com.umc.linkyou.apiPayload.exception.GeneralException;
import com.umc.linkyou.config.common.WebConfig;
import com.umc.linkyou.jwt.AccessTokenBlackListManager;
import com.umc.linkyou.jwt.CurrentUserArgumentResolver;
import com.umc.linkyou.jwt.JwtTokenProvider;
import com.umc.linkyou.repository.aiArticleRepository.AiArticleRepository;
import com.umc.linkyou.service.AiArticleService;
import com.umc.linkyou.support.security.TestSecurityConfig;
import com.umc.linkyou.support.security.WithCustomUser;
import com.umc.linkyou.web.dto.AiArticleResponseDTO;
import com.umc.linkyou.web.dto.linku.LinkuResponseDTO;
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

import static com.umc.linkyou.support.util.ApiResponseTestUtils.readResult;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AiArticleController.class)
@Import({WebConfig.class, CurrentUserArgumentResolver.class, TestSecurityConfig.class})
@DisplayName("AiArticleController 테스트")
class AiArticleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AiArticleService aiArticleService;

    @MockitoBean
    private AiArticleRepository aiArticleRepository;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private AccessTokenBlackListManager accessTokenBlackListManager;

    private static final Long TEST_USER_ID = 1L;
    private static final Long TEST_LINKU_ID = 101L;

    @Nested
    @DisplayName("GET /api/v1/aiarticle/{linkuid} - AI 요약 조회")
    class GetAiArticle {

        @Nested
        @DisplayName("성공")
        class Success {

            @Test
            @DisplayName("생성이 완료된 링크를 조회하면 status=DONE과 summary를 반환한다")
            @WithCustomUser(userId = 1L)
            void 생성완료된_링크_조회시_DONE과_summary를_반환한다() throws Exception {
                AiArticleResponseDTO.AiArticleResultDTO response = AiArticleResponseDTO.AiArticleResultDTO.builder()
                        .id(1L)
                        .linkuId(TEST_LINKU_ID)
                        .status("DONE")
                        .summary("요약된 내용입니다.")
                        .tags("스프링, 백엔드")
                        .title("테스트 제목")
                        .build();

                given(aiArticleService.getAiArticle(TEST_LINKU_ID, TEST_USER_ID)).willReturn(response);

                MvcResult result = mockMvc.perform(get("/api/v1/aiarticle/{linkuid}", TEST_LINKU_ID))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.isSuccess").value(true))
                        .andReturn();

                AiArticleResponseDTO.AiArticleResultDTO body =
                        readResult(result, objectMapper, AiArticleResponseDTO.AiArticleResultDTO.class);
                assertThat(body.getStatus()).isEqualTo("DONE");
                assertThat(body.getSummary()).isEqualTo("요약된 내용입니다.");
            }

            @Test
            @DisplayName("생성이 진행 중인 링크를 조회하면 status=PENDING과 null summary를 반환한다")
            @WithCustomUser(userId = 1L)
            void 생성중인_링크_조회시_PENDING과_null_summary를_반환한다() throws Exception {
                AiArticleResponseDTO.AiArticleResultDTO response = AiArticleResponseDTO.AiArticleResultDTO.builder()
                        .id(1L)
                        .linkuId(TEST_LINKU_ID)
                        .status("PENDING")
                        .build();

                given(aiArticleService.getAiArticle(TEST_LINKU_ID, TEST_USER_ID)).willReturn(response);

                MvcResult result = mockMvc.perform(get("/api/v1/aiarticle/{linkuid}", TEST_LINKU_ID))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.isSuccess").value(true))
                        .andReturn();

                AiArticleResponseDTO.AiArticleResultDTO body =
                        readResult(result, objectMapper, AiArticleResponseDTO.AiArticleResultDTO.class);
                assertThat(body.getStatus()).isEqualTo("PENDING");
                assertThat(body.getSummary()).isNull();
            }
        }

        @Nested
        @DisplayName("실패")
        class Failure {

            @Test
            @DisplayName("아직 생성 요청조차 없었던 링크면 404를 반환한다")
            @WithCustomUser(userId = 1L)
            void 레코드가_없으면_404를_반환한다() throws Exception {
                given(aiArticleService.getAiArticle(TEST_LINKU_ID, TEST_USER_ID))
                        .willThrow(new GeneralException(AiArticleErrorStatus._AI_ARTICLE_NOT_FOUND));

                mockMvc.perform(get("/api/v1/aiarticle/{linkuid}", TEST_LINKU_ID))
                        .andExpect(status().isNotFound())
                        .andExpect(jsonPath("$.isSuccess").value(false))
                        .andExpect(jsonPath("$.code").value(AiArticleErrorStatus._AI_ARTICLE_NOT_FOUND.getCode()));
            }

            @Test
            @DisplayName("인증되지 않으면 401을 반환한다")
            void 인증되지_않으면_401을_반환한다() throws Exception {
                mockMvc.perform(get("/api/v1/aiarticle/{linkuid}", TEST_LINKU_ID))
                        .andExpect(status().isUnauthorized());
            }
        }
    }

    @Nested
    @DisplayName("POST /api/v1/aiarticle/{linkuid} - AI 요약 생성 요청")
    class CreateAiArticle {

        @Nested
        @DisplayName("성공")
        class Success {

            @Test
            @DisplayName("생성 요청이 접수되면 status=PENDING을 즉시 반환한다")
            @WithCustomUser(userId = 1L)
            void 생성_요청이_접수되면_PENDING을_즉시_반환한다() throws Exception {
                AiArticleResponseDTO.AiArticleResultDTO response = AiArticleResponseDTO.AiArticleResultDTO.builder()
                        .id(1L)
                        .linkuId(TEST_LINKU_ID)
                        .status("PENDING")
                        .build();

                given(aiArticleService.createAiArticle(TEST_LINKU_ID, TEST_USER_ID)).willReturn(response);

                MvcResult result = mockMvc.perform(post("/api/v1/aiarticle/{linkuid}", TEST_LINKU_ID))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.isSuccess").value(true))
                        .andReturn();

                AiArticleResponseDTO.AiArticleResultDTO body =
                        readResult(result, objectMapper, AiArticleResponseDTO.AiArticleResultDTO.class);
                assertThat(body.getStatus()).isEqualTo("PENDING");
                assertThat(body.getSummary()).isNull();
            }
        }

        @Nested
        @DisplayName("실패")
        class Failure {

            @Test
            @DisplayName("이미 생성이 완료된 링크면 409를 반환한다")
            @WithCustomUser(userId = 1L)
            void 이미_DONE이면_409를_반환한다() throws Exception {
                given(aiArticleService.createAiArticle(TEST_LINKU_ID, TEST_USER_ID))
                        .willThrow(new GeneralException(AiArticleErrorStatus._DUPLICATE_AI_ARTICLE));

                mockMvc.perform(post("/api/v1/aiarticle/{linkuid}", TEST_LINKU_ID))
                        .andExpect(status().isConflict())
                        .andExpect(jsonPath("$.isSuccess").value(false))
                        .andExpect(jsonPath("$.code").value(AiArticleErrorStatus._DUPLICATE_AI_ARTICLE.getCode()));
            }

            @Test
            @DisplayName("이미 생성이 진행 중인 링크면 409를 반환한다")
            @WithCustomUser(userId = 1L)
            void 이미_PENDING이면_409를_반환한다() throws Exception {
                given(aiArticleService.createAiArticle(TEST_LINKU_ID, TEST_USER_ID))
                        .willThrow(new GeneralException(AiArticleErrorStatus._AI_ARTICLE_GENERATING));

                mockMvc.perform(post("/api/v1/aiarticle/{linkuid}", TEST_LINKU_ID))
                        .andExpect(status().isConflict())
                        .andExpect(jsonPath("$.isSuccess").value(false))
                        .andExpect(jsonPath("$.code").value(AiArticleErrorStatus._AI_ARTICLE_GENERATING.getCode()));
            }

            @Test
            @DisplayName("인증되지 않으면 401을 반환한다")
            void 인증되지_않으면_401을_반환한다() throws Exception {
                mockMvc.perform(post("/api/v1/aiarticle/{linkuid}", TEST_LINKU_ID))
                        .andExpect(status().isUnauthorized());
            }
        }
    }

    @Nested
    @DisplayName("GET /api/v1/aiarticle - 카테고리별 AI 아티클 조회")
    class GetMyAiArticlesByCategory {

        @Nested
        @DisplayName("성공")
        class Success {

            @Test
            @DisplayName("특정 카테고리로 조회하면 커서 기반 페이징 결과를 반환한다")
            @WithCustomUser(userId = 1L)
            void 특정_카테고리로_조회하면_커서_기반_페이징_결과를_반환한다() throws Exception {
                Long categoryId = 1L;

                LinkuResponseDTO.AiArticleSummaryDTO article1 = LinkuResponseDTO.AiArticleSummaryDTO.builder()
                        .linkuId(101L)
                        .linku("https://example.com/article")
                        .emotionId(2L)
                        .domain("naver")
                        .domainImageUrl("https://img1.daumcdn.net/thumb/R800x0")
                        .title("첫 번째 AI 제목")
                        .linkuImageUrl("https://img1.daumcdn.net/thumb/R800x0")
                        .categoryId(1L)
                        .categoryName("어학")
                        .build();

                LinkuResponseDTO.LinkuSliceResultDTO mockSlice = LinkuResponseDTO.LinkuSliceResultDTO.builder()
                        .linkuList(List.of(article1))
                        .nextCursor("1001")
                        .hasNext(true)
                        .build();

                given(aiArticleService.getMyAiArticlesByCategory(eq(TEST_USER_ID), eq(categoryId), any(), anyInt()))
                        .willReturn(mockSlice);

                MvcResult result = mockMvc.perform(get("/api/v1/aiarticle")
                                .param("categoryId", String.valueOf(categoryId))
                                .param("cursor", "0")
                                .param("limit", "10"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.isSuccess").value(true))
                        .andReturn();

                LinkuResponseDTO.LinkuSliceResultDTO body =
                        readResult(result, objectMapper, LinkuResponseDTO.LinkuSliceResultDTO.class);
                assertThat(body.getLinkuList()).hasSize(1);
                assertThat(body.getLinkuList().get(0).getLinkuId()).isEqualTo(101L);
                assertThat(body.getLinkuList().get(0).getLinku()).isEqualTo("https://example.com/article");
                assertThat(body.getLinkuList().get(0).getEmotionId()).isEqualTo(2L);
                assertThat(body.getLinkuList().get(0).getDomain()).isEqualTo("naver");
                assertThat(body.getLinkuList().get(0).getTitle()).isEqualTo("첫 번째 AI 제목");
                assertThat(body.getLinkuList().get(0).getCategoryId()).isEqualTo(1L);
                assertThat(body.getLinkuList().get(0).getCategoryName()).isEqualTo("어학");
                assertThat(body.getNextCursor()).isEqualTo("1001");
                assertThat(body.getHasNext()).isTrue();
            }

            @Test
            @DisplayName("결과가 없는 카테고리로 조회하면 빈 리스트와 null 커서를 반환한다")
            @WithCustomUser(userId = 1L)
            void 결과가_없는_카테고리로_조회하면_빈_리스트와_null_커서를_반환한다() throws Exception {
                Long emptyCategoryId = 99L;

                LinkuResponseDTO.LinkuSliceResultDTO emptySlice = LinkuResponseDTO.LinkuSliceResultDTO.builder()
                        .linkuList(List.of())
                        .nextCursor(null)
                        .hasNext(false)
                        .build();

                given(aiArticleService.getMyAiArticlesByCategory(eq(TEST_USER_ID), eq(emptyCategoryId), any(), anyInt()))
                        .willReturn(emptySlice);

                MvcResult result = mockMvc.perform(get("/api/v1/aiarticle")
                                .param("categoryId", String.valueOf(emptyCategoryId)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.isSuccess").value(true))
                        .andReturn();

                LinkuResponseDTO.LinkuSliceResultDTO body =
                        readResult(result, objectMapper, LinkuResponseDTO.LinkuSliceResultDTO.class);
                assertThat(body.getLinkuList()).isEmpty();
                assertThat(body.getHasNext()).isFalse();
                assertThat(body.getNextCursor()).isNull();
            }

            @Test
            @DisplayName("존재하지 않는 카테고리로 조회해도 서비스가 빈 결과를 반환하면 정상 응답한다")
            @WithCustomUser(userId = 1L)
            void 존재하지_않는_카테고리로_조회해도_서비스가_빈_결과를_반환하면_정상_응답한다() throws Exception {
                Long notFoundCategoryId = 999L;

                LinkuResponseDTO.LinkuSliceResultDTO emptySlice = LinkuResponseDTO.LinkuSliceResultDTO.builder()
                        .linkuList(List.of())
                        .nextCursor(null)
                        .hasNext(false)
                        .build();

                given(aiArticleService.getMyAiArticlesByCategory(eq(TEST_USER_ID), eq(notFoundCategoryId), any(), anyInt()))
                        .willReturn(emptySlice);

                MvcResult result = mockMvc.perform(get("/api/v1/aiarticle")
                                .param("categoryId", String.valueOf(notFoundCategoryId))
                                .param("cursor", "0")
                                .param("limit", "10"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.isSuccess").value(true))
                        .andReturn();

                LinkuResponseDTO.LinkuSliceResultDTO body =
                        readResult(result, objectMapper, LinkuResponseDTO.LinkuSliceResultDTO.class);
                assertThat(body.getLinkuList()).isEmpty();
                assertThat(body.getHasNext()).isFalse();
            }

            @Test
            @DisplayName("categoryId를 생략하면 전체 카테고리 결과를 반환한다")
            @WithCustomUser(userId = 1L)
            void categoryId를_생략하면_전체_카테고리_결과를_반환한다() throws Exception {
                LinkuResponseDTO.AiArticleSummaryDTO articleFromCategoryA = LinkuResponseDTO.AiArticleSummaryDTO.builder()
                        .linkuId(201L)
                        .linku("https://example.com/a")
                        .emotionId(1L)
                        .domain("naver")
                        .domainImageUrl("https://img1.daumcdn.net/thumb/R800x0")
                        .title("카테고리 A 글")
                        .linkuImageUrl("https://img1.daumcdn.net/thumb/R800x0")
                        .categoryId(1L)
                        .categoryName("어학")
                        .build();
                LinkuResponseDTO.AiArticleSummaryDTO articleFromCategoryB = LinkuResponseDTO.AiArticleSummaryDTO.builder()
                        .linkuId(202L)
                        .linku("https://example.com/b")
                        .emotionId(2L)
                        .domain("google")
                        .domainImageUrl("https://img1.daumcdn.net/thumb/R800x0")
                        .title("카테고리 B 글")
                        .linkuImageUrl("https://img1.daumcdn.net/thumb/R800x0")
                        .categoryId(2L)
                        .categoryName("뉴스")
                        .build();

                LinkuResponseDTO.LinkuSliceResultDTO mockSlice = LinkuResponseDTO.LinkuSliceResultDTO.builder()
                        .linkuList(List.of(articleFromCategoryA, articleFromCategoryB))
                        .nextCursor(null)
                        .hasNext(false)
                        .build();

                given(aiArticleService.getMyAiArticlesByCategory(eq(TEST_USER_ID), isNull(), any(), anyInt()))
                        .willReturn(mockSlice);

                MvcResult result = mockMvc.perform(get("/api/v1/aiarticle").param("limit", "10"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.isSuccess").value(true))
                        .andReturn();

                LinkuResponseDTO.LinkuSliceResultDTO body =
                        readResult(result, objectMapper, LinkuResponseDTO.LinkuSliceResultDTO.class);
                assertThat(body.getLinkuList()).hasSize(2);
                assertThat(body.getLinkuList().get(0).getLinkuId()).isEqualTo(201L);
                assertThat(body.getLinkuList().get(0).getCategoryId()).isEqualTo(1L);
                assertThat(body.getLinkuList().get(1).getLinkuId()).isEqualTo(202L);
                assertThat(body.getLinkuList().get(1).getCategoryId()).isEqualTo(2L);
            }
        }

        @Nested
        @DisplayName("실패")
        class Failure {

            @Test
            @DisplayName("categoryId가 문자열이면 400을 반환한다")
            @WithCustomUser(userId = 1L)
            void categoryId가_문자열이면_400을_반환한다() throws Exception {
                mockMvc.perform(get("/api/v1/aiarticle")
                                .param("categoryId", "invalid"))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.isSuccess").value(false))
                        .andExpect(jsonPath("$.code").value(CommonErrorStatus._BAD_REQUEST.getCode()));
            }

            @Test
            @DisplayName("서비스에서 예외가 발생하면 에러 응답을 반환한다")
            @WithCustomUser(userId = 1L)
            void 서비스에서_예외가_발생하면_에러_응답을_반환한다() throws Exception {
                given(aiArticleService.getMyAiArticlesByCategory(any(), any(), any(), anyInt()))
                        .willThrow(new GeneralException(ErrorStatus._INTERNAL_SERVER_ERROR));

                mockMvc.perform(get("/api/v1/aiarticle")
                                .param("categoryId", "1")
                                .param("cursor", "0")
                                .param("limit", "10"))
                        .andExpect(status().isInternalServerError())
                        .andExpect(jsonPath("$.isSuccess").value(false))
                        .andExpect(jsonPath("$.code").value(ErrorStatus._INTERNAL_SERVER_ERROR.getCode()));
            }

            @Test
            @DisplayName("인증되지 않으면 401을 반환한다")
            void 인증되지_않으면_401을_반환한다() throws Exception {
                mockMvc.perform(get("/api/v1/aiarticle")
                                .param("categoryId", "1"))
                        .andExpect(status().isUnauthorized());
            }
        }
    }
}
