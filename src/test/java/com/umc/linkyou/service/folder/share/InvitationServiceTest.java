package com.umc.linkyou.service.folder.share;

import com.umc.linkyou.apiPayload.code.status.folder.InvitationErrorStatus;
import com.umc.linkyou.apiPayload.code.status.user.UserErrorStatus;
import com.umc.linkyou.apiPayload.exception.GeneralException;
import com.umc.linkyou.domain.Users;
import com.umc.linkyou.domain.enums.PermissionType;
import com.umc.linkyou.domain.folder.Folder;
import com.umc.linkyou.domain.folder.FolderShareLink;
import com.umc.linkyou.repository.FolderShareLinkRepository;
import com.umc.linkyou.repository.userRepository.UserRepository;
import com.umc.linkyou.repository.usersFolderRepository.UsersFolderRepository;
import com.umc.linkyou.web.dto.folder.share.InvitationInfoResponseDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static com.umc.linkyou.support.fixture.FolderFixture.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("InvitationServiceImpl 단위 테스트")
class InvitationServiceTest {
    @InjectMocks private InvitationServiceImpl invitationService;

    @Mock private FolderShareLinkRepository folderShareLinkRepository;
    @Mock private UsersFolderRepository usersFolderRepository;
    @Mock private UserRepository userRepository;

    private static final String TOKEN = "invite-token";
    private static final Long MEMBER_ID = 5L;

    private FolderShareLink link(Users creator, boolean active, LocalDateTime expiresAt) {
        return FolderShareLink.builder()
                .token(TOKEN)
                .folder(folder())
                .creator(creator)
                .permissionType(PermissionType.VIEWER)
                .expiresAt(expiresAt)
                .isActive(active)
                .build();
    }

    @Nested
    @DisplayName("초대 정보 조회(getInvitationInfo)")
    class GetInvitationInfo {
        @Nested
        @DisplayName("성공")
        class Success {
            @Test
            @DisplayName("유효한 토큰이면 폴더명과 초대자 닉네임을 반환한다")
            void 유효한토큰_초대정보반환() {
                given(folderShareLinkRepository.findByToken(TOKEN))
                        .willReturn(Optional.of(link(owner(), true, LocalDateTime.now().plusDays(1))));

                InvitationInfoResponseDTO result = invitationService.getInvitationInfo(TOKEN);

                assertThat(result.getFolderName()).isEqualTo("어학");
                assertThat(result.getOwnerName()).isEqualTo("주인");
            }
        }

        @Nested
        @DisplayName("실패")
        class Failure {
            @Test
            @DisplayName("존재하지 않는 토큰이면 INVITATION_NOT_FOUND를 던진다")
            void 토큰없음_예외() {
                given(folderShareLinkRepository.findByToken(TOKEN)).willReturn(Optional.empty());

                assertThatThrownBy(() -> invitationService.getInvitationInfo(TOKEN))
                        .isInstanceOf(GeneralException.class)
                        .satisfies(ex -> assertThat(((GeneralException) ex).getCode())
                                .isEqualTo(InvitationErrorStatus.INVITATION_NOT_FOUND));
            }

            @Test
            @DisplayName("만료되었거나 비활성화된 토큰이면 INVITATION_EXPIRED를 던진다")
            void 토큰만료_예외() {
                given(folderShareLinkRepository.findByToken(TOKEN))
                        .willReturn(Optional.of(link(owner(), true, LocalDateTime.now().minusDays(1))));

                assertThatThrownBy(() -> invitationService.getInvitationInfo(TOKEN))
                        .isInstanceOf(GeneralException.class)
                        .satisfies(ex -> assertThat(((GeneralException) ex).getCode())
                                .isEqualTo(InvitationErrorStatus.INVITATION_EXPIRED));
            }
        }
    }

    @Nested
    @DisplayName("초대 수락(acceptInvitation)")
    class AcceptInvitation {
        @Nested
        @DisplayName("성공")
        class Success {
            @Test
            @DisplayName("아직 멤버가 아니면 VIEWER 권한으로 추가하고 폴더 ID를 반환한다")
            void 신규멤버_추가후_폴더ID반환() {
                Folder folder = folder();
                Users member = Users.builder().id(MEMBER_ID).build();

                given(folderShareLinkRepository.findByToken(TOKEN))
                        .willReturn(Optional.of(link(owner(), true, LocalDateTime.now().plusDays(1))));
                given(userRepository.findById(MEMBER_ID)).willReturn(Optional.of(member));
                given(usersFolderRepository.existsActiveMember(MEMBER_ID, folder.getFolderId())).willReturn(false);

                Long result = invitationService.acceptInvitation(MEMBER_ID, TOKEN);

                assertThat(result).isEqualTo(FOLDER_ID);
                verify(usersFolderRepository).save(any());
            }

            @Test
            @DisplayName("이미 활성 멤버면 저장 없이 폴더 ID만 반환한다")
            void 이미멤버_저장없이_폴더ID반환() {
                Folder folder = folder();
                Users member = Users.builder().id(MEMBER_ID).build();

                given(folderShareLinkRepository.findByToken(TOKEN))
                        .willReturn(Optional.of(link(owner(), true, LocalDateTime.now().plusDays(1))));
                given(userRepository.findById(MEMBER_ID)).willReturn(Optional.of(member));
                given(usersFolderRepository.existsActiveMember(MEMBER_ID, folder.getFolderId())).willReturn(true);

                Long result = invitationService.acceptInvitation(MEMBER_ID, TOKEN);

                assertThat(result).isEqualTo(FOLDER_ID);
                verify(usersFolderRepository, never()).save(any());
            }
        }

        @Nested
        @DisplayName("실패")
        class Failure {
            @Test
            @DisplayName("존재하지 않는 토큰이면 INVITATION_NOT_FOUND를 던진다")
            void 토큰없음_예외() {
                given(folderShareLinkRepository.findByToken(TOKEN)).willReturn(Optional.empty());

                assertThatThrownBy(() -> invitationService.acceptInvitation(MEMBER_ID, TOKEN))
                        .isInstanceOf(GeneralException.class)
                        .satisfies(ex -> assertThat(((GeneralException) ex).getCode())
                                .isEqualTo(InvitationErrorStatus.INVITATION_NOT_FOUND));
            }

            @Test
            @DisplayName("만료된 토큰이면 INVITATION_EXPIRED를 던진다")
            void 토큰만료_예외() {
                given(folderShareLinkRepository.findByToken(TOKEN))
                        .willReturn(Optional.of(link(owner(), true, LocalDateTime.now().minusDays(1))));

                assertThatThrownBy(() -> invitationService.acceptInvitation(MEMBER_ID, TOKEN))
                        .isInstanceOf(GeneralException.class)
                        .satisfies(ex -> assertThat(((GeneralException) ex).getCode())
                                .isEqualTo(InvitationErrorStatus.INVITATION_EXPIRED));
            }

            @Test
            @DisplayName("존재하지 않는 유저면 _USER_NOT_FOUND를 던진다")
            void 유저없음_예외() {
                given(folderShareLinkRepository.findByToken(TOKEN))
                        .willReturn(Optional.of(link(owner(), true, LocalDateTime.now().plusDays(1))));
                given(userRepository.findById(MEMBER_ID)).willReturn(Optional.empty());

                assertThatThrownBy(() -> invitationService.acceptInvitation(MEMBER_ID, TOKEN))
                        .isInstanceOf(GeneralException.class)
                        .satisfies(ex -> assertThat(((GeneralException) ex).getCode())
                                .isEqualTo(UserErrorStatus._USER_NOT_FOUND));
            }

            @Test
            @DisplayName("초대 생성자 본인이면 INVITATION_CREATOR_CANNOT_ACCEPT를 던진다")
            void 생성자본인_예외() {
                given(folderShareLinkRepository.findByToken(TOKEN))
                        .willReturn(Optional.of(link(owner(), true, LocalDateTime.now().plusDays(1))));
                given(userRepository.findById(OWNER_ID)).willReturn(Optional.of(owner()));

                assertThatThrownBy(() -> invitationService.acceptInvitation(OWNER_ID, TOKEN))
                        .isInstanceOf(GeneralException.class)
                        .satisfies(ex -> assertThat(((GeneralException) ex).getCode())
                                .isEqualTo(InvitationErrorStatus.INVITATION_CREATOR_CANNOT_ACCEPT));

                verify(usersFolderRepository, never()).save(any());
            }
        }
    }
}
