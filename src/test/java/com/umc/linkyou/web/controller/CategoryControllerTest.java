package com.umc.linkyou.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.linkyou.apiPayload.code.status.category.CategoryErrorStatus;
import com.umc.linkyou.apiPayload.exception.GeneralException;
import com.umc.linkyou.config.common.WebConfig;
import com.umc.linkyou.jwt.AccessTokenBlackListManager;
import com.umc.linkyou.jwt.CurrentUserArgumentResolver;
import com.umc.linkyou.jwt.JwtTokenProvider;
import com.umc.linkyou.service.category.CategoryService;
import com.umc.linkyou.support.security.TestSecurityConfig;
import com.umc.linkyou.support.security.WithCustomUser;
import com.umc.linkyou.web.dto.category.CategoryListResponseDTO;
import com.umc.linkyou.web.dto.category.UpdateCategoryColorRequestDTO;
import com.umc.linkyou.web.dto.category.UserCategoryColorResponseDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static com.umc.linkyou.support.util.ApiResponseTestUtils.readResult;
import static com.umc.linkyou.support.util.ApiResponseTestUtils.readResultList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CategoryController.class)
@Import({WebConfig.class, CurrentUserArgumentResolver.class, TestSecurityConfig.class})
@DisplayName("CategoryController 테스트")
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CategoryService categoryService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private AccessTokenBlackListManager accessTokenBlackListManager;

    private static final Long CATEGORY_ID = 10L;
    private static final Long FCOLOR_ID = 100L;

    @Nested
    @DisplayName("GET /api/v1/categories - 카테고리 목록 조회")
    class GetCategoryList {

        @Nested
        @DisplayName("성공")
        class Success {
            @Test
            @DisplayName("인증된 사용자가 조회하면 카테고리 목록을 반환한다")
            @WithCustomUser(userId = 1L)
            void 인증된_사용자가_조회하면_카테고리_목록을_반환한다() throws Exception {
                CategoryListResponseDTO category = CategoryListResponseDTO.builder()
                        .categoryId(CATEGORY_ID)
                        .categoryName("어학")
                        .colorName("블루")
                        .build();
                given(categoryService.getCategories(1L)).willReturn(List.of(category));

                MvcResult result = mockMvc.perform(get("/api/v1/categories"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.isSuccess").value(true))
                        .andExpect(jsonPath("$.code").value("CATEGORY2001"))
                        .andReturn();

                List<CategoryListResponseDTO> body =
                        readResultList(result, objectMapper, CategoryListResponseDTO.class);
                assertThat(body).hasSize(1);
                assertThat(body.get(0).getCategoryId()).isEqualTo(CATEGORY_ID);
                assertThat(body.get(0).getCategoryName()).isEqualTo("어학");
            }
        }

        @Nested
        @DisplayName("실패")
        class Failure {
            @Test
            @DisplayName("인증되지 않은 요청은 401을 반환한다")
            void 인증되지_않으면_401을_반환한다() throws Exception {
                mockMvc.perform(get("/api/v1/categories"))
                        .andExpect(status().isUnauthorized());
            }
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/categories/{categoryId}/color - 카테고리 색상 수정")
    class UpdateUserCategoryColor {

        @Nested
        @DisplayName("성공")
        class Success {
            @Test
            @DisplayName("정상 요청 시 변경된 색상 정보를 반환한다")
            @WithCustomUser(userId = 1L)
            void 정상_요청_시_변경된_색상_정보를_반환한다() throws Exception {
                UserCategoryColorResponseDTO response = UserCategoryColorResponseDTO.builder()
                        .categoryId(CATEGORY_ID)
                        .fcolorId(FCOLOR_ID)
                        .colorName("블루")
                        .build();
                given(categoryService.updateUserCategoryColor(eq(1L), eq(CATEGORY_ID), any()))
                        .willReturn(response);

                UpdateCategoryColorRequestDTO request = new UpdateCategoryColorRequestDTO();
                request.setFcolorId(FCOLOR_ID);

                MvcResult result = mockMvc.perform(put("/api/v1/categories/{categoryId}/color", CATEGORY_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                                .with(csrf()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.isSuccess").value(true))
                        .andExpect(jsonPath("$.code").value("CATEGORY2002"))
                        .andReturn();

                UserCategoryColorResponseDTO body =
                        readResult(result, objectMapper, UserCategoryColorResponseDTO.class);
                assertThat(body.getCategoryId()).isEqualTo(CATEGORY_ID);
                assertThat(body.getFcolorId()).isEqualTo(FCOLOR_ID);
            }
        }

        @Nested
        @DisplayName("실패")
        class Failure {
            @Test
            @DisplayName("존재하지 않는 카테고리면 404와 에러코드를 반환한다")
            @WithCustomUser(userId = 1L)
            void 존재하지_않는_카테고리면_404와_에러코드를_반환한다() throws Exception {
                given(categoryService.updateUserCategoryColor(eq(1L), eq(CATEGORY_ID), any()))
                        .willThrow(new GeneralException(CategoryErrorStatus._CATEGORY_NOT_FOUND));

                UpdateCategoryColorRequestDTO request = new UpdateCategoryColorRequestDTO();
                request.setFcolorId(FCOLOR_ID);

                mockMvc.perform(put("/api/v1/categories/{categoryId}/color", CATEGORY_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                                .with(csrf()))
                        .andExpect(status().isNotFound())
                        .andExpect(jsonPath("$.isSuccess").value(false))
                        .andExpect(jsonPath("$.code").value(CategoryErrorStatus._CATEGORY_NOT_FOUND.getCode()));
            }

            @Test
            @DisplayName("존재하지 않는 fcolorId면 404와 에러코드를 반환한다")
            @WithCustomUser(userId = 1L)
            void 존재하지_않는_fcolorId면_404와_에러코드를_반환한다() throws Exception {
                given(categoryService.updateUserCategoryColor(eq(1L), eq(CATEGORY_ID), any()))
                        .willThrow(new GeneralException(CategoryErrorStatus._FCOLOR_NOT_FOUND));

                UpdateCategoryColorRequestDTO request = new UpdateCategoryColorRequestDTO();
                request.setFcolorId(FCOLOR_ID);

                mockMvc.perform(put("/api/v1/categories/{categoryId}/color", CATEGORY_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                                .with(csrf()))
                        .andExpect(status().isNotFound())
                        .andExpect(jsonPath("$.isSuccess").value(false))
                        .andExpect(jsonPath("$.code").value(CategoryErrorStatus._FCOLOR_NOT_FOUND.getCode()));
            }

            @Test
            @DisplayName("인증되지 않은 요청은 401을 반환한다")
            void 인증되지_않으면_401을_반환한다() throws Exception {
                UpdateCategoryColorRequestDTO request = new UpdateCategoryColorRequestDTO();
                request.setFcolorId(FCOLOR_ID);

                mockMvc.perform(put("/api/v1/categories/{categoryId}/color", CATEGORY_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                                .with(csrf()))
                        .andExpect(status().isUnauthorized());
            }
        }
    }
}
