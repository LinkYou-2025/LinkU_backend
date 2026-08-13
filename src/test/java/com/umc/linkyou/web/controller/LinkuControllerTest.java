package com.umc.linkyou.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.linkyou.apiPayload.code.status.folder.FolderErrorStatus;
import com.umc.linkyou.apiPayload.code.status.linku.LinkuErrorStatus;
import com.umc.linkyou.apiPayload.exception.GeneralException;
import com.umc.linkyou.config.common.WebConfig;
import com.umc.linkyou.jwt.AccessTokenBlackListManager;
import com.umc.linkyou.jwt.CurrentUserArgumentResolver;
import com.umc.linkyou.jwt.JwtTokenProvider;
import com.umc.linkyou.service.Linku.LinkuCreateService;
import com.umc.linkyou.service.Linku.LinkuRecommendService;
import com.umc.linkyou.service.Linku.LinkuSearchService;
import com.umc.linkyou.service.Linku.LinkuService;
import com.umc.linkyou.support.security.TestSecurityConfig;
import com.umc.linkyou.support.security.WithCustomUser;
import com.umc.linkyou.web.dto.linku.LinkuRequestDTO;
import com.umc.linkyou.web.dto.linku.LinkuResponseDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LinkuController.class)
@Import({WebConfig.class, CurrentUserArgumentResolver.class, TestSecurityConfig.class})
@DisplayName("LinkuController 테스트")
class LinkuControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private LinkuService linkuService;

    @MockitoBean
    private LinkuCreateService linkuCreateService;

    @MockitoBean
    private LinkuSearchService linkuSearchService;

    @MockitoBean
    private LinkuRecommendService linkuRecommendService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private AccessTokenBlackListManager accessTokenBlackListManager;

    private static final Long LINKU_ID = 100L;
    private static final Long NEW_FOLDER_ID = 20L;

    @Nested
    @DisplayName("PATCH /api/v1/linku/saved/{userLinkuId}/folder - 링크 폴더 이동")
    class UpdateLinkuFolder {

        @Nested
        @DisplayName("성공")
        class Success {

            @Test
            @DisplayName("인증된 사용자가 정상 folderId로 요청하면 이동된 폴더 정보를 반환한다")
            @WithCustomUser(userId = 1L)
            void 정상_요청_시_이동된_폴더_정보를_반환한다() throws Exception {
                // given
                LinkuRequestDTO.LinkuFolderUpdateDTO request =
                        LinkuRequestDTO.LinkuFolderUpdateDTO.builder().folderId(NEW_FOLDER_ID).build();

                LinkuResponseDTO.LinkuFolderChangeResultDTO result = LinkuResponseDTO.LinkuFolderChangeResultDTO.builder()
                        .userLinkuId(LINKU_ID)
                        .folderId(NEW_FOLDER_ID)
                        .folderName("영어 공부")
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build();

                given(linkuService.updateLinkuFolder(eq(1L), eq(LINKU_ID), any()))
                        .willReturn(result);

                // when & then
                mockMvc.perform(patch("/api/v1/linku/saved/{userLinkuId}/folder", LINKU_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                                .with(csrf()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.isSuccess").value(true))
                        .andExpect(jsonPath("$.code").value("LINKU2007"))
                        .andExpect(jsonPath("$.result.userLinkuId").value(LINKU_ID))
                        .andExpect(jsonPath("$.result.folderId").value(NEW_FOLDER_ID))
                        .andExpect(jsonPath("$.result.folderName").value("영어 공부"));
            }
        }

        @Nested
        @DisplayName("실패")
        class Failure {

            @Test
            @DisplayName("이동할 폴더가 존재하지 않으면 404와 에러코드를 반환한다")
            @WithCustomUser(userId = 1L)
            void 폴더가_존재하지_않으면_404를_반환한다() throws Exception {
                // given
                LinkuRequestDTO.LinkuFolderUpdateDTO request =
                        LinkuRequestDTO.LinkuFolderUpdateDTO.builder().folderId(NEW_FOLDER_ID).build();

                given(linkuService.updateLinkuFolder(eq(1L), eq(LINKU_ID), any()))
                        .willThrow(new GeneralException(FolderErrorStatus._FOLDER_NOT_FOUND));

                // when & then
                mockMvc.perform(patch("/api/v1/linku/saved/{userLinkuId}/folder", LINKU_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                                .with(csrf()))
                        .andExpect(status().isNotFound())
                        .andExpect(jsonPath("$.isSuccess").value(false))
                        .andExpect(jsonPath("$.code").value(FolderErrorStatus._FOLDER_NOT_FOUND.getCode()));
            }

            @Test
            @DisplayName("이동할 폴더에 대한 권한이 없으면 403과 에러코드를 반환한다")
            @WithCustomUser(userId = 1L)
            void 폴더_권한이_없으면_403을_반환한다() throws Exception {
                // given
                LinkuRequestDTO.LinkuFolderUpdateDTO request =
                        LinkuRequestDTO.LinkuFolderUpdateDTO.builder().folderId(NEW_FOLDER_ID).build();

                given(linkuService.updateLinkuFolder(eq(1L), eq(LINKU_ID), any()))
                        .willThrow(new GeneralException(FolderErrorStatus._FOLDER_ACCESS_FORBIDDEN));

                // when & then
                mockMvc.perform(patch("/api/v1/linku/saved/{userLinkuId}/folder", LINKU_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                                .with(csrf()))
                        .andExpect(status().isForbidden())
                        .andExpect(jsonPath("$.isSuccess").value(false))
                        .andExpect(jsonPath("$.code").value(FolderErrorStatus._FOLDER_ACCESS_FORBIDDEN.getCode()));
            }

            @Test
            @DisplayName("사용자의 링크가 없으면 404와 에러코드를 반환한다")
            @WithCustomUser(userId = 1L)
            void 사용자의_링크가_없으면_404를_반환한다() throws Exception {
                // given
                LinkuRequestDTO.LinkuFolderUpdateDTO request =
                        LinkuRequestDTO.LinkuFolderUpdateDTO.builder().folderId(NEW_FOLDER_ID).build();

                given(linkuService.updateLinkuFolder(eq(1L), eq(LINKU_ID), any()))
                        .willThrow(new GeneralException(LinkuErrorStatus._USER_LINKU_NOT_FOUND));

                // when & then
                mockMvc.perform(patch("/api/v1/linku/saved/{userLinkuId}/folder", LINKU_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                                .with(csrf()))
                        .andExpect(status().isNotFound())
                        .andExpect(jsonPath("$.isSuccess").value(false))
                        .andExpect(jsonPath("$.code").value(LinkuErrorStatus._USER_LINKU_NOT_FOUND.getCode()));
            }

            @Test
            @DisplayName("인증되지 않은 요청은 401을 반환한다")
            void 인증되지_않으면_401을_반환한다() throws Exception {
                // given
                LinkuRequestDTO.LinkuFolderUpdateDTO request =
                        LinkuRequestDTO.LinkuFolderUpdateDTO.builder().folderId(NEW_FOLDER_ID).build();

                // when & then
                mockMvc.perform(patch("/api/v1/linku/{linkuId}/folder", LINKU_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                                .with(csrf()))
                        .andExpect(status().isUnauthorized());
            }
        }
    }
}
