package com.umc.linkyou.service.folder.share;

import com.umc.linkyou.apiPayload.code.status.folder.ShareFolderErrorStatus;
import com.umc.linkyou.apiPayload.exception.GeneralException;
import com.umc.linkyou.domain.AlarmSetting;
import com.umc.linkyou.domain.enums.AlarmType;
import com.umc.linkyou.domain.enums.PermissionType;
import com.umc.linkyou.domain.mapping.folder.UsersFolder;
import com.umc.linkyou.repository.AlarmSettingRepository;
import com.umc.linkyou.repository.FolderRepository.FolderRepository;
import com.umc.linkyou.repository.usersFolderRepository.UsersFolderRepository;
import com.umc.linkyou.service.alarm.AlarmService;
import com.umc.linkyou.web.dto.alarm.AlarmRequestDTO;
import com.umc.linkyou.web.dto.folder.share.FolderPermissionRequestDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static com.umc.linkyou.support.fixture.FolderFixture.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ShareFolderServiceImpl 단위 테스트")
class ShareFolderServiceTest {
    @InjectMocks private ShareFolderServiceImpl shareFolderService;

    @Mock private FolderRepository folderRepository;
    @Mock private UsersFolderRepository usersFolderRepository;
    @Mock private AlarmService alarmService;
    @Mock private AlarmSettingRepository alarmSettingRepository;

    private static final Long MEMBER_ID = 5L;
    private static final Long USERS_FOLDER_ID = 10L;

    private UsersFolder targetUsersFolder(PermissionType type) {
        UsersFolder uf = participant(MEMBER_ID, type);
        uf.setUpdatedAt(LocalDateTime.now());
        return uf;
    }

    @Nested
    @DisplayName("유저의 폴더 권한 수정 (updateViewerPermission)")
    class UpdateViewerPermission {
        @Nested
        @DisplayName("성공")
        class Success {
            @Test
            @DisplayName("대상 멤버의 폴더 알림이 켜져 있으면 FOLDER_PERMISSION_CHANGED를 발송한다")
            void 폴더알림켜짐_알람발송() {
                UsersFolder usersFolder = targetUsersFolder(PermissionType.VIEWER);
                FolderPermissionRequestDTO request = new FolderPermissionRequestDTO();
                request.setPermission(PermissionType.WRITER);

                given(folderRepository.existsById(FOLDER_ID)).willReturn(true);
                given(usersFolderRepository.existsFolderOwner(OWNER_ID, FOLDER_ID)).willReturn(true);
                given(usersFolderRepository.findById(USERS_FOLDER_ID)).willReturn(Optional.of(usersFolder));
                given(alarmSettingRepository.findByUserId(MEMBER_ID)).willReturn(Optional.of(AlarmSetting.createDefault(usersFolder.getUser())));

                shareFolderService.updateViewerPermission(OWNER_ID, FOLDER_ID, USERS_FOLDER_ID, request);

                verify(alarmService).sendAlarm(eq(MEMBER_ID), eq(new AlarmRequestDTO.AlarmSendRequestDTO(
                    AlarmType.FOLDER_PERMISSION_CHANGED, FOLDER_ID, Map.of("folderName", "어학"))));
            }

            @Test
            @DisplayName("대상 멤버의 폴더 알림이 꺼져 있으면 알람을 발송하지 않는다")
            void 폴더알림꺼짐_알람미발송() {
                UsersFolder usersFolder = targetUsersFolder(PermissionType.VIEWER);
                FolderPermissionRequestDTO request = new FolderPermissionRequestDTO();
                request.setPermission(PermissionType.WRITER);

                AlarmSetting offSetting = AlarmSetting.createDefault(usersFolder.getUser());
                offSetting.updateFolder(false);

                given(folderRepository.existsById(FOLDER_ID)).willReturn(true);
                given(usersFolderRepository.existsFolderOwner(OWNER_ID, FOLDER_ID)).willReturn(true);
                given(usersFolderRepository.findById(USERS_FOLDER_ID)).willReturn(Optional.of(usersFolder));
                given(alarmSettingRepository.findByUserId(MEMBER_ID)).willReturn(Optional.of(offSetting));

                shareFolderService.updateViewerPermission(OWNER_ID, FOLDER_ID, USERS_FOLDER_ID, request);

                verify(alarmService, never()).sendAlarm(any(), any());
            }
        }

        @Nested
        @DisplayName("실패")
        class Failure {
            @Test
            @DisplayName("소유자가 아니면 예외를 던지고, 알람도 발송하지 않는다")
            void 소유자아님_예외() {
                given(folderRepository.existsById(FOLDER_ID)).willReturn(true);
                given(usersFolderRepository.existsFolderOwner(OWNER_ID, FOLDER_ID)).willReturn(false);

                FolderPermissionRequestDTO request = new FolderPermissionRequestDTO();
                request.setPermission(PermissionType.WRITER);

                assertThatThrownBy(() -> shareFolderService.updateViewerPermission(OWNER_ID, FOLDER_ID, USERS_FOLDER_ID, request))
                    .isInstanceOf(GeneralException.class)
                    .satisfies(ex -> assertThat(((GeneralException) ex).getCode())
                        .isEqualTo(ShareFolderErrorStatus._FOLDER_PERMISSION_NOT_ALLOWED));

                verify(alarmService, never()).sendAlarm(any(), any());
            }
        }
    }
}