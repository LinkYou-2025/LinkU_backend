package com.umc.linkyou.service.folder.shared;

import com.umc.linkyou.apiPayload.code.status.folder.FolderErrorStatus;
import com.umc.linkyou.apiPayload.exception.GeneralException;
import com.umc.linkyou.domain.enums.PermissionType;
import com.umc.linkyou.domain.folder.Folder;
import com.umc.linkyou.domain.mapping.folder.UsersFolder;
import com.umc.linkyou.repository.usersFolderRepository.UsersFolderRepository;
import com.umc.linkyou.web.dto.folder.share.SharedFolderGroupResponseDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static com.umc.linkyou.support.fixture.FolderFixture.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SharedFolderService 단위 테스트")
class SharedFolderServiceTest {
    @InjectMocks private SharedFolderServiceImpl sharedFolderService;

    @Mock private UsersFolderRepository usersFolderRepository;

    private static final Long VIEWER_ID = 5L;

    @Nested
    @DisplayName("공유 받은 폴더 목록 조회(getSharedFolders)")
    class GetSharedFolders {
        @Nested
        @DisplayName("성공")
        class Success {
            @Test
            @DisplayName("공유 받은 폴더가 없으면 빈 리스트를 반환한다")
            void 공유받은폴더없음_빈리스트반환() {
                given(usersFolderRepository.findAllSharedFolders(VIEWER_ID)).willReturn(List.of());

                List<SharedFolderGroupResponseDTO> result = sharedFolderService.getSharedFolders(VIEWER_ID);

                assertThat(result).isEmpty();
            }

            @Test
            @DisplayName("공유 받은 폴더가 있으면 소유자별로 그룹핑하여 반환한다")
            void 공유받은폴더존재_소유자별그룹핑반환() {
                Folder sharedFolder = folder();
                UsersFolder ownerMapping = UsersFolder.builder().user(owner()).folder(sharedFolder).permissionType(PermissionType.OWNER).build();

                given(usersFolderRepository.findAllSharedFolders(VIEWER_ID)).willReturn(List.of(sharedFolder));
                given(usersFolderRepository.findAllByUserIdAndFolderIdIn(VIEWER_ID, List.of(FOLDER_ID))).willReturn(List.of());
                given(usersFolderRepository.findOwnersByFolderIdIn(List.of(FOLDER_ID))).willReturn(List.of(ownerMapping));

                List<SharedFolderGroupResponseDTO> result = sharedFolderService.getSharedFolders(VIEWER_ID);

                assertThat(result).hasSize(1);
                assertThat(result.get(0).getUserId()).isEqualTo(OWNER_ID);
                assertThat(result.get(0).getNickname()).isEqualTo("주인");
                assertThat(result.get(0).getFolders()).hasSize(1);
                assertThat(result.get(0).getFolders().get(0).getFolderId()).isEqualTo(FOLDER_ID);
            }
        }

        @Nested
        @DisplayName("실패")
        class Failure {
            @Test
            @DisplayName("폴더 소유자 정보를 찾을 수 없으면 _FOLDER_OWNER_NOT_FOUND를 던진다")
            void 소유자정보없음_예외() {
                Folder sharedFolder = folder();

                given(usersFolderRepository.findAllSharedFolders(VIEWER_ID)).willReturn(List.of(sharedFolder));
                given(usersFolderRepository.findAllByUserIdAndFolderIdIn(VIEWER_ID, List.of(FOLDER_ID))).willReturn(List.of());
                given(usersFolderRepository.findOwnersByFolderIdIn(List.of(FOLDER_ID))).willReturn(List.of());

                assertThatThrownBy(() -> sharedFolderService.getSharedFolders(VIEWER_ID))
                        .isInstanceOf(GeneralException.class)
                        .satisfies(ex -> assertThat(((GeneralException) ex).getCode())
                                .isEqualTo(FolderErrorStatus._FOLDER_OWNER_NOT_FOUND));
            }
        }
    }

    @Nested
    @DisplayName("공유 받은 폴더 삭제(deleteSharedFolder)")
    class DeleteSharedFolder {
        @Nested
        @DisplayName("성공")
        class Success {
            @Test
            @DisplayName("소유자가 아닌 멤버는 자신의 공유 폴더 목록에서 제거할 수 있다")
            void 정상_요청_시_공유폴더에서_제거된다() {
                UsersFolder viewerMapping = participant(VIEWER_ID, PermissionType.VIEWER);
                given(usersFolderRepository.findByUserIdAndFolderId(VIEWER_ID, FOLDER_ID)).willReturn(Optional.of(viewerMapping));

                sharedFolderService.deleteSharedFolder(VIEWER_ID, FOLDER_ID);

                verify(usersFolderRepository).delete(viewerMapping);
            }
        }

        @Nested
        @DisplayName("실패")
        class Failure {
            @Test
            @DisplayName("유저-폴더 관계가 없으면 _FOLDER_NOT_FOUND를 던진다")
            void 유저폴더관계없음_예외() {
                given(usersFolderRepository.findByUserIdAndFolderId(VIEWER_ID, FOLDER_ID)).willReturn(Optional.empty());

                assertThatThrownBy(() -> sharedFolderService.deleteSharedFolder(VIEWER_ID, FOLDER_ID))
                        .isInstanceOf(GeneralException.class)
                        .satisfies(ex -> assertThat(((GeneralException) ex).getCode())
                                .isEqualTo(FolderErrorStatus._FOLDER_NOT_FOUND));
            }

            @Test
            @DisplayName("소유자 본인은 이 API로 삭제할 수 없어 _FOLDER_DELETE_FORBIDDEN을 던진다")
            void 소유자는_삭제할수없음_예외() {
                UsersFolder ownerMapping = participant(OWNER_ID, PermissionType.OWNER);
                given(usersFolderRepository.findByUserIdAndFolderId(OWNER_ID, FOLDER_ID)).willReturn(Optional.of(ownerMapping));

                assertThatThrownBy(() -> sharedFolderService.deleteSharedFolder(OWNER_ID, FOLDER_ID))
                        .isInstanceOf(GeneralException.class)
                        .satisfies(ex -> assertThat(((GeneralException) ex).getCode())
                                .isEqualTo(FolderErrorStatus._FOLDER_DELETE_FORBIDDEN));

                verify(usersFolderRepository, never()).delete(any());
            }
        }
    }
}
