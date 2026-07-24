package com.umc.linkyou.service.users;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.linkyou.apiPayload.code.status.user.UserErrorStatus;
import com.umc.linkyou.apiPayload.exception.GeneralException;
import com.umc.linkyou.domain.Users;
import com.umc.linkyou.domain.enums.DeviceType;
import com.umc.linkyou.domain.enums.Provider;
import com.umc.linkyou.domain.enums.Role;
import com.umc.linkyou.domain.enums.UserStatus;
import com.umc.linkyou.jwt.TokenIssueService;
import com.umc.linkyou.oauth2.mobile.dto.MobileLoginResponse;

@ExtendWith(MockitoExtension.class)
class UserLoginServiceTest {

    @InjectMocks private UserLoginService userLoginService;

    @Mock private UserStatusValidator userStatusValidator;
    @Mock private TokenIssueService tokenIssueService;

    @Nested
    @DisplayName("handleSocialLogin")
    class HandleSocialLogin {

        @Test
        @DisplayName("성공 - ACTIVE 유저는 access+refresh 토큰 쌍을 받는다")
        void ACTIVE_유저는_토큰_쌍을_받는다() {
            // given
            Users user =
                    Users.builder().id(1L).role(Role.USER).status(UserStatus.ACTIVE).build();
            when(userStatusValidator.isWithinWithdrawGracePeriod(user)).thenReturn(false);
            when(tokenIssueService.issueForStatus(
                            eq(1L), eq("kakao@example.com"), eq("KAKAO"), eq(Role.USER),
                            eq(UserStatus.ACTIVE), eq("device-1"), eq(DeviceType.PHONE)))
                    .thenReturn(new TokenIssueService.IssuedTokenPair("access", "refresh"));

            // when
            MobileLoginResponse result =
                    userLoginService.handleSocialLogin(
                            user, "kakao@example.com", Provider.KAKAO, "device-1", DeviceType.PHONE);

            // then
            assertEquals("access", result.getAccessToken());
            assertEquals("refresh", result.getRefreshToken());
            assertEquals(UserStatus.ACTIVE, result.getStatus());
            verify(userStatusValidator).validateLoginAllowed(user);
        }

        @Test
        @DisplayName("성공 - 탈퇴 유예 기간 이내인 유저는 복구 전용 토큰만 받는다")
        void 유예_기간_이내인_유저는_복구_전용_토큰만_받는다() {
            // given
            Users user =
                    Users.builder().id(2L).role(Role.USER).status(UserStatus.INACTIVE).build();
            user.withdraw("테스트 탈퇴", LocalDateTime.now().minusDays(3));

            when(userStatusValidator.isWithinWithdrawGracePeriod(user)).thenReturn(true);
            when(tokenIssueService.issueRecoveryToken(2L, "kakao@example.com", "KAKAO", Role.USER))
                    .thenReturn("recoveryToken");

            // when
            MobileLoginResponse result =
                    userLoginService.handleSocialLogin(
                            user, "kakao@example.com", Provider.KAKAO, "device-1", DeviceType.PHONE);

            // then
            assertEquals("recoveryToken", result.getAccessToken());
            assertNull(result.getRefreshToken());
            assertEquals(UserStatus.INACTIVE, result.getStatus());
            assertEquals(user.getInactiveDate(), result.getInactiveDate());
            verify(userStatusValidator, never()).validateLoginAllowed(any());
            verify(tokenIssueService, never())
                    .issueForStatus(any(), any(), any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("실패 - 탈퇴 유예 기간이 지난 유저는 로그인이 차단된다")
        void 유예_기간이_지난_유저는_로그인이_차단된다() {
            // given
            Users user =
                    Users.builder().id(3L).role(Role.USER).status(UserStatus.INACTIVE).build();
            user.withdraw("테스트 탈퇴", LocalDateTime.now().minusDays(20));

            when(userStatusValidator.isWithinWithdrawGracePeriod(user)).thenReturn(false);
            doThrow(new GeneralException(UserErrorStatus._USER_INACTIVE))
                    .when(userStatusValidator)
                    .validateLoginAllowed(user);

            // when & then
            GeneralException ex =
                    assertThrows(
                            GeneralException.class,
                            () ->
                                    userLoginService.handleSocialLogin(
                                            user,
                                            "kakao@example.com",
                                            Provider.KAKAO,
                                            "device-1",
                                            DeviceType.PHONE));
            assertEquals(UserErrorStatus._USER_INACTIVE, ex.getCode());
        }
    }
}
