package com.umc.linkyou.service;

import com.umc.linkyou.apiPayload.code.status.ErrorStatus;
import com.umc.linkyou.apiPayload.code.status.user.UserErrorStatus;
import com.umc.linkyou.apiPayload.exception.GeneralException;
import com.umc.linkyou.jwt.AccessTokenBlackListManager;
import com.umc.linkyou.jwt.JwtTokenProvider;
import com.umc.linkyou.jwt.RefreshTokenManager;
import com.umc.linkyou.jwt.TokenIssueService;
import com.umc.linkyou.domain.Users;
import com.umc.linkyou.domain.enums.DeviceType;
import com.umc.linkyou.domain.enums.Provider;
import com.umc.linkyou.domain.enums.UserStatus;
import com.umc.linkyou.repository.authAccountRepository.AuthAccountRepository;
import com.umc.linkyou.repository.userRepository.UserRepository;
import com.umc.linkyou.service.users.UserStatusValidator;
import com.umc.linkyou.service.users.UserWithdrawService;
import com.umc.linkyou.web.dto.UserRequestDTO;
import com.umc.linkyou.web.dto.UserResponseDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserWithdrawService - withdrawUser/recoverUser 테스트")
public class UserWithdrawServiceTest {


    @Mock
    private UserRepository userRepository;
    @Mock private RefreshTokenManager refreshTokenManager;
    @Mock private AuthAccountRepository authAccountRepository;
    @Mock private UserStatusValidator userStatusValidator;
    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private AccessTokenBlackListManager accessTokenBlackListManager;
    @Mock private TokenIssueService tokenIssueService;

    @InjectMocks
    private UserWithdrawService userWithdrawService;

    private static final UserRequestDTO.RecoverDTO RECOVER_REQUEST =
            new UserRequestDTO.RecoverDTO("test-device", DeviceType.PHONE);

    @Nested
    @DisplayName("withdrawUser - 회원 탈퇴 테스트")
    class Withdraw {

        @Test
        @DisplayName("accessToken이 주어지면 리프레시 토큰 삭제 + 액세스 토큰을 블랙리스트에 등록하고 즉시 로그아웃 처리한다")
        void 탈퇴_성공_액세스토큰_블랙리스트등록() {
            // given
            Long userId = 10L;
            String accessToken = "access-token-value";
            long remainingTtlMs = 60_000L;

            Users user = Users.builder().id(userId).status(UserStatus.ACTIVE).build();
            UserRequestDTO.DeleteReasonDTO reasonDTO = new UserRequestDTO.DeleteReasonDTO();
            reasonDTO.setReason("더 이상 사용하지 않음");

            given(userRepository.findById(userId)).willReturn(Optional.of(user));
            given(userRepository.save(user)).willReturn(user);
            given(jwtTokenProvider.getRemainingExpiryMs(accessToken)).willReturn(remainingTtlMs);

            // when
            Users result = userWithdrawService.withdrawUser(userId, reasonDTO, accessToken);

            // then
            assertThat(result.getStatus()).isEqualTo(UserStatus.INACTIVE);
            assertThat(result.getDeleted_reason()).isEqualTo("더 이상 사용하지 않음");
            then(refreshTokenManager).should(times(1)).deleteAllTokens(userId);
            then(accessTokenBlackListManager)
                    .should(times(1))
                    .addToBlacklist(accessToken, remainingTtlMs);
        }

        @Test
        @DisplayName("accessToken이 없으면(webhook 등 내부 호출) 블랙리스트 등록을 시도하지 않는다")
        void 탈퇴_성공_액세스토큰_없음() {
            // given
            Long userId = 11L;
            Users user = Users.builder().id(userId).status(UserStatus.ACTIVE).build();
            UserRequestDTO.DeleteReasonDTO reasonDTO = new UserRequestDTO.DeleteReasonDTO();
            reasonDTO.setReason("웹훅에 의한 탈퇴");

            given(userRepository.findById(userId)).willReturn(Optional.of(user));
            given(userRepository.save(user)).willReturn(user);

            // when : accessToken 없는 2-arg 오버로드 호출
            Users result = userWithdrawService.withdrawUser(userId, reasonDTO);

            // then
            assertThat(result.getStatus()).isEqualTo(UserStatus.INACTIVE);
            then(refreshTokenManager).should(times(1)).deleteAllTokens(userId);
            then(jwtTokenProvider).should(never()).getRemainingExpiryMs(org.mockito.ArgumentMatchers.anyString());
            then(accessTokenBlackListManager)
                    .should(never())
                    .addToBlacklist(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyLong());
        }

        @Test
        @DisplayName("이미 만료된 accessToken(TTL 0 이하)은 블랙리스트에 등록하지 않는다")
        void 탈퇴_성공_만료된_토큰은_블랙리스트_등록_안함() {
            // given
            Long userId = 12L;
            String expiredAccessToken = "expired-access-token";
            Users user = Users.builder().id(userId).status(UserStatus.ACTIVE).build();
            UserRequestDTO.DeleteReasonDTO reasonDTO = new UserRequestDTO.DeleteReasonDTO();
            reasonDTO.setReason("만료 토큰 테스트");

            given(userRepository.findById(userId)).willReturn(Optional.of(user));
            given(userRepository.save(user)).willReturn(user);
            given(jwtTokenProvider.getRemainingExpiryMs(expiredAccessToken)).willReturn(0L);

            // when
            userWithdrawService.withdrawUser(userId, reasonDTO, expiredAccessToken);

            // then
            then(accessTokenBlackListManager)
                    .should(never())
                    .addToBlacklist(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyLong());
        }

        @Test
        @DisplayName("만료/위조 등으로 토큰 파싱이 실패해도 탈퇴 자체는 정상 처리된다")
        void 탈퇴_성공_토큰파싱_실패해도_탈퇴는_진행() {
            // given
            Long userId = 13L;
            String malformedAccessToken = "malformed-or-expired-token";
            Users user = Users.builder().id(userId).status(UserStatus.ACTIVE).build();
            UserRequestDTO.DeleteReasonDTO reasonDTO = new UserRequestDTO.DeleteReasonDTO();
            reasonDTO.setReason("파싱 실패 테스트");

            given(userRepository.findById(userId)).willReturn(Optional.of(user));
            given(userRepository.save(user)).willReturn(user);
            given(jwtTokenProvider.getRemainingExpiryMs(malformedAccessToken))
                    .willThrow(new RuntimeException("invalid token"));

            // when : 예외가 밖으로 전파되지 않고 탈퇴 결과가 정상 반환되어야 한다
            Users result = userWithdrawService.withdrawUser(userId, reasonDTO, malformedAccessToken);

            // then
            assertThat(result.getStatus()).isEqualTo(UserStatus.INACTIVE);
            then(refreshTokenManager).should(times(1)).deleteAllTokens(userId);
            then(accessTokenBlackListManager)
                    .should(never())
                    .addToBlacklist(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyLong());
        }

        @Test
        @DisplayName("존재하지 않는 유저 ID면 USER_NOT_FOUND 예외가 발생하고 토큰 처리도 하지 않는다")
        void 탈퇴_실패_존재하지않는_유저() {
            // given
            Long userId = 999L;
            UserRequestDTO.DeleteReasonDTO reasonDTO = new UserRequestDTO.DeleteReasonDTO();
            reasonDTO.setReason("탈퇴 사유");
            given(userRepository.findById(userId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> userWithdrawService.withdrawUser(userId, reasonDTO, "any-token"))
                    .isInstanceOf(GeneralException.class)
                    .satisfies(e -> {
                        GeneralException ex = (GeneralException) e;
                        assertThat(ex.getCode()).isEqualTo(UserErrorStatus._USER_NOT_FOUND);
                    });
            then(refreshTokenManager).should(never()).deleteAllTokens(org.mockito.ArgumentMatchers.anyLong());
            then(accessTokenBlackListManager)
                    .should(never())
                    .addToBlacklist(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyLong());
        }
    }

    @Nested
    @DisplayName("성공 케이스")
    class Success {

        @Test
        @DisplayName("INACTIVE 상태이고 14일 이내라면 ACTIVE로 복구되고 액세스/리프레시 토큰이 발급된다")
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
            given(userStatusValidator.isWithinWithdrawGracePeriod(user)).willReturn(true);
            given(authAccountRepository.findEmailByUserIdAndProvider(userId, Provider.GENERAL))
                    .willReturn(Optional.of("user1@example.com"));
            given(tokenIssueService.issueTokenPair(
                            eq(userId), eq("user1@example.com"), eq("GENERAL"), any(),
                            eq("test-device"), eq(DeviceType.PHONE)))
                    .willReturn(new TokenIssueService.IssuedTokenPair("access-token", "refresh-token"));

            // when
            UserResponseDTO.withDrawalResultDTO result =
                    userWithdrawService.recoverUser(userId, "GENERAL", RECOVER_REQUEST);

            // then
            assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
            assertThat(user.getInactiveDate()).isNull();
            assertThat(user.getDeleted_reason()).isNull();
            assertThat(result.getAccessToken()).isEqualTo("access-token");
            assertThat(result.getRefreshToken()).isEqualTo("refresh-token");
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
            given(userStatusValidator.isWithinWithdrawGracePeriod(user)).willReturn(true);
            given(authAccountRepository.findEmailByUserIdAndProvider(userId, Provider.GENERAL))
                    .willReturn(Optional.of("user2@example.com"));
            given(tokenIssueService.issueTokenPair(
                            eq(userId), eq("user2@example.com"), eq("GENERAL"), any(),
                            eq("test-device"), eq(DeviceType.PHONE)))
                    .willReturn(new TokenIssueService.IssuedTokenPair("access-token", "refresh-token"));

            // when
            userWithdrawService.recoverUser(userId, "GENERAL", RECOVER_REQUEST);

            // then
            assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
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
            given(userStatusValidator.isWithinWithdrawGracePeriod(user)).willReturn(true);
            given(authAccountRepository.findEmailByUserIdAndProvider(userId, Provider.GENERAL))
                    .willReturn(Optional.of("user3@example.com"));
            given(tokenIssueService.issueTokenPair(
                            eq(userId), eq("user3@example.com"), eq("GENERAL"), any(),
                            eq("test-device"), eq(DeviceType.PHONE)))
                    .willReturn(new TokenIssueService.IssuedTokenPair("access-token", "refresh-token"));

            // when
            userWithdrawService.recoverUser(userId, "GENERAL", RECOVER_REQUEST);

            // then
            assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
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
            assertThatThrownBy(() -> userWithdrawService.recoverUser(userId, "GENERAL", RECOVER_REQUEST))
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
            assertThatThrownBy(() -> userWithdrawService.recoverUser(userId, "GENERAL", RECOVER_REQUEST))
                    .isInstanceOf(GeneralException.class)
                    .satisfies(e -> {
                        GeneralException ex = (GeneralException) e;
                        assertThat(ex.getCode()).isEqualTo(ErrorStatus._BAD_REQUEST);
                    });
        }

        @Test
        @DisplayName("탈퇴 유예 기간(14일)이 지난 경우 BAD_REQUEST 예외가 발생한다")
        void 유예기간_경과_예외() {
            // given
            Long userId = 4L;
            Users user = Users.builder()
                    .id(userId)
                    .status(UserStatus.INACTIVE)
                    .build();
            user.withdraw("테스트 탈퇴", LocalDateTime.now().minusDays(15));

            given(userRepository.findById(userId)).willReturn(Optional.of(user));
            given(userStatusValidator.isWithinWithdrawGracePeriod(user)).willReturn(false);

            // when & then
            assertThatThrownBy(() -> userWithdrawService.recoverUser(userId, "GENERAL", RECOVER_REQUEST))
                    .isInstanceOf(GeneralException.class)
                    .satisfies(e -> {
                        GeneralException ex = (GeneralException) e;
                        assertThat(ex.getCode()).isEqualTo(ErrorStatus._BAD_REQUEST);
                    });
        }
    }
}
