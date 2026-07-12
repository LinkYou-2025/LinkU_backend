package com.umc.linkyou.web.controller;

import com.umc.linkyou.apiPayload.code.status.ErrorStatus;
import com.umc.linkyou.apiPayload.exception.GeneralException;
import com.umc.linkyou.domain.enums.Role;
import com.umc.linkyou.jwt.AccessTokenBlackListManager;
import com.umc.linkyou.jwt.CustomUserDetails;
import com.umc.linkyou.jwt.JwtTokenProvider;
import com.umc.linkyou.jwt.SecurityErrorResponseWriter;
import com.umc.linkyou.config.common.WebConfig;
import com.umc.linkyou.jwt.CurrentUserArgumentResolver;
import com.umc.linkyou.repository.aiArticleRepository.AiArticleRepository;
import com.umc.linkyou.service.AiArticleService;
import com.umc.linkyou.web.dto.linku.LinkuResponseDTO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AiArticleController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({WebConfig.class, CurrentUserArgumentResolver.class})
@DisplayName("AiArticleController 테스트")
class AiArticleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AiArticleService aiArticleService;

    @MockitoBean
    private AiArticleRepository aiArticleRepository;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private AccessTokenBlackListManager accessTokenBlackListManager;

    @MockitoBean
    private SecurityErrorResponseWriter securityErrorResponseWriter;

    private static final Long TEST_USER_ID = 1L;

    @BeforeEach
    void setUp() {
        CustomUserDetails userDetails = new CustomUserDetails(TEST_USER_ID, Role.USER, "KAKAO");
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("getMyAiArticlesByCategory")
    class GetMyAiArticlesByCategory {

        @Nested
        @DisplayName("성공 케이스")
        class Success {

            @Test
            @DisplayName("특정 카테고리 ID로 조회 시 커서 기반 페이징 결과를 반환한다")
            void getMyAiArticlesByCategory_Success() throws Exception {
                // given
                Long categoryId = 1L;

                LinkuResponseDTO.AiArticleSummaryDTO article1 = LinkuResponseDTO.AiArticleSummaryDTO.builder()
                        .linkuId(101L)
                        .linku("https://example.com/article")
                        .emotionId(2L)
                        .domain("naver")
                        .domainImageUrl("https://img1.daumcdn.net/thumb/R800x0")
                        .title("첫 번째 AI 제목")
                        .linkuImageUrl("https://img1.daumcdn.net/thumb/R800x0")
                        .build();

                LinkuResponseDTO.LinkuSliceResultDTO mockSlice = LinkuResponseDTO.LinkuSliceResultDTO.builder()
                        .linkuList(List.of(article1))
                        .nextCursor("1001")
                        .hasNext(true)
                        .build();

                given(aiArticleService.getMyAiArticlesByCategory(eq(TEST_USER_ID), eq(categoryId), any(), anyInt()))
                        .willReturn(mockSlice);

                // when & then
                mockMvc.perform(get("/api/v1/aiarticle")
                                .param("categoryId", String.valueOf(categoryId))
                                .param("cursor", "0")
                                .param("limit", "10")
                                .contentType(MediaType.APPLICATION_JSON))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.isSuccess").value(true))
                        .andExpect(jsonPath("$.result.linkuList.length()").value(1))
                        .andExpect(jsonPath("$.result.linkuList[0].linkuId").value(101L))
                        .andExpect(jsonPath("$.result.linkuList[0].linku").value("https://example.com/article"))
                        .andExpect(jsonPath("$.result.linkuList[0].emotionId").value(2L))
                        .andExpect(jsonPath("$.result.linkuList[0].domain").value("naver"))
                        .andExpect(jsonPath("$.result.linkuList[0].title").value("첫 번째 AI 제목"))
                        .andExpect(jsonPath("$.result.nextCursor").value("1001"))
                        .andExpect(jsonPath("$.result.hasNext").value(true))
                        .andDo(print());
            }

            @Test
            @DisplayName("결과가 없는 카테고리 조회 시 빈 리스트와 null 커서를 반환한다")
            void getMyAiArticlesByCategory_Empty() throws Exception {
                // given
                Long emptyCategoryId = 99L;

                LinkuResponseDTO.LinkuSliceResultDTO emptySlice = LinkuResponseDTO.LinkuSliceResultDTO.builder()
                        .linkuList(List.of())
                        .nextCursor(null)
                        .hasNext(false)
                        .build();

                given(aiArticleService.getMyAiArticlesByCategory(eq(TEST_USER_ID), eq(emptyCategoryId), any(), anyInt()))
                        .willReturn(emptySlice);

                // when & then
                mockMvc.perform(get("/api/v1/aiarticle")
                                .param("categoryId", String.valueOf(emptyCategoryId))
                                .contentType(MediaType.APPLICATION_JSON))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.isSuccess").value(true))
                        .andExpect(jsonPath("$.result.linkuList").isEmpty())
                        .andExpect(jsonPath("$.result.hasNext").value(false))
                        .andExpect(jsonPath("$.result.nextCursor").doesNotExist())
                        .andDo(print());
            }

            @Test
            @DisplayName("존재하지 않는 카테고리 ID여도 서비스가 빈 결과를 반환하면 정상 응답한다")
            void getMyAiArticlesByCategory_NotFoundCategory_ReturnsEmptySlice() throws Exception {
                // given
                Long notFoundCategoryId = 999L;

                LinkuResponseDTO.LinkuSliceResultDTO emptySlice = LinkuResponseDTO.LinkuSliceResultDTO.builder()
                        .linkuList(List.of())
                        .nextCursor(null)
                        .hasNext(false)
                        .build();

                given(aiArticleService.getMyAiArticlesByCategory(eq(TEST_USER_ID), eq(notFoundCategoryId), any(), anyInt()))
                        .willReturn(emptySlice);

                // when & then
                mockMvc.perform(get("/api/v1/aiarticle")
                                .param("categoryId", String.valueOf(notFoundCategoryId))
                                .param("cursor", "0")
                                .param("limit", "10")
                                .contentType(MediaType.APPLICATION_JSON))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.isSuccess").value(true))
                        .andExpect(jsonPath("$.result.linkuList").isEmpty())
                        .andExpect(jsonPath("$.result.hasNext").value(false))
                        .andExpect(jsonPath("$.result.nextCursor").doesNotExist())
                        .andDo(print());
            }
        }

        @Nested
        @DisplayName("실패 케이스")
        class Failure {

            @Test
            @DisplayName("categoryId가 문자열이면 400 Bad Request를 반환한다")
            void getMyAiArticlesByCategory_InvalidCategoryIdType() throws Exception {
                // when & then
                mockMvc.perform(get("/api/v1/aiarticle")
                                .param("categoryId", "invalid")
                                .contentType(MediaType.APPLICATION_JSON))
                        .andExpect(status().isBadRequest())
                        .andDo(print());
            }

            @Test
            @DisplayName("categoryId가 없으면 400 Bad Request를 반환한다")
            void getMyAiArticlesByCategory_MissingCategoryId() throws Exception {
                // when & then
                mockMvc.perform(get("/api/v1/aiarticle")
                                .contentType(MediaType.APPLICATION_JSON))
                        .andExpect(status().isBadRequest())
                        .andDo(print());
            }

            @Test
            @DisplayName("서비스에서 예외가 발생하면 에러 응답을 반환한다")
            void getMyAiArticlesByCategory_ServiceThrowsException_ReturnsErrorResponse() throws Exception {
                // given
                given(aiArticleService.getMyAiArticlesByCategory(any(), any(), any(), anyInt()))
                        .willThrow(new GeneralException(ErrorStatus._INTERNAL_SERVER_ERROR));

                // when & then
                mockMvc.perform(get("/api/v1/aiarticle")
                                .param("categoryId", "1")
                                .param("cursor", "0")
                                .param("limit", "10")
                                .contentType(MediaType.APPLICATION_JSON))
                        .andExpect(status().isInternalServerError())
                        .andExpect(jsonPath("$.isSuccess").value(false))
                        .andDo(print());
            }
        }
    }
}
