package com.umc.linkyou.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.linkyou.apiPayload.code.status.ErrorStatus;
import com.umc.linkyou.apiPayload.code.status.folder.FolderErrorStatus;
import com.umc.linkyou.apiPayload.exception.GeneralException;
import com.umc.linkyou.config.common.WebConfig;
import com.umc.linkyou.jwt.AccessTokenBlackListManager;
import com.umc.linkyou.jwt.CurrentUserArgumentResolver;
import com.umc.linkyou.jwt.JwtTokenProvider;
import com.umc.linkyou.jwt.SecurityErrorResponseWriter;
import com.umc.linkyou.service.folder.FolderService;
import com.umc.linkyou.support.security.TestSecurityConfig;
import com.umc.linkyou.support.security.WithCustomUser;
import com.umc.linkyou.web.dto.folder.BookmarkUpdateRequestDTO;
import com.umc.linkyou.web.dto.folder.BookmarkUpdateResponseDTO;
import com.umc.linkyou.web.dto.folder.FolderCreateRequestDTO;
import com.umc.linkyou.web.dto.folder.FolderListResponseDTO;
import com.umc.linkyou.web.dto.folder.FolderResponseDTO;
import com.umc.linkyou.web.dto.folder.FolderTreeResponseDTO;
import com.umc.linkyou.web.dto.folder.FolderUpdateRequestDTO;
import com.umc.linkyou.web.dto.folder.linku.FolderLinkusResponseDTO;
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
import static org.mockito.Mockito.doThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FolderController.class)
@Import({WebConfig.class, CurrentUserArgumentResolver.class, TestSecurityConfig.class})
@DisplayName("FolderController 테스트")
class FolderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private FolderService folderService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private AccessTokenBlackListManager accessTokenBlackListManager;

    @MockitoBean
    private SecurityErrorResponseWriter securityErrorResponseWriter;

    private static final Long PARENT_FOLDER_ID = 200L;
    private static final Long FOLDER_ID = 100L;

    @Nested
    @DisplayName("POST /api/v1/folders/{parentFolderId}/subfolders - 소분류 폴더 생성")
    class CreateFolder {

        @Nested
        @DisplayName("성공")
        class Success {
            @Test
            @DisplayName("정상 요청 시 생성된 폴더 정보를 반환한다")
            @WithCustomUser(userId = 1L)
            void 정상_요청_시_생성된_폴더정보를_반환한다() throws Exception {
                FolderCreateRequestDTO request = new FolderCreateRequestDTO();
                request.setFolderName("새폴더");

                FolderResponseDTO response = FolderResponseDTO.builder()
                        .folderId(FOLDER_ID).folderName("새폴더").parentFolderId(PARENT_FOLDER_ID).isBookmarked(false).build();

                given(folderService.createFolder(eq(1L), eq(PARENT_FOLDER_ID), any())).willReturn(response);

                MvcResult result = mockMvc.perform(post("/api/v1/folders/{parentFolderId}/subfolders", PARENT_FOLDER_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                                .with(csrf()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.isSuccess").value(true))
                        .andExpect(jsonPath("$.code").value("FOLDER2011"))
                        .andReturn();

                FolderResponseDTO created = readResult(result, objectMapper, FolderResponseDTO.class);
                assertThat(created.getFolderId()).isEqualTo(FOLDER_ID);
                assertThat(created.getFolderName()).isEqualTo("새폴더");
            }
        }

        @Nested
        @DisplayName("실패")
        class Failure {
            @Test
            @DisplayName("부모 폴더가 없으면 404와 에러코드를 반환한다")
            @WithCustomUser(userId = 1L)
            void 부모폴더가_없으면_404를_반환한다() throws Exception {
                FolderCreateRequestDTO request = new FolderCreateRequestDTO();
                request.setFolderName("새폴더");

                given(folderService.createFolder(eq(1L), eq(PARENT_FOLDER_ID), any()))
                        .willThrow(new GeneralException(FolderErrorStatus._FOLDER_PARENT_NOT_FOUND));

                mockMvc.perform(post("/api/v1/folders/{parentFolderId}/subfolders", PARENT_FOLDER_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                                .with(csrf()))
                        .andExpect(status().isNotFound())
                        .andExpect(jsonPath("$.isSuccess").value(false))
                        .andExpect(jsonPath("$.code").value(FolderErrorStatus._FOLDER_PARENT_NOT_FOUND.getCode()));
            }

            @Test
            @DisplayName("부모가 이미 소분류면 400과 에러코드를 반환한다")
            @WithCustomUser(userId = 1L)
            void 부모가_소분류면_400을_반환한다() throws Exception {
                FolderCreateRequestDTO request = new FolderCreateRequestDTO();
                request.setFolderName("새폴더");

                given(folderService.createFolder(eq(1L), eq(PARENT_FOLDER_ID), any()))
                        .willThrow(new GeneralException(FolderErrorStatus._FOLDER_MAX_DEPTH_EXCEEDED));

                mockMvc.perform(post("/api/v1/folders/{parentFolderId}/subfolders", PARENT_FOLDER_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                                .with(csrf()))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.isSuccess").value(false))
                        .andExpect(jsonPath("$.code").value(FolderErrorStatus._FOLDER_MAX_DEPTH_EXCEEDED.getCode()));
            }

            @Test
            @DisplayName("인증되지 않은 요청은 401을 반환한다")
            void 인증되지_않으면_401을_반환한다() throws Exception {
                FolderCreateRequestDTO request = new FolderCreateRequestDTO();
                request.setFolderName("새폴더");

                mockMvc.perform(post("/api/v1/folders/{parentFolderId}/subfolders", PARENT_FOLDER_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                                .with(csrf()))
                        .andExpect(status().isUnauthorized());
            }
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/folders/subfolders/{folderId} - 소분류 폴더 수정")
    class UpdateFolder {

        @Nested
        @DisplayName("성공")
        class Success {
            @Test
            @DisplayName("정상 요청 시 수정된 폴더 정보를 반환한다")
            @WithCustomUser(userId = 1L)
            void 정상_요청_시_수정된_폴더정보를_반환한다() throws Exception {
                FolderUpdateRequestDTO request = new FolderUpdateRequestDTO();
                request.setFolderName("수정된이름");

                FolderResponseDTO response = FolderResponseDTO.builder()
                        .folderId(FOLDER_ID).folderName("수정된이름").isBookmarked(true).build();

                given(folderService.updateFolder(eq(1L), eq(FOLDER_ID), any())).willReturn(response);

                MvcResult result = mockMvc.perform(put("/api/v1/folders/subfolders/{folderId}", FOLDER_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                                .with(csrf()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.isSuccess").value(true))
                        .andExpect(jsonPath("$.code").value("FOLDER2004"))
                        .andReturn();

                FolderResponseDTO updated = readResult(result, objectMapper, FolderResponseDTO.class);
                assertThat(updated.getFolderName()).isEqualTo("수정된이름");
            }
        }

        @Nested
        @DisplayName("실패")
        class Failure {
            @Test
            @DisplayName("수정 권한이 없으면 403과 에러코드를 반환한다")
            @WithCustomUser(userId = 1L)
            void 수정권한이_없으면_403을_반환한다() throws Exception {
                FolderUpdateRequestDTO request = new FolderUpdateRequestDTO();
                request.setFolderName("수정된이름");

                given(folderService.updateFolder(eq(1L), eq(FOLDER_ID), any()))
                        .willThrow(new GeneralException(FolderErrorStatus._FOLDER_UPDATE_FORBIDDEN));

                mockMvc.perform(put("/api/v1/folders/subfolders/{folderId}", FOLDER_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                                .with(csrf()))
                        .andExpect(status().isForbidden())
                        .andExpect(jsonPath("$.isSuccess").value(false))
                        .andExpect(jsonPath("$.code").value(FolderErrorStatus._FOLDER_UPDATE_FORBIDDEN.getCode()));
            }
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/folders/subfolders/{folderId} - 소분류 폴더 삭제")
    class DeleteFolder {

        @Nested
        @DisplayName("성공")
        class Success {
            @Test
            @DisplayName("정상 요청 시 삭제에 성공한다")
            @WithCustomUser(userId = 1L)
            void 정상_요청_시_삭제에_성공한다() throws Exception {
                mockMvc.perform(delete("/api/v1/folders/subfolders/{folderId}", FOLDER_ID).with(csrf()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.isSuccess").value(true))
                        .andExpect(jsonPath("$.code").value("FOLDER2005"));
            }
        }

        @Nested
        @DisplayName("실패")
        class Failure {
            @Test
            @DisplayName("삭제 권한이 없으면 403과 에러코드를 반환한다")
            @WithCustomUser(userId = 1L)
            void 삭제권한이_없으면_403을_반환한다() throws Exception {
                doThrow(new GeneralException(FolderErrorStatus._FOLDER_DELETE_FORBIDDEN))
                        .when(folderService).deleteFolder(eq(1L), eq(FOLDER_ID));

                mockMvc.perform(delete("/api/v1/folders/subfolders/{folderId}", FOLDER_ID).with(csrf()))
                        .andExpect(status().isForbidden())
                        .andExpect(jsonPath("$.isSuccess").value(false))
                        .andExpect(jsonPath("$.code").value(FolderErrorStatus._FOLDER_DELETE_FORBIDDEN.getCode()));
            }
        }
    }

    @Nested
    @DisplayName("GET /api/v1/folders/my - 내 폴더 트리 조회")
    class GetMyFolderTree {

        @Nested
        @DisplayName("성공")
        class Success {
            @Test
            @DisplayName("정상 요청 시 폴더 트리를 반환한다")
            @WithCustomUser(userId = 1L)
            void 정상_요청_시_폴더트리를_반환한다() throws Exception {
                FolderTreeResponseDTO tree = FolderTreeResponseDTO.builder()
                        .folderId(PARENT_FOLDER_ID).folderName("중분류").isBookmarked(false).build();

                given(folderService.getMyFolderTree(1L)).willReturn(List.of(tree));

                MvcResult result = mockMvc.perform(get("/api/v1/folders/my"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.isSuccess").value(true))
                        .andExpect(jsonPath("$.code").value("FOLDER2001"))
                        .andReturn();

                List<FolderTreeResponseDTO> tree2 = readResultList(result, objectMapper, FolderTreeResponseDTO.class);
                assertThat(tree2).hasSize(1);
                assertThat(tree2.get(0).getFolderId()).isEqualTo(PARENT_FOLDER_ID);
            }
        }

        @Nested
        @DisplayName("실패")
        class Failure {
            @Test
            @DisplayName("인증되지 않은 요청은 401을 반환한다")
            void 인증되지_않으면_401을_반환한다() throws Exception {
                mockMvc.perform(get("/api/v1/folders/my"))
                        .andExpect(status().isUnauthorized());
            }
        }
    }

    @Nested
    @DisplayName("GET /api/v1/folders/parentFolders - 중분류 폴더 조회")
    class GetParentFolderList {

        @Nested
        @DisplayName("성공")
        class Success {
            @Test
            @DisplayName("정상 요청 시 중분류 폴더 목록을 반환한다")
            @WithCustomUser(userId = 1L)
            void 정상_요청_시_중분류폴더목록을_반환한다() throws Exception {
                FolderListResponseDTO item = FolderListResponseDTO.builder()
                        .folderId(PARENT_FOLDER_ID).folderName("중분류").isBookmarked(false).isSharing("private").build();

                given(folderService.getParentFolders(1L, "name")).willReturn(List.of(item));

                MvcResult result = mockMvc.perform(get("/api/v1/folders/parentFolders"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.isSuccess").value(true))
                        .andReturn();

                List<FolderListResponseDTO> folders = readResultList(result, objectMapper, FolderListResponseDTO.class);
                assertThat(folders).hasSize(1);
                assertThat(folders.get(0).getIsSharing()).isEqualTo("private");
            }
        }
    }

    @Nested
    @DisplayName("GET /api/v1/folders/{parentFolderId}/subfolders - 하위 폴더 조회")
    class GetSubFolderList {

        @Nested
        @DisplayName("성공")
        class Success {
            @Test
            @DisplayName("정상 요청 시 하위 폴더 목록을 반환한다")
            @WithCustomUser(userId = 1L)
            void 정상_요청_시_하위폴더목록을_반환한다() throws Exception {
                FolderListResponseDTO item = FolderListResponseDTO.builder()
                        .folderId(FOLDER_ID).folderName("소분류").parentFolderId(PARENT_FOLDER_ID).isBookmarked(false).isSharing("private").build();

                given(folderService.getSubFolders(1L, PARENT_FOLDER_ID)).willReturn(List.of(item));

                MvcResult result = mockMvc.perform(get("/api/v1/folders/{parentFolderId}/subfolders", PARENT_FOLDER_ID))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.isSuccess").value(true))
                        .andReturn();

                List<FolderListResponseDTO> folders = readResultList(result, objectMapper, FolderListResponseDTO.class);
                assertThat(folders).hasSize(1);
                assertThat(folders.get(0).getFolderId()).isEqualTo(FOLDER_ID);
            }
        }

        @Nested
        @DisplayName("실패")
        class Failure {
            @Test
            @DisplayName("접근 권한이 없으면 403과 에러코드를 반환한다")
            @WithCustomUser(userId = 1L)
            void 접근권한이_없으면_403을_반환한다() throws Exception {
                given(folderService.getSubFolders(1L, PARENT_FOLDER_ID))
                        .willThrow(new GeneralException(FolderErrorStatus._FOLDER_ACCESS_FORBIDDEN));

                mockMvc.perform(get("/api/v1/folders/{parentFolderId}/subfolders", PARENT_FOLDER_ID))
                        .andExpect(status().isForbidden())
                        .andExpect(jsonPath("$.isSuccess").value(false))
                        .andExpect(jsonPath("$.code").value(FolderErrorStatus._FOLDER_ACCESS_FORBIDDEN.getCode()));
            }
        }
    }

    @Nested
    @DisplayName("PATCH /api/v1/folders/{folderId}/bookmark - 북마크 설정/해제")
    class UpdateBookmark {

        @Nested
        @DisplayName("성공")
        class Success {
            @Test
            @DisplayName("정상 요청 시 변경된 북마크 상태를 반환한다")
            @WithCustomUser(userId = 1L)
            void 정상_요청_시_북마크상태를_반환한다() throws Exception {
                BookmarkUpdateRequestDTO request = new BookmarkUpdateRequestDTO();
                request.setIsBookmarked(true);

                BookmarkUpdateResponseDTO response = BookmarkUpdateResponseDTO.builder()
                        .folderId(FOLDER_ID).isBookmarked(true).build();

                given(folderService.updateBookmark(1L, FOLDER_ID, true)).willReturn(response);

                MvcResult result = mockMvc.perform(patch("/api/v1/folders/{folderId}/bookmark", FOLDER_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                                .with(csrf()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.isSuccess").value(true))
                        .andReturn();

                BookmarkUpdateResponseDTO bookmark = readResult(result, objectMapper, BookmarkUpdateResponseDTO.class);
                assertThat(bookmark.getIsBookmarked()).isTrue();
            }
        }

        @Nested
        @DisplayName("실패")
        class Failure {
            @Test
            @DisplayName("유저-폴더 관계가 없으면 404와 에러코드를 반환한다")
            @WithCustomUser(userId = 1L)
            void 유저폴더관계가_없으면_404를_반환한다() throws Exception {
                BookmarkUpdateRequestDTO request = new BookmarkUpdateRequestDTO();
                request.setIsBookmarked(true);

                given(folderService.updateBookmark(1L, FOLDER_ID, true))
                        .willThrow(new GeneralException(ErrorStatus._FOLDER_BOOKMARK_NOT_FOUND));

                mockMvc.perform(patch("/api/v1/folders/{folderId}/bookmark", FOLDER_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                                .with(csrf()))
                        .andExpect(status().isNotFound())
                        .andExpect(jsonPath("$.isSuccess").value(false))
                        .andExpect(jsonPath("$.code").value(ErrorStatus._FOLDER_BOOKMARK_NOT_FOUND.getCode()));
            }
        }
    }

    @Nested
    @DisplayName("GET /api/v1/folders/{folderId}/linkus - 폴더 내부 링크/폴더 목록 조회")
    class GetFolderLinkus {

        @Nested
        @DisplayName("성공")
        class Success {
            @Test
            @DisplayName("정상 요청 시 링크와 하위 폴더 목록을 반환한다")
            @WithCustomUser(userId = 1L)
            void 정상_요청_시_링크와_하위폴더목록을_반환한다() throws Exception {
                FolderLinkusResponseDTO response = new FolderLinkusResponseDTO();
                response.setFolders(List.of());
                response.setLinks(List.of());
                response.setNextCursor(null);

                given(folderService.getFolderLinkus(1L, FOLDER_ID, 20, null, "name")).willReturn(response);

                MvcResult result = mockMvc.perform(get("/api/v1/folders/{folderId}/linkus", FOLDER_ID))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.isSuccess").value(true))
                        .andExpect(jsonPath("$.code").value("FOLDER2006"))
                        .andReturn();

                FolderLinkusResponseDTO linkus = readResult(result, objectMapper, FolderLinkusResponseDTO.class);
                assertThat(linkus.getFolders()).isEmpty();
                assertThat(linkus.getLinks()).isEmpty();
                assertThat(linkus.getNextCursor()).isNull();
            }
        }

        @Nested
        @DisplayName("실패")
        class Failure {
            @Test
            @DisplayName("커서가 유효하지 않으면 400과 에러코드를 반환한다")
            @WithCustomUser(userId = 1L)
            void 커서가_유효하지_않으면_400을_반환한다() throws Exception {
                given(folderService.getFolderLinkus(1L, FOLDER_ID, 20, "abc", "name"))
                        .willThrow(new GeneralException(FolderErrorStatus._FOLDER_INVALID_CURSOR));

                mockMvc.perform(get("/api/v1/folders/{folderId}/linkus", FOLDER_ID).param("cursor", "abc"))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.isSuccess").value(false))
                        .andExpect(jsonPath("$.code").value(FolderErrorStatus._FOLDER_INVALID_CURSOR.getCode()));
            }
        }
    }
}
