package com.umc.linkyou.oauth2;

import com.umc.linkyou.domain.AuthAccount;
import com.umc.linkyou.domain.Users;
import com.umc.linkyou.domain.enums.Provider;
import com.umc.linkyou.repository.authAccountRepository.AuthAccountRepository;
import com.umc.linkyou.repository.userRepository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserSocialLoginHelperTest {

    @InjectMocks
    private UserSocialLoginHelper userSocialLoginHelper;
    @Mock private AuthAccountRepository authAccountRepository;

    @Test
    @DisplayName("기존에 일반 계정이 있는 이메일로 소셜 로그인 시, 기존 유저를 반환한다.")
    void findOrCreateUser_LinkToExistingGeneral() {
        // given
        String email = "test@example.com";
        String externalId = "kakao_12345";
        Users existingUser = Users.builder().id(10L).nickName("일반유저").build();

        // 소셜 계정은 없지만
        when(authAccountRepository.findByProviderAndExternalId(Provider.KAKAO, externalId))
                .thenReturn(Optional.empty());
        // 이메일이 같은 유저는 존재함
        when(authAccountRepository.findUserByEmailExcludingProvider(email, Provider.KAKAO))
                .thenReturn(Optional.of(existingUser));

        // when
        Users result = userSocialLoginHelper.findOrCreateUser(email, "카카오닉네임", externalId, "photo.jpg", Provider.KAKAO);

        // then
        assertEquals(10L, result.getId()); // 기존 유저 ID와 동일한지 확인
        verify(authAccountRepository, times(1)).save(any(AuthAccount.class)); // 새로운 소셜 AuthAccount가 저장되었는지 확인
    }
}
