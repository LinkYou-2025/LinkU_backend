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
import static org.mockito.ArgumentMatchers.anyInt;
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
    @DisplayName("성공 - 특정 카테고리 ID로 조회 시 커서 기반 페이징 결과를 반환한다")
    void getMyAiArticlesByCategory_Success() throws Exception {
        // given
        Long categoryId = 1L;
        Long cursor = 0L;
        int limit = 10;

        // 1. 내부 리스트 데이터 준비
        LinkuResponseDTO.LinkuResultDTO article1 = LinkuResponseDTO.LinkuResultDTO.builder()
                .linkuId(101L)
                .userLinkuId(1001L) // 커서로 사용될 ID
                .categoryId(categoryId)
                .title("첫 번째 AI 제목")
                .aiArticleExists(true)
                .build();

        List<LinkuResponseDTO.LinkuResultDTO> mockList = List.of(article1);

        // 2. Slice 결과 DTO 생성
        LinkuResponseDTO.LinkuSliceResultDTO mockSlice = LinkuResponseDTO.LinkuSliceResultDTO.builder()
                .linkuList(mockList)
                .nextCursor("1001")
                .hasNext(true)
                .build();

        // 서비스 메서드 시그니처 변경 반영 (userId, categoryId, cursor, limit)
        given(aiArticleService.getMyAiArticlesByCategory(eq(TEST_USER_ID), eq(categoryId), any(), anyInt()))
                .willReturn(mockSlice);

        // when & then
        mockMvc.perform(get("/api/v1/aiarticle/category/{categoryId}", categoryId)
                        .param("cursor", "0")
                        .param("limit", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                // 3. JSON 구조 검증 ($.result.linkuList[0] 형태)
                .andExpect(jsonPath("$.result.linkuList.length()").value(1))
                .andExpect(jsonPath("$.result.linkuList[0].linkuId").value(101L))
                .andExpect(jsonPath("$.result.nextCursor").value("1001"))
                .andExpect(jsonPath("$.result.hasNext").value(true))
                .andDo(print());
    }

    @Test
    @DisplayName("성공 - 결과가 없는 카테고리 조회 시 빈 리스트와 null 커서를 반환한다")
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
        mockMvc.perform(get("/api/v1/aiarticle/category/{categoryId}", emptyCategoryId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.linkuList").isEmpty())
                .andExpect(jsonPath("$.result.hasNext").value(false))
                .andExpect(jsonPath("$.result.nextCursor").isEmpty())
                .andDo(print());
    }
}
