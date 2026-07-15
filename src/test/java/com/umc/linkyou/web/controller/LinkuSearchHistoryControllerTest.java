package com.umc.linkyou.web.controller;

import com.umc.linkyou.apiPayload.code.status.linku.LinkuErrorStatus;
import com.umc.linkyou.apiPayload.code.status.linku.LinkuSuccessStatus;
import com.umc.linkyou.apiPayload.exception.GeneralException;
import com.umc.linkyou.config.common.WebConfig;
import com.umc.linkyou.jwt.AccessTokenBlackListManager;
import com.umc.linkyou.jwt.CurrentUserArgumentResolver;
import com.umc.linkyou.jwt.JwtTokenProvider;
import com.umc.linkyou.jwt.SecurityErrorResponseWriter;
import com.umc.linkyou.service.Linku.LinkuSearchService;
import com.umc.linkyou.support.security.TestSecurityConfig;
import com.umc.linkyou.support.security.WithCustomUser;
import com.umc.linkyou.web.dto.linku.LinkuSearchHistoryItemDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LinkuSearchHistoryController.class)
@Import({WebConfig.class, CurrentUserArgumentResolver.class, TestSecurityConfig.class})
@DisplayName("LinkuSearchHistoryController 테스트")
class LinkuSearchHistoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LinkuSearchService linkuSearchService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private AccessTokenBlackListManager accessTokenBlackListManager;

    @MockitoBean
    private SecurityErrorResponseWriter securityErrorResponseWriter;

    @Nested
    @DisplayName("GET /api/v1/links/search/history - 최근 검색어 조회")
    class GetRecentKeywords {

        @Nested
        @DisplayName("성공")
        class Success {

            @Test
            @DisplayName("인증된 사용자가 요청 시 최근 검색어 목록을 반환한다")
            @WithCustomUser(userId = 1L)
            void 인증된_사용자가_요청_시_최근_검색어_목록을_반환한다() throws Exception {
                given(linkuSearchService.getRecentKeywords(1L))
                        .willReturn(List.of(
                                new LinkuSearchHistoryItemDTO(2L, "코틀린"),
                                new LinkuSearchHistoryItemDTO(1L, "자바")
                        ));

                mockMvc.perform(get("/api/v1/links/search/history"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.isSuccess").value(true))
                        .andExpect(jsonPath("$.code").value(LinkuSuccessStatus.SEARCH_HISTORY_OK.getCode()))
                        .andExpect(jsonPath("$.result[0].searchHistoryId").value(2))
                        .andExpect(jsonPath("$.result[0].keyword").value("코틀린"))
                        .andExpect(jsonPath("$.result[1].searchHistoryId").value(1))
                        .andExpect(jsonPath("$.result[1].keyword").value("자바"));
            }

            @Test
            @DisplayName("검색어가 없으면 빈 배열을 반환한다")
            @WithCustomUser(userId = 1L)
            void 검색어가_없으면_빈_배열을_반환한다() throws Exception {
                given(linkuSearchService.getRecentKeywords(1L)).willReturn(List.of());

                mockMvc.perform(get("/api/v1/links/search/history"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.isSuccess").value(true))
                        .andExpect(jsonPath("$.result").isEmpty());
            }
        }

        @Nested
        @DisplayName("실패")
        class Failure {

            @Test
            @DisplayName("인증되지 않은 요청은 401을 반환한다")
            void 인증되지_않으면_401을_반환한다() throws Exception {
                mockMvc.perform(get("/api/v1/links/search/history"))
                        .andExpect(status().isUnauthorized());
            }
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/links/search/history/{searchHistoryId} - 검색어 단일 삭제")
    class DeleteKeyword {

        @Nested
        @DisplayName("성공")
        class Success {

            @Test
            @DisplayName("존재하는 검색어 ID로 요청 시 삭제 성공 응답을 반환한다")
            @WithCustomUser(userId = 1L)
            void 존재하는_검색어_ID로_요청_시_삭제_성공_응답을_반환한다() throws Exception {
                willDoNothing().given(linkuSearchService).deleteKeyword(1L, 1L);

                mockMvc.perform(delete("/api/v1/links/search/history/{searchHistoryId}", 1L)
                                .with(csrf()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.isSuccess").value(true))
                        .andExpect(jsonPath("$.code").value(LinkuSuccessStatus.SEARCH_HISTORY_DELETED.getCode()));
            }
        }

        @Nested
        @DisplayName("실패")
        class Failure {

            @Test
            @DisplayName("존재하지 않는 검색어 ID로 요청 시 404를 반환한다")
            @WithCustomUser(userId = 1L)
            void 존재하지_않는_검색어_ID로_요청_시_404를_반환한다() throws Exception {
                willThrow(new GeneralException(LinkuErrorStatus._SEARCH_HISTORY_NOT_FOUND))
                        .given(linkuSearchService).deleteKeyword(anyLong(), anyLong());

                mockMvc.perform(delete("/api/v1/links/search/history/{searchHistoryId}", 99L)
                                .with(csrf()))
                        .andExpect(status().isNotFound())
                        .andExpect(jsonPath("$.isSuccess").value(false))
                        .andExpect(jsonPath("$.code").value(LinkuErrorStatus._SEARCH_HISTORY_NOT_FOUND.getCode()));
            }

            @Test
            @DisplayName("인증되지 않은 요청은 401을 반환한다")
            void 인증되지_않으면_401을_반환한다() throws Exception {
                mockMvc.perform(delete("/api/v1/links/search/history/{searchHistoryId}", 1L)
                                .with(csrf()))
                        .andExpect(status().isUnauthorized());
            }
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/links/search/history - 검색어 전체 삭제")
    class DeleteAllKeywords {

        @Nested
        @DisplayName("성공")
        class Success {

            @Test
            @DisplayName("검색어가 존재하면 전체 삭제 성공 응답을 반환한다")
            @WithCustomUser(userId = 1L)
            void 검색어가_존재하면_전체_삭제_성공_응답을_반환한다() throws Exception {
                willDoNothing().given(linkuSearchService).deleteAllKeywords(1L);

                mockMvc.perform(delete("/api/v1/links/search/history")
                                .with(csrf()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.isSuccess").value(true))
                        .andExpect(jsonPath("$.code").value(LinkuSuccessStatus.SEARCH_HISTORY_ALL_DELETED.getCode()));
            }
        }

        @Nested
        @DisplayName("실패")
        class Failure {

            @Test
            @DisplayName("삭제할 검색어가 없으면 404를 반환한다")
            @WithCustomUser(userId = 1L)
            void 삭제할_검색어가_없으면_404를_반환한다() throws Exception {
                willThrow(new GeneralException(LinkuErrorStatus._SEARCH_HISTORY_NOT_FOUND))
                        .given(linkuSearchService).deleteAllKeywords(anyLong());

                mockMvc.perform(delete("/api/v1/links/search/history")
                                .with(csrf()))
                        .andExpect(status().isNotFound())
                        .andExpect(jsonPath("$.isSuccess").value(false))
                        .andExpect(jsonPath("$.code").value(LinkuErrorStatus._SEARCH_HISTORY_NOT_FOUND.getCode()));
            }

            @Test
            @DisplayName("인증되지 않은 요청은 401을 반환한다")
            void 인증되지_않으면_401을_반환한다() throws Exception {
                mockMvc.perform(delete("/api/v1/links/search/history")
                                .with(csrf()))
                        .andExpect(status().isUnauthorized());
            }
        }
    }
}
