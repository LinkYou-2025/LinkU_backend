package com.umc.linkyou.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.linkyou.apiPayload.code.status.folder.FolderErrorStatus;
import com.umc.linkyou.apiPayload.code.status.folder.ShareFolderErrorStatus;
import com.umc.linkyou.apiPayload.exception.GeneralException;
import com.umc.linkyou.config.common.WebConfig;
import com.umc.linkyou.domain.enums.PermissionType;
import com.umc.linkyou.jwt.AccessTokenBlackListManager;
import com.umc.linkyou.jwt.CurrentUserArgumentResolver;
import com.umc.linkyou.jwt.JwtTokenProvider;
import com.umc.linkyou.service.folder.share.ShareFolderService;
import com.umc.linkyou.support.security.TestSecurityConfig;
import com.umc.linkyou.support.security.WithCustomUser;
import com.umc.linkyou.web.dto.folder.share.FolderPermissionRequestDTO;
import com.umc.linkyou.web.dto.folder.share.MySharedFolderResponseDTO;
import com.umc.linkyou.web.dto.folder.share.ShareFolderResponseDTO;
import com.umc.linkyou.web.dto.folder.share.ViewerResponseDTO;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ShareFolderController.class)
@Import({WebConfig.class, CurrentUserArgumentResolver.class, TestSecurityConfig.class})
@DisplayName("ShareFolderController 테스트")
class ShareFolderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ShareFolderService shareFolderService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private AccessTokenBlackListManager accessTokenBlackListManager;

    private static final Long FOLDER_ID = 100L;
    private static final Long USERS_FOLDER_ID = 10L;

    @Nested
    @DisplayName("POST /api/v1/folders/share/{folderId}/invitation - 초대 링크 생성")
    class CreateInviteLink {

        @Nested
        @DisplayName("성공")
        class Success {
            @Test
            @DisplayName("정상 요청 시 초대 토큰을 반환한다")
            @WithCustomUser(userId = 1L)
            void 정상_요청_시_초대링크를_반환한다() throws Exception {
                given(shareFolderService.createInviteLink(1L, FOLDER_ID)).willReturn("token-abc");

                mockMvc.perform(post("/api/v1/folders/share/{folderId}/invitation", FOLDER_ID).with(csrf()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.isSuccess").value(true))
                        .andExpect(jsonPath("$.code").value("FOLDER2008"))
                        .andExpect(jsonPath("$.result").value("token-abc"));
            }
        }

        @Nested
        @DisplayName("실패")
        class Failure {
            @Test
            @DisplayName("소유자가 아니면 403과 에러코드를 반환한다")
            @WithCustomUser(userId = 1L)
            void 소유자가_아니면_403을_반환한다() throws Exception {
                given(shareFolderService.createInviteLink(1L, FOLDER_ID))
                        .willThrow(new GeneralException(ShareFolderErrorStatus._FOLDER_PERMISSION_NOT_ALLOWED));

                mockMvc.perform(post("/api/v1/folders/share/{folderId}/invitation", FOLDER_ID).with(csrf()))
                        .andExpect(status().isForbidden())
                        .andExpect(jsonPath("$.isSuccess").value(false))
                        .andExpect(jsonPath("$.code").value(ShareFolderErrorStatus._FOLDER_PERMISSION_NOT_ALLOWED.getCode()));
            }

            @Test
            @DisplayName("인증되지 않은 요청은 401을 반환한다")
            void 인증되지_않으면_401을_반환한다() throws Exception {
                mockMvc.perform(post("/api/v1/folders/share/{folderId}/invitation", FOLDER_ID).with(csrf()))
                        .andExpect(status().isUnauthorized());
            }
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/folders/share/{folderId}/invitation - 초대 링크 비활성화")
    class DeactivateInviteLink {

        @Nested
        @DisplayName("성공")
        class Success {
            @Test
            @DisplayName("정상 요청 시 비활성화에 성공한다")
            @WithCustomUser(userId = 1L)
            void 정상_요청_시_비활성화에_성공한다() throws Exception {
                mockMvc.perform(delete("/api/v1/folders/share/{folderId}/invitation", FOLDER_ID).with(csrf()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.isSuccess").value(true))
                        .andExpect(jsonPath("$.code").value("FOLDER2009"));
            }
        }
    }

    @Nested
    @DisplayName("GET /api/v1/folders/share/{folderId}/members - 폴더 멤버 조회")
    class GetFolderViewers {

        @Nested
        @DisplayName("성공")
        class Success {
            @Test
            @DisplayName("정상 요청 시 멤버 목록을 반환한다")
            @WithCustomUser(userId = 1L)
            void 정상_요청_시_멤버목록을_반환한다() throws Exception {
                ViewerResponseDTO viewer = new ViewerResponseDTO();
                viewer.setUserId(5L);
                viewer.setUserName("멤버");
                viewer.setPermission(PermissionType.VIEWER.name());

                given(shareFolderService.getViewers(1L, FOLDER_ID)).willReturn(List.of(viewer));

                MvcResult result = mockMvc.perform(get("/api/v1/folders/share/{folderId}/members", FOLDER_ID))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.isSuccess").value(true))
                        .andReturn();

                List<ViewerResponseDTO> viewers = readResultList(result, objectMapper, ViewerResponseDTO.class);
                assertThat(viewers).hasSize(1);
                assertThat(viewers.get(0).getUserId()).isEqualTo(5L);
                assertThat(viewers.get(0).getPermission()).isEqualTo("VIEWER");
            }
        }

        @Nested
        @DisplayName("실패")
        class Failure {
            @Test
            @DisplayName("존재하지 않는 폴더면 404와 에러코드를 반환한다")
            @WithCustomUser(userId = 1L)
            void 존재하지_않는_폴더면_404를_반환한다() throws Exception {
                given(shareFolderService.getViewers(1L, FOLDER_ID))
                        .willThrow(new GeneralException(FolderErrorStatus._FOLDER_NOT_FOUND));

                mockMvc.perform(get("/api/v1/folders/share/{folderId}/members", FOLDER_ID))
                        .andExpect(status().isNotFound())
                        .andExpect(jsonPath("$.isSuccess").value(false))
                        .andExpect(jsonPath("$.code").value(FolderErrorStatus._FOLDER_NOT_FOUND.getCode()));
            }
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/folders/share/{folderId}/members/{userFolderId} - 폴더 권한 수정")
    class UpdateViewerPermission {

        @Nested
        @DisplayName("성공")
        class Success {
            @Test
            @DisplayName("정상 요청 시 변경된 권한 정보를 반환한다")
            @WithCustomUser(userId = 1L)
            void 정상_요청_시_변경된_권한정보를_반환한다() throws Exception {
                FolderPermissionRequestDTO request = new FolderPermissionRequestDTO();
                request.setPermission(PermissionType.WRITER);

                ShareFolderResponseDTO response = ShareFolderResponseDTO.builder()
                        .folderId(FOLDER_ID).userId(5L).permission("WRITER").sharedAt("2026-01-01T00:00:00").build();

                given(shareFolderService.updateViewerPermission(eq(1L), eq(FOLDER_ID), eq(USERS_FOLDER_ID), any()))
                        .willReturn(response);

                MvcResult result = mockMvc.perform(put("/api/v1/folders/share/{folderId}/members/{userFolderId}", FOLDER_ID, USERS_FOLDER_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                                .with(csrf()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.isSuccess").value(true))
                        .andReturn();

                ShareFolderResponseDTO permission = readResult(result, objectMapper, ShareFolderResponseDTO.class);
                assertThat(permission.getPermission()).isEqualTo("WRITER");
            }
        }

        @Nested
        @DisplayName("실패")
        class Failure {
            @Test
            @DisplayName("소유자의 권한은 수정할 수 없어 403과 에러코드를 반환한다")
            @WithCustomUser(userId = 1L)
            void 소유자권한수정시_403을_반환한다() throws Exception {
                FolderPermissionRequestDTO request = new FolderPermissionRequestDTO();
                request.setPermission(PermissionType.WRITER);

                given(shareFolderService.updateViewerPermission(eq(1L), eq(FOLDER_ID), eq(USERS_FOLDER_ID), any()))
                        .willThrow(new GeneralException(ShareFolderErrorStatus._FOLDER_OWNER_UPDATE_NOT_ALLOWED));

                mockMvc.perform(put("/api/v1/folders/share/{folderId}/members/{userFolderId}", FOLDER_ID, USERS_FOLDER_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                                .with(csrf()))
                        .andExpect(status().isForbidden())
                        .andExpect(jsonPath("$.isSuccess").value(false))
                        .andExpect(jsonPath("$.code").value(ShareFolderErrorStatus._FOLDER_OWNER_UPDATE_NOT_ALLOWED.getCode()));
            }
        }
    }

    @Nested
    @DisplayName("POST /api/v1/folders/share/{folderId}/unshare - 폴더 비공개 전환")
    class UnshareFolder {

        @Nested
        @DisplayName("성공")
        class Success {
            @Test
            @DisplayName("정상 요청 시 비공개 전환 결과를 반환한다")
            @WithCustomUser(userId = 1L)
            void 정상_요청_시_비공개전환결과를_반환한다() throws Exception {
                ShareFolderResponseDTO response = ShareFolderResponseDTO.builder()
                        .folderId(FOLDER_ID).userId(1L).permission("PRIVATE").sharedAt("2026-01-01T00:00:00").build();

                given(shareFolderService.unshare(1L, FOLDER_ID)).willReturn(response);

                MvcResult result = mockMvc.perform(post("/api/v1/folders/share/{folderId}/unshare", FOLDER_ID).with(csrf()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.isSuccess").value(true))
                        .andReturn();

                ShareFolderResponseDTO unshared = readResult(result, objectMapper, ShareFolderResponseDTO.class);
                assertThat(unshared.getPermission()).isEqualTo("PRIVATE");
            }
        }
    }

    @Nested
    @DisplayName("POST /api/v1/folders/share/{folderId}/leave - 폴더 나가기")
    class LeaveFolder {

        @Nested
        @DisplayName("성공")
        class Success {
            @Test
            @DisplayName("정상 요청 시 소유권 위임 결과를 반환한다")
            @WithCustomUser(userId = 1L)
            void 정상_요청_시_소유권위임결과를_반환한다() throws Exception {
                ShareFolderResponseDTO response = ShareFolderResponseDTO.builder()
                        .folderId(FOLDER_ID).userId(5L).permission("OWNER").sharedAt("2026-01-01T00:00:00").build();

                given(shareFolderService.leaveFolder(1L, FOLDER_ID)).willReturn(response);

                MvcResult result = mockMvc.perform(post("/api/v1/folders/share/{folderId}/leave", FOLDER_ID).with(csrf()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.isSuccess").value(true))
                        .andReturn();

                ShareFolderResponseDTO left = readResult(result, objectMapper, ShareFolderResponseDTO.class);
                assertThat(left.getUserId()).isEqualTo(5L);
            }
        }

        @Nested
        @DisplayName("실패")
        class Failure {
            @Test
            @DisplayName("위임할 멤버가 없으면 409와 에러코드를 반환한다")
            @WithCustomUser(userId = 1L)
            void 위임할멤버가_없으면_409를_반환한다() throws Exception {
                given(shareFolderService.leaveFolder(1L, FOLDER_ID))
                        .willThrow(new GeneralException(ShareFolderErrorStatus._FOLDER_LEAVE_NO_MEMBER_TO_TRANSFER));

                mockMvc.perform(post("/api/v1/folders/share/{folderId}/leave", FOLDER_ID).with(csrf()))
                        .andExpect(status().isConflict())
                        .andExpect(jsonPath("$.isSuccess").value(false))
                        .andExpect(jsonPath("$.code").value(ShareFolderErrorStatus._FOLDER_LEAVE_NO_MEMBER_TO_TRANSFER.getCode()));
            }
        }
    }

    @Nested
    @DisplayName("GET /api/v1/folders/share/my - 내가 공유한 폴더 목록 조회")
    class GetMySharedFolders {

        @Nested
        @DisplayName("성공")
        class Success {
            @Test
            @DisplayName("정상 요청 시 공유한 폴더 목록을 반환한다")
            @WithCustomUser(userId = 1L)
            void 정상_요청_시_공유한폴더목록을_반환한다() throws Exception {
                MySharedFolderResponseDTO item = MySharedFolderResponseDTO.builder()
                        .folderId(FOLDER_ID).folderName("어학").memberCount(2).build();

                given(shareFolderService.getMySharedFolders(1L)).willReturn(List.of(item));

                MvcResult result = mockMvc.perform(get("/api/v1/folders/share/my"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.isSuccess").value(true))
                        .andExpect(jsonPath("$.code").value("FOLDER2019"))
                        .andReturn();

                List<MySharedFolderResponseDTO> folders = readResultList(result, objectMapper, MySharedFolderResponseDTO.class);
                assertThat(folders).hasSize(1);
                assertThat(folders.get(0).getMemberCount()).isEqualTo(2);
            }
        }
    }
}
