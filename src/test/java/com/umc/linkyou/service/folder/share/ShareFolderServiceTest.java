package com.umc.linkyou.service.folder.share;

import com.umc.linkyou.apiPayload.code.status.folder.FolderErrorStatus;
import com.umc.linkyou.apiPayload.code.status.folder.InvitationErrorStatus;
import com.umc.linkyou.apiPayload.code.status.folder.ShareFolderErrorStatus;
import com.umc.linkyou.apiPayload.exception.GeneralException;
import com.umc.linkyou.domain.AlarmSetting;
import com.umc.linkyou.domain.Users;
import com.umc.linkyou.domain.enums.PermissionType;
import com.umc.linkyou.domain.folder.Folder;
import com.umc.linkyou.domain.folder.FolderShareLink;
import com.umc.linkyou.domain.mapping.folder.UsersFolder;
import com.umc.linkyou.repository.AlarmSettingRepository;
import com.umc.linkyou.repository.FolderRepository.FolderRepository;
import com.umc.linkyou.repository.FolderShareLinkRepository;
import com.umc.linkyou.repository.userRepository.UserRepository;
import com.umc.linkyou.repository.usersFolderRepository.UsersFolderRepository;
import com.umc.linkyou.service.alarm.event.FolderPermissionChangedAlarmEvent;
import com.umc.linkyou.web.dto.folder.share.FolderPermissionRequestDTO;
import com.umc.linkyou.web.dto.folder.share.MySharedFolderResponseDTO;
import com.umc.linkyou.web.dto.folder.share.ShareFolderResponseDTO;
import com.umc.linkyou.web.dto.folder.share.ViewerResponseDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
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
    @Mock private UserRepository userRepository;
    @Mock private UsersFolderRepository usersFolderRepository;
    @Mock private FolderShareLinkRepository folderShareLinkRepository;
    @Mock private AlarmSettingRepository alarmSettingRepository;
    @Mock private ApplicationEventPublisher eventPublisher;

    private UsersFolder participantWithCreatedAt(Long userId, PermissionType type, LocalDateTime createdAt) {
        UsersFolder uf = participant(userId, type);
        ReflectionTestUtils.setField(uf, "createdAt", createdAt);
        return uf;
    }

    private static final Long MEMBER_ID = 5L;
    private static final Long USERS_FOLDER_ID = 10L;

    private UsersFolder targetUsersFolder(PermissionType type) {
        UsersFolder uf = participant(MEMBER_ID, type);
        ReflectionTestUtils.setField(uf, "updatedAt", LocalDateTime.now());
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

                ArgumentCaptor<FolderPermissionChangedAlarmEvent> captor =
                        ArgumentCaptor.forClass(FolderPermissionChangedAlarmEvent.class);
                verify(eventPublisher).publishEvent(captor.capture());

                FolderPermissionChangedAlarmEvent event = captor.getValue();
                assertThat(event.memberId()).isEqualTo(MEMBER_ID);
                assertThat(event.folderId()).isEqualTo(FOLDER_ID);
                assertThat(event.folderName()).isEqualTo("어학");
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

                verify(eventPublisher, never()).publishEvent(any());
            }
        }

        @Nested
        @DisplayName("실패")
        class Failure {
            @Test
            @DisplayName("존재하지 않는 폴더면 _FOLDER_NOT_FOUND를 던진다")
            void 폴더없음_예외() {
                given(folderRepository.existsById(FOLDER_ID)).willReturn(false);

                FolderPermissionRequestDTO request = new FolderPermissionRequestDTO();
                request.setPermission(PermissionType.WRITER);

                assertThatThrownBy(() -> shareFolderService.updateViewerPermission(OWNER_ID, FOLDER_ID, USERS_FOLDER_ID, request))
                    .isInstanceOf(GeneralException.class)
                    .satisfies(ex -> assertThat(((GeneralException) ex).getCode())
                        .isEqualTo(FolderErrorStatus._FOLDER_NOT_FOUND));

                verify(eventPublisher, never()).publishEvent(any());
            }

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

                verify(eventPublisher, never()).publishEvent(any());
            }

            @Test
            @DisplayName("대상 유저-폴더 관계가 없으면 _FOLDER_PERMISSION_NOT_FOUND를 던진다")
            void 대상유저폴더관계없음_예외() {
                given(folderRepository.existsById(FOLDER_ID)).willReturn(true);
                given(usersFolderRepository.existsFolderOwner(OWNER_ID, FOLDER_ID)).willReturn(true);
                given(usersFolderRepository.findById(USERS_FOLDER_ID)).willReturn(Optional.empty());

                FolderPermissionRequestDTO request = new FolderPermissionRequestDTO();
                request.setPermission(PermissionType.WRITER);

                assertThatThrownBy(() -> shareFolderService.updateViewerPermission(OWNER_ID, FOLDER_ID, USERS_FOLDER_ID, request))
                    .isInstanceOf(GeneralException.class)
                    .satisfies(ex -> assertThat(((GeneralException) ex).getCode())
                        .isEqualTo(ShareFolderErrorStatus._FOLDER_PERMISSION_NOT_FOUND));

                verify(eventPublisher, never()).publishEvent(any());
            }

            @Test
            @DisplayName("대상 유저-폴더 관계가 다른 폴더의 것이면 _FOLDER_PERMISSION_NOT_ALLOWED를 던진다")
            void 다른폴더의_유저폴더관계_예외() {
                Folder otherFolder = Folder.builder().folderId(999L).folderName("다른폴더").build();
                UsersFolder otherFolderUf = UsersFolder.builder()
                        .user(Users.builder().id(MEMBER_ID).build())
                        .folder(otherFolder)
                        .permissionType(PermissionType.VIEWER)
                        .build();

                given(folderRepository.existsById(FOLDER_ID)).willReturn(true);
                given(usersFolderRepository.existsFolderOwner(OWNER_ID, FOLDER_ID)).willReturn(true);
                given(usersFolderRepository.findById(USERS_FOLDER_ID)).willReturn(Optional.of(otherFolderUf));

                FolderPermissionRequestDTO request = new FolderPermissionRequestDTO();
                request.setPermission(PermissionType.WRITER);

                assertThatThrownBy(() -> shareFolderService.updateViewerPermission(OWNER_ID, FOLDER_ID, USERS_FOLDER_ID, request))
                    .isInstanceOf(GeneralException.class)
                    .satisfies(ex -> assertThat(((GeneralException) ex).getCode())
                        .isEqualTo(ShareFolderErrorStatus._FOLDER_PERMISSION_NOT_ALLOWED));

                verify(eventPublisher, never()).publishEvent(any());
            }

            @Test
            @DisplayName("대상이 폴더 소유자면 _FOLDER_OWNER_UPDATE_NOT_ALLOWED를 던진다")
            void 오너권한수정_예외() {
                UsersFolder ownerTarget = targetUsersFolder(PermissionType.OWNER);

                given(folderRepository.existsById(FOLDER_ID)).willReturn(true);
                given(usersFolderRepository.existsFolderOwner(OWNER_ID, FOLDER_ID)).willReturn(true);
                given(usersFolderRepository.findById(USERS_FOLDER_ID)).willReturn(Optional.of(ownerTarget));

                FolderPermissionRequestDTO request = new FolderPermissionRequestDTO();
                request.setPermission(PermissionType.WRITER);

                assertThatThrownBy(() -> shareFolderService.updateViewerPermission(OWNER_ID, FOLDER_ID, USERS_FOLDER_ID, request))
                    .isInstanceOf(GeneralException.class)
                    .satisfies(ex -> assertThat(((GeneralException) ex).getCode())
                        .isEqualTo(ShareFolderErrorStatus._FOLDER_OWNER_UPDATE_NOT_ALLOWED));

                verify(eventPublisher, never()).publishEvent(any());
            }

            @Test
            @DisplayName("OWNER 권한으로 변경 요청 시 _INVALID_PERMISSION_TYPE을 던진다")
            void OWNER로_변경요청_예외() {
                UsersFolder usersFolder = targetUsersFolder(PermissionType.VIEWER);

                given(folderRepository.existsById(FOLDER_ID)).willReturn(true);
                given(usersFolderRepository.existsFolderOwner(OWNER_ID, FOLDER_ID)).willReturn(true);
                given(usersFolderRepository.findById(USERS_FOLDER_ID)).willReturn(Optional.of(usersFolder));

                FolderPermissionRequestDTO request = new FolderPermissionRequestDTO();
                request.setPermission(PermissionType.OWNER);

                assertThatThrownBy(() -> shareFolderService.updateViewerPermission(OWNER_ID, FOLDER_ID, USERS_FOLDER_ID, request))
                    .isInstanceOf(GeneralException.class)
                    .satisfies(ex -> assertThat(((GeneralException) ex).getCode())
                        .isEqualTo(ShareFolderErrorStatus._INVALID_PERMISSION_TYPE));

                verify(eventPublisher, never()).publishEvent(any());
            }
        }
    }

    @Nested
    @DisplayName("초대 링크 생성(createInviteLink)")
    class CreateInviteLink {
        @Nested
        @DisplayName("성공")
        class Success {
            @Test
            @DisplayName("기존 링크가 없으면 새 토큰을 생성해 저장한다")
            void 기존링크없음_새토큰생성() {
                given(folderRepository.findById(FOLDER_ID)).willReturn(Optional.of(folder()));
                given(usersFolderRepository.existsFolderOwner(OWNER_ID, FOLDER_ID)).willReturn(true);
                given(folderShareLinkRepository.findByFolder_FolderIdAndIsActiveTrue(FOLDER_ID)).willReturn(Optional.empty());
                given(userRepository.findById(OWNER_ID)).willReturn(Optional.of(owner()));

                String token = shareFolderService.createInviteLink(OWNER_ID, FOLDER_ID);

                assertThat(token).isNotBlank();
                verify(folderShareLinkRepository).save(any());
            }

            @Test
            @DisplayName("유효한 기존 링크가 있으면 기존 토큰을 반환한다")
            void 유효한링크존재_기존토큰반환() {
                FolderShareLink existing = FolderShareLink.builder()
                        .token("existing-token")
                        .folder(folder())
                        .creator(owner())
                        .permissionType(PermissionType.VIEWER)
                        .expiresAt(LocalDateTime.now().plusDays(1))
                        .isActive(true)
                        .build();

                given(folderRepository.findById(FOLDER_ID)).willReturn(Optional.of(folder()));
                given(usersFolderRepository.existsFolderOwner(OWNER_ID, FOLDER_ID)).willReturn(true);
                given(folderShareLinkRepository.findByFolder_FolderIdAndIsActiveTrue(FOLDER_ID)).willReturn(Optional.of(existing));

                String token = shareFolderService.createInviteLink(OWNER_ID, FOLDER_ID);

                assertThat(token).isEqualTo("existing-token");
                verify(folderShareLinkRepository, never()).save(any());
            }

            @Test
            @DisplayName("만료된 기존 링크가 있으면 토큰을 갱신한다")
            void 만료된링크존재_토큰갱신() {
                FolderShareLink expired = FolderShareLink.builder()
                        .token("old-token")
                        .folder(folder())
                        .creator(owner())
                        .permissionType(PermissionType.VIEWER)
                        .expiresAt(LocalDateTime.now().minusDays(1))
                        .isActive(true)
                        .build();

                given(folderRepository.findById(FOLDER_ID)).willReturn(Optional.of(folder()));
                given(usersFolderRepository.existsFolderOwner(OWNER_ID, FOLDER_ID)).willReturn(true);
                given(folderShareLinkRepository.findByFolder_FolderIdAndIsActiveTrue(FOLDER_ID)).willReturn(Optional.of(expired));

                String token = shareFolderService.createInviteLink(OWNER_ID, FOLDER_ID);

                assertThat(token).isNotEqualTo("old-token");
                assertThat(expired.getToken()).isEqualTo(token);
            }
        }

        @Nested
        @DisplayName("실패")
        class Failure {
            @Test
            @DisplayName("존재하지 않는 폴더면 _FOLDER_NOT_FOUND를 던진다")
            void 폴더없음_예외() {
                given(folderRepository.findById(FOLDER_ID)).willReturn(Optional.empty());

                assertThatThrownBy(() -> shareFolderService.createInviteLink(OWNER_ID, FOLDER_ID))
                        .isInstanceOf(GeneralException.class)
                        .satisfies(ex -> assertThat(((GeneralException) ex).getCode())
                                .isEqualTo(FolderErrorStatus._FOLDER_NOT_FOUND));
            }

            @Test
            @DisplayName("소유자가 아니면 _FOLDER_PERMISSION_NOT_ALLOWED를 던진다")
            void 소유자아님_예외() {
                given(folderRepository.findById(FOLDER_ID)).willReturn(Optional.of(folder()));
                given(usersFolderRepository.existsFolderOwner(OWNER_ID, FOLDER_ID)).willReturn(false);

                assertThatThrownBy(() -> shareFolderService.createInviteLink(OWNER_ID, FOLDER_ID))
                        .isInstanceOf(GeneralException.class)
                        .satisfies(ex -> assertThat(((GeneralException) ex).getCode())
                                .isEqualTo(ShareFolderErrorStatus._FOLDER_PERMISSION_NOT_ALLOWED));
            }
        }
    }

    @Nested
    @DisplayName("초대 링크 비활성화(deactivateInviteLink)")
    class DeactivateInviteLink {
        @Nested
        @DisplayName("성공")
        class Success {
            @Test
            @DisplayName("활성화된 링크를 비활성화한다")
            void 정상_요청_시_링크가_비활성화된다() {
                FolderShareLink link = FolderShareLink.builder()
                        .token("token").folder(folder()).creator(owner())
                        .permissionType(PermissionType.VIEWER)
                        .expiresAt(LocalDateTime.now().plusDays(1))
                        .isActive(true)
                        .build();

                given(folderRepository.existsById(FOLDER_ID)).willReturn(true);
                given(usersFolderRepository.existsFolderOwner(OWNER_ID, FOLDER_ID)).willReturn(true);
                given(folderShareLinkRepository.findByFolder_FolderIdAndIsActiveTrue(FOLDER_ID)).willReturn(Optional.of(link));

                shareFolderService.deactivateInviteLink(OWNER_ID, FOLDER_ID);

                assertThat(link.getIsActive()).isFalse();
            }
        }

        @Nested
        @DisplayName("실패")
        class Failure {
            @Test
            @DisplayName("존재하지 않는 폴더면 _FOLDER_NOT_FOUND를 던진다")
            void 폴더없음_예외() {
                given(folderRepository.existsById(FOLDER_ID)).willReturn(false);

                assertThatThrownBy(() -> shareFolderService.deactivateInviteLink(OWNER_ID, FOLDER_ID))
                        .isInstanceOf(GeneralException.class)
                        .satisfies(ex -> assertThat(((GeneralException) ex).getCode())
                                .isEqualTo(FolderErrorStatus._FOLDER_NOT_FOUND));
            }

            @Test
            @DisplayName("소유자가 아니면 _FOLDER_PERMISSION_NOT_ALLOWED를 던진다")
            void 소유자아님_예외() {
                given(folderRepository.existsById(FOLDER_ID)).willReturn(true);
                given(usersFolderRepository.existsFolderOwner(OWNER_ID, FOLDER_ID)).willReturn(false);

                assertThatThrownBy(() -> shareFolderService.deactivateInviteLink(OWNER_ID, FOLDER_ID))
                        .isInstanceOf(GeneralException.class)
                        .satisfies(ex -> assertThat(((GeneralException) ex).getCode())
                                .isEqualTo(ShareFolderErrorStatus._FOLDER_PERMISSION_NOT_ALLOWED));
            }

            @Test
            @DisplayName("활성화된 링크가 없으면 INVITATION_LINK_NOT_FOUND를 던진다")
            void 활성링크없음_예외() {
                given(folderRepository.existsById(FOLDER_ID)).willReturn(true);
                given(usersFolderRepository.existsFolderOwner(OWNER_ID, FOLDER_ID)).willReturn(true);
                given(folderShareLinkRepository.findByFolder_FolderIdAndIsActiveTrue(FOLDER_ID)).willReturn(Optional.empty());

                assertThatThrownBy(() -> shareFolderService.deactivateInviteLink(OWNER_ID, FOLDER_ID))
                        .isInstanceOf(GeneralException.class)
                        .satisfies(ex -> assertThat(((GeneralException) ex).getCode())
                                .isEqualTo(InvitationErrorStatus.INVITATION_LINK_NOT_FOUND));
            }
        }
    }

    @Nested
    @DisplayName("폴더 참여자 목록 조회(getViewers)")
    class GetViewers {
        @Nested
        @DisplayName("성공")
        class Success {
            @Test
            @DisplayName("참여자 목록을 유저명/권한과 함께 반환한다")
            void 정상_요청_시_참여자목록을_반환한다() {
                given(folderRepository.existsById(FOLDER_ID)).willReturn(true);
                given(usersFolderRepository.existsFolderOwner(OWNER_ID, FOLDER_ID)).willReturn(true);
                given(usersFolderRepository.findAllParticipantsByFolderId(FOLDER_ID)).willReturn(List.of(
                        participant(MEMBER_ID, PermissionType.VIEWER)
                ));

                List<ViewerResponseDTO> result = shareFolderService.getViewers(OWNER_ID, FOLDER_ID);

                assertThat(result).hasSize(1);
                assertThat(result.get(0).getUserId()).isEqualTo(MEMBER_ID);
                assertThat(result.get(0).getPermission()).isEqualTo(PermissionType.VIEWER.name());
            }
        }

        @Nested
        @DisplayName("실패")
        class Failure {
            @Test
            @DisplayName("존재하지 않는 폴더면 _FOLDER_NOT_FOUND를 던진다")
            void 폴더없음_예외() {
                given(folderRepository.existsById(FOLDER_ID)).willReturn(false);

                assertThatThrownBy(() -> shareFolderService.getViewers(OWNER_ID, FOLDER_ID))
                        .isInstanceOf(GeneralException.class)
                        .satisfies(ex -> assertThat(((GeneralException) ex).getCode())
                                .isEqualTo(FolderErrorStatus._FOLDER_NOT_FOUND));
            }

            @Test
            @DisplayName("소유자가 아니면 _FOLDER_PERMISSION_NOT_ALLOWED를 던진다")
            void 소유자아님_예외() {
                given(folderRepository.existsById(FOLDER_ID)).willReturn(true);
                given(usersFolderRepository.existsFolderOwner(OWNER_ID, FOLDER_ID)).willReturn(false);

                assertThatThrownBy(() -> shareFolderService.getViewers(OWNER_ID, FOLDER_ID))
                        .isInstanceOf(GeneralException.class)
                        .satisfies(ex -> assertThat(((GeneralException) ex).getCode())
                                .isEqualTo(ShareFolderErrorStatus._FOLDER_PERMISSION_NOT_ALLOWED));
            }
        }
    }

    @Nested
    @DisplayName("공유 폴더 나가기(leaveFolder)")
    class LeaveFolder {
        @Nested
        @DisplayName("성공")
        class Success {
            @Test
            @DisplayName("가장 먼저 참여한 멤버에게 소유권을 위임하고 본인은 NONE이 된다")
            void 정상_요청_시_소유권이_위임된다() {
                UsersFolder ownerUf = participantWithCreatedAt(OWNER_ID, PermissionType.OWNER, LocalDateTime.now());
                UsersFolder oldestMemberUf = participantWithCreatedAt(MEMBER_ID, PermissionType.VIEWER, LocalDateTime.now().minusDays(3));
                UsersFolder otherMemberUf = participantWithCreatedAt(3L, PermissionType.WRITER, LocalDateTime.now().minusDays(1));

                given(folderRepository.existsById(FOLDER_ID)).willReturn(true);
                given(usersFolderRepository.existsFolderOwner(OWNER_ID, FOLDER_ID)).willReturn(true);
                given(usersFolderRepository.findAllParticipantsByFolderId(FOLDER_ID))
                        .willReturn(List.of(ownerUf, oldestMemberUf, otherMemberUf));
                given(usersFolderRepository.findByUserIdAndFolderId(OWNER_ID, FOLDER_ID)).willReturn(Optional.of(ownerUf));

                ShareFolderResponseDTO result = shareFolderService.leaveFolder(OWNER_ID, FOLDER_ID);

                assertThat(result.getUserId()).isEqualTo(MEMBER_ID);
                assertThat(result.getPermission()).isEqualTo(PermissionType.OWNER.name());
                assertThat(oldestMemberUf.getPermissionType()).isEqualTo(PermissionType.OWNER);
                assertThat(ownerUf.getPermissionType()).isEqualTo(PermissionType.NONE);
            }
        }

        @Nested
        @DisplayName("실패")
        class Failure {
            @Test
            @DisplayName("존재하지 않는 폴더면 _FOLDER_NOT_FOUND를 던진다")
            void 폴더없음_예외() {
                given(folderRepository.existsById(FOLDER_ID)).willReturn(false);

                assertThatThrownBy(() -> shareFolderService.leaveFolder(OWNER_ID, FOLDER_ID))
                        .isInstanceOf(GeneralException.class)
                        .satisfies(ex -> assertThat(((GeneralException) ex).getCode())
                                .isEqualTo(FolderErrorStatus._FOLDER_NOT_FOUND));
            }

            @Test
            @DisplayName("소유자가 아니면 _FOLDER_PERMISSION_NOT_ALLOWED를 던진다")
            void 소유자아님_예외() {
                given(folderRepository.existsById(FOLDER_ID)).willReturn(true);
                given(usersFolderRepository.existsFolderOwner(OWNER_ID, FOLDER_ID)).willReturn(false);

                assertThatThrownBy(() -> shareFolderService.leaveFolder(OWNER_ID, FOLDER_ID))
                        .isInstanceOf(GeneralException.class)
                        .satisfies(ex -> assertThat(((GeneralException) ex).getCode())
                                .isEqualTo(ShareFolderErrorStatus._FOLDER_PERMISSION_NOT_ALLOWED));
            }

            @Test
            @DisplayName("위임할 다른 멤버가 없으면 _FOLDER_LEAVE_NO_MEMBER_TO_TRANSFER를 던진다")
            void 위임할멤버없음_예외() {
                given(folderRepository.existsById(FOLDER_ID)).willReturn(true);
                given(usersFolderRepository.existsFolderOwner(OWNER_ID, FOLDER_ID)).willReturn(true);
                given(usersFolderRepository.findAllParticipantsByFolderId(FOLDER_ID)).willReturn(List.of());

                assertThatThrownBy(() -> shareFolderService.leaveFolder(OWNER_ID, FOLDER_ID))
                        .isInstanceOf(GeneralException.class)
                        .satisfies(ex -> assertThat(((GeneralException) ex).getCode())
                                .isEqualTo(ShareFolderErrorStatus._FOLDER_LEAVE_NO_MEMBER_TO_TRANSFER));
            }
        }
    }

    @Nested
    @DisplayName("내가 공유한 폴더 목록 조회(getMySharedFolders)")
    class GetMySharedFolders {
        @Test
        @DisplayName("공유한 폴더가 없으면 빈 리스트를 반환한다")
        void 공유폴더없음_빈리스트반환() {
            given(usersFolderRepository.findMySharedFolders(OWNER_ID)).willReturn(List.of());

            List<MySharedFolderResponseDTO> result = shareFolderService.getMySharedFolders(OWNER_ID);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("공유한 폴더가 있으면 멤버 수와 함께 반환한다")
        void 공유폴더존재_멤버수와함께반환() {
            Folder sharedFolder = folder();
            given(usersFolderRepository.findMySharedFolders(OWNER_ID)).willReturn(List.of(sharedFolder));
            given(usersFolderRepository.findAllParticipantsByFolderIdIn(List.of(FOLDER_ID)))
                    .willReturn(List.of(participant(MEMBER_ID, PermissionType.VIEWER), participant(3L, PermissionType.WRITER)));

            List<MySharedFolderResponseDTO> result = shareFolderService.getMySharedFolders(OWNER_ID);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getFolderId()).isEqualTo(FOLDER_ID);
            assertThat(result.get(0).getMemberCount()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("폴더 비공개 전환(unshare)")
    class Unshare {
        @Nested
        @DisplayName("성공")
        class Success {
            @Test
            @DisplayName("활성 링크를 만료시키고 멤버 권한을 모두 박탈한다")
            void 정상_요청_시_멤버권한이_박탈된다() {
                UsersFolder viewer = participant(MEMBER_ID, PermissionType.VIEWER);
                FolderShareLink link = FolderShareLink.builder()
                        .token("token").folder(folder()).creator(owner())
                        .permissionType(PermissionType.VIEWER)
                        .expiresAt(LocalDateTime.now().plusDays(1))
                        .isActive(true)
                        .build();

                given(folderRepository.existsById(FOLDER_ID)).willReturn(true);
                given(usersFolderRepository.existsFolderOwner(OWNER_ID, FOLDER_ID)).willReturn(true);
                given(folderShareLinkRepository.findByFolder_FolderIdAndIsActiveTrue(FOLDER_ID)).willReturn(Optional.of(link));
                given(usersFolderRepository.findAllParticipantsByFolderId(FOLDER_ID)).willReturn(List.of(viewer));

                ShareFolderResponseDTO result = shareFolderService.unshare(OWNER_ID, FOLDER_ID);

                assertThat(result.getPermission()).isEqualTo("PRIVATE");
                assertThat(link.getIsActive()).isFalse();
                assertThat(viewer.getPermissionType()).isEqualTo(PermissionType.NONE);
            }
        }

        @Nested
        @DisplayName("실패")
        class Failure {
            @Test
            @DisplayName("존재하지 않는 폴더면 _FOLDER_NOT_FOUND를 던진다")
            void 폴더없음_예외() {
                given(folderRepository.existsById(FOLDER_ID)).willReturn(false);

                assertThatThrownBy(() -> shareFolderService.unshare(OWNER_ID, FOLDER_ID))
                        .isInstanceOf(GeneralException.class)
                        .satisfies(ex -> assertThat(((GeneralException) ex).getCode())
                                .isEqualTo(FolderErrorStatus._FOLDER_NOT_FOUND));
            }

            @Test
            @DisplayName("소유자가 아니면 _FOLDER_PERMISSION_NOT_ALLOWED를 던진다")
            void 소유자아님_예외() {
                given(folderRepository.existsById(FOLDER_ID)).willReturn(true);
                given(usersFolderRepository.existsFolderOwner(OWNER_ID, FOLDER_ID)).willReturn(false);

                assertThatThrownBy(() -> shareFolderService.unshare(OWNER_ID, FOLDER_ID))
                        .isInstanceOf(GeneralException.class)
                        .satisfies(ex -> assertThat(((GeneralException) ex).getCode())
                                .isEqualTo(ShareFolderErrorStatus._FOLDER_PERMISSION_NOT_ALLOWED));
            }
        }
    }
}