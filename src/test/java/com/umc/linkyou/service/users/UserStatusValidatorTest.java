package com.umc.linkyou.service.users;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.umc.linkyou.apiPayload.code.status.user.UserErrorStatus;
import com.umc.linkyou.apiPayload.exception.GeneralException;
import com.umc.linkyou.domain.Users;
import com.umc.linkyou.domain.enums.UserStatus;

class UserStatusValidatorTest {

    private final UserStatusValidator validator = new UserStatusValidator();

    @Nested
    @DisplayName("validateLoginAllowed")
    class ValidateLoginAllowed {

        @Test
        @DisplayName("INACTIVE 유저는 로그인이 차단된다")
        void INACTIVE_유저는_로그인이_차단된다() {
            Users user = Users.builder().id(1L).status(UserStatus.INACTIVE).build();

            GeneralException ex =
                    org.junit.jupiter.api.Assertions.assertThrows(
                            GeneralException.class, () -> validator.validateLoginAllowed(user));
            assertEquals(UserErrorStatus._USER_INACTIVE, ex.getCode());
        }

        @Test
        @DisplayName("ACTIVE 유저는 로그인이 허용된다")
        void ACTIVE_유저는_로그인이_허용된다() {
            Users user = Users.builder().id(1L).status(UserStatus.ACTIVE).build();
            org.junit.jupiter.api.Assertions.assertDoesNotThrow(
                    () -> validator.validateLoginAllowed(user));
        }

        @Test
        @DisplayName("TEMP 유저는 로그인이 허용된다")
        void TEMP_유저는_로그인이_허용된다() {
            Users user = Users.builder().id(1L).status(UserStatus.TEMP).build();
            org.junit.jupiter.api.Assertions.assertDoesNotThrow(
                    () -> validator.validateLoginAllowed(user));
        }
    }

    @Nested
    @DisplayName("isWithinWithdrawGracePeriod")
    class IsWithinWithdrawGracePeriod {

        @Test
        @DisplayName("ACTIVE 유저는 유예 대상이 아니다")
        void ACTIVE_유저는_유예_대상이_아니다() {
            Users user = Users.builder().id(1L).status(UserStatus.ACTIVE).build();
            assertFalse(validator.isWithinWithdrawGracePeriod(user));
        }

        @Test
        @DisplayName("INACTIVE지만 inactiveDate가 없으면 유예 대상이 아니다")
        void inactiveDate가_없으면_유예_대상이_아니다() {
            Users user = Users.builder().id(1L).status(UserStatus.INACTIVE).build();
            assertFalse(validator.isWithinWithdrawGracePeriod(user));
        }

        @Test
        @DisplayName("탈퇴 후 7일이면 유예 기간 이내다")
        void 탈퇴_후_7일이면_유예_기간_이내다() {
            Users user = Users.builder().id(1L).status(UserStatus.INACTIVE).build();
            user.withdraw("테스트", LocalDateTime.now().minusDays(7));
            assertTrue(validator.isWithinWithdrawGracePeriod(user));
        }

        @Test
        @DisplayName("탈퇴 직후(1분 이내)에도 유예 기간 이내다")
        void 탈퇴_직후에도_유예_기간_이내다() {
            Users user = Users.builder().id(1L).status(UserStatus.INACTIVE).build();
            user.withdraw("테스트", LocalDateTime.now().minusMinutes(1));
            assertTrue(validator.isWithinWithdrawGracePeriod(user));
        }

        @Test
        @DisplayName("탈퇴 후 정확히 13일 23시간이면 유예 기간 이내다 (경계값)")
        void 유예기간_경계값에서는_이내로_판단한다() {
            Users user = Users.builder().id(1L).status(UserStatus.INACTIVE).build();
            user.withdraw("테스트", LocalDateTime.now().minusDays(13).minusHours(23));
            assertTrue(validator.isWithinWithdrawGracePeriod(user));
        }

        @Test
        @DisplayName("탈퇴 후 15일이 지나면 유예 기간이 지난 것으로 판단한다")
        void 탈퇴_후_15일이_지나면_유예_기간이_지난_것으로_판단한다() {
            Users user = Users.builder().id(1L).status(UserStatus.INACTIVE).build();
            user.withdraw("테스트", LocalDateTime.now().minusDays(15));
            assertFalse(validator.isWithinWithdrawGracePeriod(user));
        }
    }
}
