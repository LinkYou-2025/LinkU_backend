package com.umc.linkyou.web.controller;

import com.umc.linkyou.config.security.jwt.JwtTokenProvider;
import com.umc.linkyou.repository.aiArticleRepository.AiArticleRepository;
import com.umc.linkyou.service.AiArticleService;
import com.umc.linkyou.utils.UsersUtils;
import com.umc.linkyou.web.dto.linku.LinkuResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AiArticleController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("AiArticleController 테스트")
class AiArticleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AiArticleService aiArticleService;

    @MockitoBean
    private UsersUtils usersUtils;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private AiArticleRepository aiArticleRepository;

    private final Long TEST_USER_ID = 1L;

    @BeforeEach
    void setUp() {
        given(usersUtils.getAuthenticatedUserId(any())).willReturn(TEST_USER_ID);
    }

    @Test
    @DisplayName("성공 - 특정 카테고리 ID로 조회 시 해당 카테고리의 AI 요약 리스트를 반환한다")
    void getMyAiArticlesByCategory_Success() throws Exception {
        // given
        Long categoryId = 1L;

        // 2개의 서로 다른 데이터 준비
        LinkuResponseDTO.LinkuResultDTO article1 = LinkuResponseDTO.LinkuResultDTO.builder()
                .linkuId(101L)
                .categoryId(categoryId)
                .title("첫 번째 AI 제목")
                .summary("첫 번째 요약")
                .keyword("#태그1 #태그2")
                .aiArticleExists(true)
                .build();

        LinkuResponseDTO.LinkuResultDTO article2 = LinkuResponseDTO.LinkuResultDTO.builder()
                .linkuId(102L)
                .categoryId(categoryId)
                .title("두 번째 AI 제목")
                .summary("두 번째 요약")
                .keyword("#태그3 #태그4")
                .aiArticleExists(true)
                .build();

        List<LinkuResponseDTO.LinkuResultDTO> mockList = List.of(article1, article2);

        given(aiArticleService.getMyAiArticlesByCategory(eq(TEST_USER_ID), eq(categoryId)))
                .willReturn(mockList);

        // when & then
        mockMvc.perform(get("/api/v1/aiarticle/category/{categoryId}", categoryId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                // 1. 전체 리스트 개수 확인
                .andExpect(jsonPath("$.result.length()").value(2))
                // 2. 첫 번째 항목 상세 검증
                .andExpect(jsonPath("$.result[0].linkuId").value(101L))
                .andExpect(jsonPath("$.result[0].title").value("첫 번째 AI 제목"))
                .andExpect(jsonPath("$.result[0].categoryId").value(categoryId))
                // 3. 두 번째 항목 상세 검증
                .andExpect(jsonPath("$.result[1].linkuId").value(102L))
                .andExpect(jsonPath("$.result[1].title").value("두 번째 AI 제목"))
                .andExpect(jsonPath("$.result[1].summary").value("두 번째 요약"))
                .andDo(print());
    }

    @Test
    @DisplayName("성공 - 결과가 없는 카테고리 조회 시 빈 리스트를 반환한다")
    void getMyAiArticlesByCategory_Empty() throws Exception {
        Long emptyCategoryId = 99L;
        given(aiArticleService.getMyAiArticlesByCategory(eq(TEST_USER_ID), eq(emptyCategoryId)))
                .willReturn(List.of());

        mockMvc.perform(get("/api/v1/aiarticle/category/{categoryId}", emptyCategoryId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").isEmpty())
                .andDo(print());
    }
}
