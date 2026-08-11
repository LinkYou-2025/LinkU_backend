package com.umc.linkyou.service.folder;

import com.umc.linkyou.apiPayload.code.status.ErrorStatus;
import com.umc.linkyou.apiPayload.code.status.folder.FolderErrorStatus;
import com.umc.linkyou.apiPayload.exception.GeneralException;
import com.umc.linkyou.domain.enums.PermissionType;
import com.umc.linkyou.domain.folder.Folder;
import com.umc.linkyou.domain.mapping.folder.UsersFolder;
import com.umc.linkyou.repository.FolderRepository.FolderRepository;
import com.umc.linkyou.repository.classification.CategoryRepository;
import com.umc.linkyou.repository.mapping.linkuFolderRepository.LinkuFolderRepository;
import com.umc.linkyou.repository.userRepository.UserRepository;
import com.umc.linkyou.repository.usersFolderRepository.UsersFolderRepository;
import com.umc.linkyou.service.alarm.event.FolderDeletedAlarmEvent;
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
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Sort;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.umc.linkyou.support.fixture.FolderFixture.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FolderService 단위 테스트")
class FolderServiceTest {
    @InjectMocks private FolderServiceImpl folderService;

    @Mock private UserRepository userRepository;
    @Mock private FolderRepository folderRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private UsersFolderRepository usersFolderRepository;
    @Mock private LinkuFolderRepository linkuFolderRepository;
    @Mock private ApplicationEventPublisher eventPublisher;

    @Nested
    @DisplayName("폴더 삭제")
    class DeleteFolder {
        @Nested
        @DisplayName("성공")
        class Success {
            @Test
            @DisplayName("공유 멤버가 있으면 owner를 제외한 memberIds로 알람 이벤트를 발행한다")
            void 공유멤버존재_이벤트발행() {
                Folder folder = folder();

                given(folderRepository.findById(FOLDER_ID)).willReturn(Optional.of(folder));
                given(usersFolderRepository.existsFolderOwner(OWNER_ID, FOLDER_ID)).willReturn(true);
                given(usersFolderRepository.findAllParticipantsByFolderId(FOLDER_ID)).willReturn(List.of(
                        participant(OWNER_ID, PermissionType.OWNER),
                        participant(2L, PermissionType.VIEWER),
                        participant(3L, PermissionType.WRITER)
                ));
                given(userRepository.findById(OWNER_ID)).willReturn(Optional.of(owner()));

                folderService.deleteFolder(OWNER_ID, FOLDER_ID);
                verify(folderRepository).delete(folder);

                ArgumentCaptor<FolderDeletedAlarmEvent> captor = ArgumentCaptor.forClass(FolderDeletedAlarmEvent.class);
                verify(eventPublisher).publishEvent(captor.capture());
                FolderDeletedAlarmEvent event = captor.getValue();
                assertThat(event.folderId()).isEqualTo(FOLDER_ID);
                assertThat(event.memberIds()).containsExactlyInAnyOrder(2L, 3L);
                assertThat(event.deleterNickname()).isEqualTo("주인");
                assertThat(event.folderName()).isEqualTo("어학");
            }

            @Test
            @DisplayName("공유 멤버가 없으면(본인뿐이면) 알람 이벤트를 발행하지 않는다")
            void 공유멤버없음_이벤트미발행() {
                Folder folder = folder();

                given(folderRepository.findById(FOLDER_ID)).willReturn(Optional.of(folder));
                given(usersFolderRepository.existsFolderOwner(OWNER_ID, FOLDER_ID)).willReturn(true);
                given(usersFolderRepository.findAllParticipantsByFolderId(FOLDER_ID)).willReturn(List.of(
                        participant(OWNER_ID, PermissionType.OWNER)
                ));
                given(userRepository.findById(OWNER_ID)).willReturn(Optional.of(owner()));

                folderService.deleteFolder(OWNER_ID, FOLDER_ID);

                verify(folderRepository).delete(folder);
                verify(eventPublisher, never()).publishEvent(any());
            }
        }

        @Nested
        @DisplayName("실패")
        class Failure {
            @Test
            @DisplayName("존재하지 않는 폴더면 _FOLDER_NOT_FOUND를 던진다")
            void 폴더없음_예외() {
                given(folderRepository.findById(FOLDER_ID)).willReturn(Optional.empty());

                assertThatThrownBy(() -> folderService.deleteFolder(OWNER_ID, FOLDER_ID))
                        .isInstanceOf(GeneralException.class)
                        .satisfies(ex -> assertThat(((GeneralException) ex).getCode())
                                .isEqualTo(FolderErrorStatus._FOLDER_NOT_FOUND));
            }

            @Test
            @DisplayName("소유자가 아니면 _FOLDER_DELETE_FORBIDDEN을 던지고, 알람도 발행하지 않는다")
            void 소유자아님_예외() {
                given(folderRepository.findById(FOLDER_ID)).willReturn(Optional.of(folder()));
                given(usersFolderRepository.existsFolderOwner(OWNER_ID, FOLDER_ID)).willReturn(false);

                assertThatThrownBy(() -> folderService.deleteFolder(OWNER_ID, FOLDER_ID))
                        .isInstanceOf(GeneralException.class)
                        .satisfies(ex -> assertThat(((GeneralException) ex).getCode())
                                .isEqualTo(FolderErrorStatus._FOLDER_DELETE_FORBIDDEN));

                verify(folderRepository, never()).delete(any());
                verify(eventPublisher, never()).publishEvent(any());
            }
        }
    }

    @Nested
    @DisplayName("소분류 폴더 생성")
    class CreateFolder {
        private static final String NEW_FOLDER_NAME = "새폴더";

        private FolderCreateRequestDTO request() {
            FolderCreateRequestDTO req = new FolderCreateRequestDTO();
            req.setFolderName(NEW_FOLDER_NAME);
            return req;
        }

        @Nested
        @DisplayName("성공")
        class Success {
            @Test
            @DisplayName("소유자 또는 편집자가 생성하면 소분류 폴더가 생성된다")
            void 정상_요청_시_폴더가_생성된다() {
                Folder parent = parentFolder();

                given(folderRepository.findById(PARENT_FOLDER_ID)).willReturn(Optional.of(parent));
                given(usersFolderRepository.existsFolderOwnerOrWriter(OWNER_ID, PARENT_FOLDER_ID)).willReturn(true);
                given(categoryRepository.existsByCategoryName(NEW_FOLDER_NAME)).willReturn(false);
                given(folderRepository.existsByParentIdAndName(PARENT_FOLDER_ID, NEW_FOLDER_NAME)).willReturn(false);
                given(userRepository.findById(OWNER_ID)).willReturn(Optional.of(owner()));

                FolderResponseDTO result = folderService.createFolder(OWNER_ID, PARENT_FOLDER_ID, request());

                assertThat(result.getFolderName()).isEqualTo(NEW_FOLDER_NAME);
                assertThat(result.getCategoryId()).isEqualTo(CATEGORY_ID);
                assertThat(result.getParentFolderId()).isEqualTo(PARENT_FOLDER_ID);
                assertThat(result.getIsBookmarked()).isFalse();
                verify(usersFolderRepository).save(any());
            }
        }

        @Nested
        @DisplayName("실패")
        class Failure {
            @Test
            @DisplayName("부모 폴더가 없으면 _FOLDER_PARENT_NOT_FOUND를 던진다")
            void 부모폴더없음_예외() {
                given(folderRepository.findById(PARENT_FOLDER_ID)).willReturn(Optional.empty());

                assertThatThrownBy(() -> folderService.createFolder(OWNER_ID, PARENT_FOLDER_ID, request()))
                        .isInstanceOf(GeneralException.class)
                        .satisfies(ex -> assertThat(((GeneralException) ex).getCode())
                                .isEqualTo(FolderErrorStatus._FOLDER_PARENT_NOT_FOUND));
            }

            @Test
            @DisplayName("부모가 이미 소분류(3단계 이상)면 _FOLDER_MAX_DEPTH_EXCEEDED를 던진다")
            void 부모가_소분류이면_예외() {
                Folder grandParent = parentFolder();
                Folder subFolderAsParent = subFolder(grandParent);

                given(folderRepository.findById(FOLDER_ID)).willReturn(Optional.of(subFolderAsParent));
                given(usersFolderRepository.existsFolderOwnerOrWriter(OWNER_ID, FOLDER_ID)).willReturn(true);

                assertThatThrownBy(() -> folderService.createFolder(OWNER_ID, FOLDER_ID, request()))
                        .isInstanceOf(GeneralException.class)
                        .satisfies(ex -> assertThat(((GeneralException) ex).getCode())
                                .isEqualTo(FolderErrorStatus._FOLDER_MAX_DEPTH_EXCEEDED));
            }

            @Test
            @DisplayName("생성 권한(소유자/편집자)이 없으면 _FOLDER_CREATE_FORBIDDEN을 던진다")
            void 생성권한없음_예외() {
                given(folderRepository.findById(PARENT_FOLDER_ID)).willReturn(Optional.of(parentFolder()));
                given(usersFolderRepository.existsFolderOwnerOrWriter(OWNER_ID, PARENT_FOLDER_ID)).willReturn(false);

                assertThatThrownBy(() -> folderService.createFolder(OWNER_ID, PARENT_FOLDER_ID, request()))
                        .isInstanceOf(GeneralException.class)
                        .satisfies(ex -> assertThat(((GeneralException) ex).getCode())
                                .isEqualTo(FolderErrorStatus._FOLDER_CREATE_FORBIDDEN));
            }

            @Test
            @DisplayName("카테고리명과 동일한 이름이면 _FOLDER_NAME_CONFLICT를 던진다")
            void 카테고리명중복_예외() {
                given(folderRepository.findById(PARENT_FOLDER_ID)).willReturn(Optional.of(parentFolder()));
                given(usersFolderRepository.existsFolderOwnerOrWriter(OWNER_ID, PARENT_FOLDER_ID)).willReturn(true);
                given(categoryRepository.existsByCategoryName(NEW_FOLDER_NAME)).willReturn(true);

                assertThatThrownBy(() -> folderService.createFolder(OWNER_ID, PARENT_FOLDER_ID, request()))
                        .isInstanceOf(GeneralException.class)
                        .satisfies(ex -> assertThat(((GeneralException) ex).getCode())
                                .isEqualTo(FolderErrorStatus._FOLDER_NAME_CONFLICT));
            }

            @Test
            @DisplayName("같은 부모 아래 동일한 이름이 있으면 _FOLDER_CREATE_DUPLICATE를 던진다")
            void 폴더명중복_예외() {
                given(folderRepository.findById(PARENT_FOLDER_ID)).willReturn(Optional.of(parentFolder()));
                given(usersFolderRepository.existsFolderOwnerOrWriter(OWNER_ID, PARENT_FOLDER_ID)).willReturn(true);
                given(categoryRepository.existsByCategoryName(NEW_FOLDER_NAME)).willReturn(false);
                given(folderRepository.existsByParentIdAndName(PARENT_FOLDER_ID, NEW_FOLDER_NAME)).willReturn(true);

                assertThatThrownBy(() -> folderService.createFolder(OWNER_ID, PARENT_FOLDER_ID, request()))
                        .isInstanceOf(GeneralException.class)
                        .satisfies(ex -> assertThat(((GeneralException) ex).getCode())
                                .isEqualTo(FolderErrorStatus._FOLDER_CREATE_DUPLICATE));
            }
        }
    }

    @Nested
    @DisplayName("소분류 폴더 수정")
    class UpdateFolder {
        private static final String RENAMED = "수정된이름";

        private FolderUpdateRequestDTO request() {
            FolderUpdateRequestDTO req = new FolderUpdateRequestDTO();
            req.setFolderName(RENAMED);
            return req;
        }

        @Nested
        @DisplayName("성공")
        class Success {
            @Test
            @DisplayName("소유자가 정상 이름으로 수정하면 변경된 폴더 정보를 반환한다")
            void 정상_요청_시_폴더명이_수정된다() {
                Folder parent = parentFolder();
                Folder folder = subFolder(parent);
                UsersFolder usersFolder = UsersFolder.builder()
                        .user(owner()).folder(folder).permissionType(PermissionType.OWNER).isBookmarked(true).build();

                given(folderRepository.findById(FOLDER_ID)).willReturn(Optional.of(folder));
                given(usersFolderRepository.findByUserIdAndFolderId(OWNER_ID, FOLDER_ID)).willReturn(Optional.of(usersFolder));
                given(categoryRepository.existsByCategoryName(RENAMED)).willReturn(false);
                given(folderRepository.existsByParentIdAndName(PARENT_FOLDER_ID, RENAMED)).willReturn(false);

                FolderResponseDTO result = folderService.updateFolder(OWNER_ID, FOLDER_ID, request());

                assertThat(result.getFolderName()).isEqualTo(RENAMED);
                assertThat(result.getIsBookmarked()).isTrue();
            }
        }

        @Nested
        @DisplayName("실패")
        class Failure {
            @Test
            @DisplayName("존재하지 않는 폴더면 _FOLDER_NOT_FOUND를 던진다")
            void 폴더없음_예외() {
                given(folderRepository.findById(FOLDER_ID)).willReturn(Optional.empty());

                assertThatThrownBy(() -> folderService.updateFolder(OWNER_ID, FOLDER_ID, request()))
                        .isInstanceOf(GeneralException.class)
                        .satisfies(ex -> assertThat(((GeneralException) ex).getCode())
                                .isEqualTo(FolderErrorStatus._FOLDER_NOT_FOUND));
            }

            @Test
            @DisplayName("유저-폴더 관계가 없으면 _FOLDER_UPDATE_FORBIDDEN을 던진다")
            void 유저폴더관계없음_예외() {
                given(folderRepository.findById(FOLDER_ID)).willReturn(Optional.of(subFolder(parentFolder())));
                given(usersFolderRepository.findByUserIdAndFolderId(OWNER_ID, FOLDER_ID)).willReturn(Optional.empty());

                assertThatThrownBy(() -> folderService.updateFolder(OWNER_ID, FOLDER_ID, request()))
                        .isInstanceOf(GeneralException.class)
                        .satisfies(ex -> assertThat(((GeneralException) ex).getCode())
                                .isEqualTo(FolderErrorStatus._FOLDER_UPDATE_FORBIDDEN));
            }

            @Test
            @DisplayName("소유자가 아니면 _FOLDER_UPDATE_FORBIDDEN을 던진다")
            void 소유자아님_예외() {
                Folder folder = subFolder(parentFolder());
                UsersFolder usersFolder = UsersFolder.builder()
                        .user(owner()).folder(folder).permissionType(PermissionType.VIEWER).build();

                given(folderRepository.findById(FOLDER_ID)).willReturn(Optional.of(folder));
                given(usersFolderRepository.findByUserIdAndFolderId(OWNER_ID, FOLDER_ID)).willReturn(Optional.of(usersFolder));

                assertThatThrownBy(() -> folderService.updateFolder(OWNER_ID, FOLDER_ID, request()))
                        .isInstanceOf(GeneralException.class)
                        .satisfies(ex -> assertThat(((GeneralException) ex).getCode())
                                .isEqualTo(FolderErrorStatus._FOLDER_UPDATE_FORBIDDEN));
            }

            @Test
            @DisplayName("중분류(부모 없는) 폴더면 _FOLDER_PARENT_NOT_FOUND를 던진다")
            void 부모폴더없음_예외() {
                Folder rootFolder = parentFolder();
                UsersFolder usersFolder = UsersFolder.builder()
                        .user(owner()).folder(rootFolder).permissionType(PermissionType.OWNER).build();

                given(folderRepository.findById(PARENT_FOLDER_ID)).willReturn(Optional.of(rootFolder));
                given(usersFolderRepository.findByUserIdAndFolderId(OWNER_ID, PARENT_FOLDER_ID)).willReturn(Optional.of(usersFolder));

                assertThatThrownBy(() -> folderService.updateFolder(OWNER_ID, PARENT_FOLDER_ID, request()))
                        .isInstanceOf(GeneralException.class)
                        .satisfies(ex -> assertThat(((GeneralException) ex).getCode())
                                .isEqualTo(FolderErrorStatus._FOLDER_PARENT_NOT_FOUND));
            }

            @Test
            @DisplayName("같은 부모 아래 동일한 이름이 있으면 _FOLDER_CREATE_DUPLICATE를 던진다")
            void 폴더명중복_예외() {
                Folder parent = parentFolder();
                Folder folder = subFolder(parent);
                UsersFolder usersFolder = UsersFolder.builder()
                        .user(owner()).folder(folder).permissionType(PermissionType.OWNER).build();

                given(folderRepository.findById(FOLDER_ID)).willReturn(Optional.of(folder));
                given(usersFolderRepository.findByUserIdAndFolderId(OWNER_ID, FOLDER_ID)).willReturn(Optional.of(usersFolder));
                given(categoryRepository.existsByCategoryName(RENAMED)).willReturn(false);
                given(folderRepository.existsByParentIdAndName(PARENT_FOLDER_ID, RENAMED)).willReturn(true);

                assertThatThrownBy(() -> folderService.updateFolder(OWNER_ID, FOLDER_ID, request()))
                        .isInstanceOf(GeneralException.class)
                        .satisfies(ex -> assertThat(((GeneralException) ex).getCode())
                                .isEqualTo(FolderErrorStatus._FOLDER_CREATE_DUPLICATE));
            }
        }
    }

    @Nested
    @DisplayName("내 폴더 목록(트리) 조회(getMyFolderTree)")
    class GetMyFolderTree {
        @Test
        @DisplayName("중분류 폴더 하위에 소분류 폴더를 트리 형태로 구성하여 반환한다")
        void 정상_조회_시_트리구조로_반환한다() {
            Folder parent = parentFolder();
            Folder child = subFolder(parent);

            UsersFolder parentUf = UsersFolder.builder().user(owner()).folder(parent).permissionType(PermissionType.OWNER).isBookmarked(false).build();
            UsersFolder childUf = UsersFolder.builder().user(owner()).folder(child).permissionType(PermissionType.OWNER).isBookmarked(true).build();

            given(usersFolderRepository.findAllByUserId(OWNER_ID)).willReturn(List.of(parentUf, childUf));

            List<FolderTreeResponseDTO> result = folderService.getMyFolderTree(OWNER_ID);

            assertThat(result).hasSize(1);
            FolderTreeResponseDTO root = result.get(0);
            assertThat(root.getFolderId()).isEqualTo(PARENT_FOLDER_ID);
            assertThat(root.getChildren()).hasSize(1);
            assertThat(root.getChildren().get(0).getFolderId()).isEqualTo(FOLDER_ID);
            assertThat(root.getChildren().get(0).getIsBookmarked()).isTrue();
        }
    }

    @Nested
    @DisplayName("중분류 폴더 조회(getParentFolders)")
    class GetParentFolders {
        @Test
        @DisplayName("공유 중인 폴더는 isSharing이 share로 표시된다")
        void 공유폴더_isSharing_share() {
            Folder parent = parentFolder();
            UsersFolder uf = UsersFolder.builder().user(owner()).folder(parent).permissionType(PermissionType.OWNER).isBookmarked(false).build();

            given(usersFolderRepository.findParentFolders(OWNER_ID)).willReturn(List.of(uf));
            given(usersFolderRepository.findAllSharedFolderIdsIn(List.of(PARENT_FOLDER_ID))).willReturn(Set.of(PARENT_FOLDER_ID));

            List<FolderListResponseDTO> result = folderService.getParentFolders(OWNER_ID, "name");

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getIsSharing()).isEqualTo("share");
        }

        @Test
        @DisplayName("공유 중이지 않은 폴더는 isSharing이 private로 표시된다")
        void 비공유폴더_isSharing_private() {
            Folder parent = parentFolder();
            UsersFolder uf = UsersFolder.builder().user(owner()).folder(parent).permissionType(PermissionType.OWNER).isBookmarked(false).build();

            given(usersFolderRepository.findParentFolders(OWNER_ID)).willReturn(List.of(uf));
            given(usersFolderRepository.findAllSharedFolderIdsIn(List.of(PARENT_FOLDER_ID))).willReturn(Collections.emptySet());

            List<FolderListResponseDTO> result = folderService.getParentFolders(OWNER_ID, "name");

            assertThat(result.get(0).getIsSharing()).isEqualTo("private");
        }

        @Test
        @DisplayName("응답에 categoryId가 포함된다")
        void 응답에_categoryId가_포함된다() {
            Folder parent = parentFolder();
            UsersFolder uf = UsersFolder.builder().user(owner()).folder(parent).permissionType(PermissionType.OWNER).isBookmarked(false).build();

            given(usersFolderRepository.findParentFolders(OWNER_ID)).willReturn(List.of(uf));
            given(usersFolderRepository.findAllSharedFolderIdsIn(List.of(PARENT_FOLDER_ID))).willReturn(Collections.emptySet());

            List<FolderListResponseDTO> result = folderService.getParentFolders(OWNER_ID, "name");

            assertThat(result.get(0).getCategoryId()).isEqualTo(CATEGORY_ID);
        }
    }

    @Nested
    @DisplayName("자식 폴더 목록 조회(getSubFolders)")
    class GetSubFolders {
        @Test
        @DisplayName("자식 폴더가 없으면 빈 리스트를 반환한다")
        void 자식폴더없음_빈리스트반환() {
            given(usersFolderRepository.findAllByUserIdAndParentFolderId(OWNER_ID, PARENT_FOLDER_ID)).willReturn(List.of());

            List<FolderListResponseDTO> result = folderService.getSubFolders(OWNER_ID, PARENT_FOLDER_ID);

            assertThat(result).isEmpty();
            verify(usersFolderRepository, never()).findAllSharedFolderIdsIn(any());
        }

        @Test
        @DisplayName("자식 폴더가 있으면 parentFolderId를 포함해 목록을 반환한다")
        void 자식폴더존재_목록반환() {
            Folder parent = parentFolder();
            Folder child = subFolder(parent);
            UsersFolder uf = UsersFolder.builder().user(owner()).folder(child).permissionType(PermissionType.OWNER).isBookmarked(false).build();

            given(usersFolderRepository.findAllByUserIdAndParentFolderId(OWNER_ID, PARENT_FOLDER_ID)).willReturn(List.of(uf));
            given(usersFolderRepository.findAllSharedFolderIdsIn(List.of(FOLDER_ID))).willReturn(Collections.emptySet());

            List<FolderListResponseDTO> result = folderService.getSubFolders(OWNER_ID, PARENT_FOLDER_ID);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getFolderId()).isEqualTo(FOLDER_ID);
            assertThat(result.get(0).getParentFolderId()).isEqualTo(PARENT_FOLDER_ID);
            assertThat(result.get(0).getIsSharing()).isEqualTo("private");
        }
    }

    @Nested
    @DisplayName("북마크 설정/해제(updateBookmark)")
    class UpdateBookmark {
        @Nested
        @DisplayName("성공")
        class Success {
            @Test
            @DisplayName("정상 요청 시 북마크 상태를 변경한다")
            void 정상_요청_시_북마크상태가_변경된다() {
                UsersFolder usersFolder = UsersFolder.builder().user(owner()).folder(folder()).permissionType(PermissionType.OWNER).isBookmarked(false).build();
                given(usersFolderRepository.findByUserIdAndFolderId(OWNER_ID, FOLDER_ID)).willReturn(Optional.of(usersFolder));

                BookmarkUpdateResponseDTO result = folderService.updateBookmark(OWNER_ID, FOLDER_ID, true);

                assertThat(result.getFolderId()).isEqualTo(FOLDER_ID);
                assertThat(result.getIsBookmarked()).isTrue();
            }
        }

        @Nested
        @DisplayName("실패")
        class Failure {
            @Test
            @DisplayName("유저-폴더 관계가 없으면 _FOLDER_BOOKMARK_NOT_FOUND를 던진다")
            void 유저폴더관계없음_예외() {
                given(usersFolderRepository.findByUserIdAndFolderId(OWNER_ID, FOLDER_ID)).willReturn(Optional.empty());

                assertThatThrownBy(() -> folderService.updateBookmark(OWNER_ID, FOLDER_ID, true))
                        .isInstanceOf(GeneralException.class)
                        .satisfies(ex -> assertThat(((GeneralException) ex).getCode())
                                .isEqualTo(ErrorStatus._FOLDER_BOOKMARK_NOT_FOUND));
            }

            @Test
            @DisplayName("공유 해제 등으로 권한이 NONE이 된 경우 _FOLDER_BOOKMARK_NOT_FOUND를 던진다")
            void 권한이_NONE이면_예외() {
                UsersFolder revokedUsersFolder = UsersFolder.builder()
                        .user(owner()).folder(folder()).permissionType(PermissionType.NONE).isBookmarked(false).build();
                given(usersFolderRepository.findByUserIdAndFolderId(OWNER_ID, FOLDER_ID)).willReturn(Optional.of(revokedUsersFolder));

                assertThatThrownBy(() -> folderService.updateBookmark(OWNER_ID, FOLDER_ID, true))
                        .isInstanceOf(GeneralException.class)
                        .satisfies(ex -> assertThat(((GeneralException) ex).getCode())
                                .isEqualTo(ErrorStatus._FOLDER_BOOKMARK_NOT_FOUND));
            }
        }
    }

    @Nested
    @DisplayName("폴더 내부 링크/폴더 목록 조회(getFolderLinkus)")
    class GetFolderLinkus {
        @Nested
        @DisplayName("성공")
        class Success {
            @Test
            @DisplayName("하위 폴더와 링크가 없으면 빈 목록과 null 커서를 반환한다")
            void 하위폴더와_링크가_없으면_빈목록을_반환한다() {
                given(folderRepository.findById(FOLDER_ID)).willReturn(Optional.of(folder()));
                given(usersFolderRepository.existsFolderOwner(OWNER_ID, FOLDER_ID)).willReturn(true);
                given(folderRepository.findAllByParentFolderId(eq(FOLDER_ID), any(Sort.class))).willReturn(List.of());
                given(linkuFolderRepository.findWithCursor(eq(FOLDER_ID), eq(Long.MAX_VALUE), any())).willReturn(List.of());

                FolderLinkusResponseDTO result = folderService.getFolderLinkus(OWNER_ID, FOLDER_ID, 20, null, "name");

                assertThat(result.getFolders()).isEmpty();
                assertThat(result.getLinks()).isEmpty();
                assertThat(result.getNextCursor()).isNull();
            }

            @Test
            @DisplayName("소유자가 아니어도 활성 공유 멤버면 조회할 수 있다")
            void 활성공유멤버는_조회할_수_있다() {
                given(folderRepository.findById(FOLDER_ID)).willReturn(Optional.of(folder()));
                given(usersFolderRepository.existsFolderOwner(OWNER_ID, FOLDER_ID)).willReturn(false);
                given(usersFolderRepository.existsActiveMember(OWNER_ID, FOLDER_ID)).willReturn(true);
                given(folderRepository.findAllByParentFolderId(eq(FOLDER_ID), any(Sort.class))).willReturn(List.of());
                given(linkuFolderRepository.findWithCursor(eq(FOLDER_ID), eq(Long.MAX_VALUE), any())).willReturn(List.of());

                FolderLinkusResponseDTO result = folderService.getFolderLinkus(OWNER_ID, FOLDER_ID, 20, null, "name");

                assertThat(result.getFolders()).isEmpty();
            }
        }

        @Nested
        @DisplayName("실패")
        class Failure {
            @Test
            @DisplayName("존재하지 않는 폴더면 _FOLDER_NOT_FOUND를 던진다")
            void 폴더없음_예외() {
                given(folderRepository.findById(FOLDER_ID)).willReturn(Optional.empty());

                assertThatThrownBy(() -> folderService.getFolderLinkus(OWNER_ID, FOLDER_ID, 20, null, "name"))
                        .isInstanceOf(GeneralException.class)
                        .satisfies(ex -> assertThat(((GeneralException) ex).getCode())
                                .isEqualTo(FolderErrorStatus._FOLDER_NOT_FOUND));
            }

            @Test
            @DisplayName("소유자도 활성 멤버도 아니면 _FOLDER_ACCESS_FORBIDDEN을 던진다")
            void 접근권한없음_예외() {
                given(folderRepository.findById(FOLDER_ID)).willReturn(Optional.of(folder()));
                given(usersFolderRepository.existsFolderOwner(OWNER_ID, FOLDER_ID)).willReturn(false);
                given(usersFolderRepository.existsActiveMember(OWNER_ID, FOLDER_ID)).willReturn(false);

                assertThatThrownBy(() -> folderService.getFolderLinkus(OWNER_ID, FOLDER_ID, 20, null, "name"))
                        .isInstanceOf(GeneralException.class)
                        .satisfies(ex -> assertThat(((GeneralException) ex).getCode())
                                .isEqualTo(FolderErrorStatus._FOLDER_ACCESS_FORBIDDEN));
            }

            @Test
            @DisplayName("커서가 숫자로 변환되지 않으면 _FOLDER_INVALID_CURSOR를 던진다")
            void 잘못된커서_예외() {
                given(folderRepository.findById(FOLDER_ID)).willReturn(Optional.of(folder()));
                given(usersFolderRepository.existsFolderOwner(OWNER_ID, FOLDER_ID)).willReturn(true);
                given(folderRepository.findAllByParentFolderId(eq(FOLDER_ID), any(Sort.class))).willReturn(List.of());

                assertThatThrownBy(() -> folderService.getFolderLinkus(OWNER_ID, FOLDER_ID, 20, "abc", "name"))
                        .isInstanceOf(GeneralException.class)
                        .satisfies(ex -> assertThat(((GeneralException) ex).getCode())
                                .isEqualTo(FolderErrorStatus._FOLDER_INVALID_CURSOR));
            }
        }
    }
}