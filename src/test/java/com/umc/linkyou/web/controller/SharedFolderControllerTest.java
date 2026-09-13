package com.umc.linkyou.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.linkyou.apiPayload.code.status.folder.FolderErrorStatus;
import com.umc.linkyou.apiPayload.exception.GeneralException;
import com.umc.linkyou.config.common.WebConfig;
import com.umc.linkyou.jwt.AccessTokenBlackListManager;
import com.umc.linkyou.jwt.CurrentUserArgumentResolver;
import com.umc.linkyou.jwt.JwtTokenProvider;
import com.umc.linkyou.service.folder.shared.SharedFolderService;
import com.umc.linkyou.support.security.TestSecurityConfig;
import com.umc.linkyou.support.security.WithCustomUser;
import com.umc.linkyou.web.dto.folder.share.SharedFolderGroupResponseDTO;
import com.umc.linkyou.web.dto.folder.share.SharedFolderItemDTO;
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

import static com.umc.linkyou.support.util.ApiResponseTestUtils.readResultList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SharedFolderController.class)
@Import({WebConfig.class, CurrentUserArgumentResolver.class, TestSecurityConfig.class})
@DisplayName("SharedFolderController 테스트")
class SharedFolderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private SharedFolderService sharedFolderService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private AccessTokenBlackListManager accessTokenBlackListManager;

    private static final Long FOLDER_ID = 100L;

    @Nested
    @DisplayName("GET /api/v1/folders/shared - 공유 받은 폴더 목록 조회")
    class GetSharedFolders {

        @Nested
        @DisplayName("성공")
        class Success {
            @Test
            @DisplayName("정상 요청 시 소유자별로 그룹핑된 폴더 목록을 반환한다")
            @WithCustomUser(userId = 1L)
            void 정상_요청_시_그룹핑된_목록을_반환한다() throws Exception {
                SharedFolderItemDTO folder = SharedFolderItemDTO.builder()
                        .folderId(FOLDER_ID).folderName("어학").isBookmarked(false).categoryId(10L).build();
                SharedFolderGroupResponseDTO group = SharedFolderGroupResponseDTO.builder()
                        .userId(2L).nickname("친구").folders(List.of(folder)).build();

                given(sharedFolderService.getSharedFolders(1L)).willReturn(List.of(group));

                MvcResult result = mockMvc.perform(get("/api/v1/folders/shared"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.isSuccess").value(true))
                        .andExpect(jsonPath("$.code").value("FOLDER2014"))
                        .andReturn();

                List<SharedFolderGroupResponseDTO> groups = readResultList(result, objectMapper, SharedFolderGroupResponseDTO.class);
                assertThat(groups).hasSize(1);
                assertThat(groups.get(0).getNickname()).isEqualTo("친구");
                assertThat(groups.get(0).getFolders()).hasSize(1);
                assertThat(groups.get(0).getFolders().get(0).getFolderId()).isEqualTo(FOLDER_ID);
                assertThat(groups.get(0).getFolders().get(0).getCategoryId()).isEqualTo(10L);
            }
        }

        @Nested
        @DisplayName("실패")
        class Failure {
            @Test
            @DisplayName("인증되지 않은 요청은 401을 반환한다")
            void 인증되지_않으면_401을_반환한다() throws Exception {
                mockMvc.perform(get("/api/v1/folders/shared"))
                        .andExpect(status().isUnauthorized());
            }
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/folders/shared/{folderId} - 공유 받은 폴더 삭제")
    class DeleteSharedFolder {

        @Nested
        @DisplayName("성공")
        class Success {
            @Test
            @DisplayName("정상 요청 시 삭제에 성공한다")
            @WithCustomUser(userId = 1L)
            void 정상_요청_시_삭제에_성공한다() throws Exception {
                mockMvc.perform(delete("/api/v1/folders/shared/{folderId}", FOLDER_ID).with(csrf()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.isSuccess").value(true))
                        .andExpect(jsonPath("$.code").value("FOLDER2015"));
            }
        }

        @Nested
        @DisplayName("실패")
        class Failure {
            @Test
            @DisplayName("소유자 본인은 삭제할 수 없어 403과 에러코드를 반환한다")
            @WithCustomUser(userId = 1L)
            void 소유자는_삭제할수없으면_403을_반환한다() throws Exception {
                doThrow(new GeneralException(FolderErrorStatus._FOLDER_DELETE_FORBIDDEN))
                        .when(sharedFolderService).deleteSharedFolder(eq(1L), eq(FOLDER_ID));

                mockMvc.perform(delete("/api/v1/folders/shared/{folderId}", FOLDER_ID).with(csrf()))
                        .andExpect(status().isForbidden())
                        .andExpect(jsonPath("$.isSuccess").value(false))
                        .andExpect(jsonPath("$.code").value(FolderErrorStatus._FOLDER_DELETE_FORBIDDEN.getCode()));
            }
        }
    }
}
