package com.umc.linkyou.service;

import com.umc.linkyou.apiPayload.code.status.ErrorStatus;
import com.umc.linkyou.apiPayload.code.status.user.UserErrorStatus;
import com.umc.linkyou.apiPayload.exception.GeneralException;
import com.umc.linkyou.jwt.RefreshTokenManager;
import com.umc.linkyou.domain.Users;
import com.umc.linkyou.domain.enums.UserStatus;
import com.umc.linkyou.repository.authAccountRepository.AuthAccountRepository;
import com.umc.linkyou.repository.userRepository.UserRepository;
import com.umc.linkyou.service.users.UserWithdrawService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserWithdrawService - recoverUser 테스트")
public class UserWithdrawServiceTest {


    @Mock
    private UserRepository userRepository;
    @Mock private RefreshTokenManager refreshTokenManager;
    @Mock private AuthAccountRepository authAccountRepository;

    @InjectMocks
    private UserWithdrawService userWithdrawService;

    @Nested
    @DisplayName("성공 케이스")
    class Success {

        @Test
        @DisplayName("INACTIVE 상태이고 14일 이내라면 ACTIVE로 복구된다")
        void 정상복구_성공() {
            // given
            Long userId = 1L;
            Users user = Users.builder()
                    .id(userId)
                    .status(UserStatus.INACTIVE)
                    .build();
            user.withdraw("테스트 탈퇴", LocalDateTime.now().minusDays(7)); // 7일 전 탈퇴

            given(userRepository.findById(userId)).willReturn(Optional.of(user));
            given(userRepository.save(user)).willReturn(user);

            // when
            Users result = userWithdrawService.recoverUser(userId);

            // then
            assertThat(result.getStatus()).isEqualTo(UserStatus.ACTIVE);
            assertThat(result.getInactiveDate()).isNull();
            assertThat(result.getDeleted_reason()).isNull();
        }

        @Test
        @DisplayName("탈퇴 직후(1분 이내)에도 복구된다")
        void 탈퇴직후_즉시복구_성공() {
            // given
            Long userId = 2L;
            Users user = Users.builder()
                    .id(userId)
                    .status(UserStatus.INACTIVE)
                    .build();
            user.withdraw("테스트 탈퇴", LocalDateTime.now().minusMinutes(1));

            given(userRepository.findById(userId)).willReturn(Optional.of(user));
            given(userRepository.save(user)).willReturn(user);

            // when
            Users result = userWithdrawService.recoverUser(userId);

            // then
            assertThat(result.getStatus()).isEqualTo(UserStatus.ACTIVE);
        }

        @Test
        @DisplayName("탈퇴 후 정확히 13일 23시간에도 복구된다 (경계값)")
        void 유예기간_경계값_복구_성공() {
            // given
            Long userId = 3L;
            Users user = Users.builder()
                    .id(userId)
                    .status(UserStatus.INACTIVE)
                    .build();
            user.withdraw("테스트 탈퇴", LocalDateTime.now().minusDays(13).minusHours(23));

            given(userRepository.findById(userId)).willReturn(Optional.of(user));
            given(userRepository.save(user)).willReturn(user);

            // when
            Users result = userWithdrawService.recoverUser(userId);

            // then
            assertThat(result.getStatus()).isEqualTo(UserStatus.ACTIVE);
        }
    }
    @Nested
    @DisplayName("실패 케이스")
    class Failure {

        @Test
        @DisplayName("존재하지 않는 유저 ID면 USER_NOT_FOUND 예외가 발생한다")
        void 존재하지않는_유저_예외() {
            // given
            Long userId = 999L;
            given(userRepository.findById(userId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> userWithdrawService.recoverUser(userId))
                    .isInstanceOf(GeneralException.class)
                    .satisfies(e -> {
                        GeneralException ex = (GeneralException) e;
                        assertThat(ex.getCode()).isEqualTo(UserErrorStatus._USER_NOT_FOUND);
                    });
        }

        @Test
        @DisplayName("이미 ACTIVE 상태인 유저는 BAD_REQUEST 예외가 발생한다")
        void 이미활성상태_예외() {
            // given
            Long userId = 1L;
            Users user = Users.builder()
                    .id(userId)
                    .status(UserStatus.ACTIVE)
                    .build();

            given(userRepository.findById(userId)).willReturn(Optional.of(user));

            // when & then
            assertThatThrownBy(() -> userWithdrawService.recoverUser(userId))
                    .isInstanceOf(GeneralException.class)
                    .satisfies(e -> {
                        GeneralException ex = (GeneralException) e;
                        assertThat(ex.getCode()).isEqualTo(ErrorStatus._BAD_REQUEST);
                    });
        }
    }
}
