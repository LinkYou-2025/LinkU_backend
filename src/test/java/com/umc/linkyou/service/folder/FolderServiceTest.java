package com.umc.linkyou.service.folder;

import com.umc.linkyou.apiPayload.code.status.folder.FolderErrorStatus;
import com.umc.linkyou.apiPayload.exception.GeneralException;
import com.umc.linkyou.awss3.AwsS3Service;
import com.umc.linkyou.domain.enums.PermissionType;
import com.umc.linkyou.domain.folder.Folder;
import com.umc.linkyou.repository.FolderRepository.FolderRepository;
import com.umc.linkyou.repository.userRepository.UserRepository;
import com.umc.linkyou.repository.usersFolderRepository.UsersFolderRepository;
import com.umc.linkyou.service.alarm.event.FolderDeletedAlarmEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Optional;

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
    @Mock private UsersFolderRepository usersFolderRepository;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private AwsS3Service awsS3Service;

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
}