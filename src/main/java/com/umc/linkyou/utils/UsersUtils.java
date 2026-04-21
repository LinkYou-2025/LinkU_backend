package com.umc.linkyou.utils;

import com.umc.linkyou.apiPayload.code.status.ErrorStatus;
import com.umc.linkyou.apiPayload.code.status.user.UserErrorStatus;
import com.umc.linkyou.apiPayload.exception.handler.UserHandler;
import com.umc.linkyou.config.security.jwt.CustomUserDetails;
import com.umc.linkyou.domain.Users;
import com.umc.linkyou.domain.enums.UserStatus;
import com.umc.linkyou.repository.userRepository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UsersUtils {
    private final UserRepository userRepository;

    public Long getAuthenticatedUserId(CustomUserDetails userDetails) {
        if (userDetails == null) {
            throw new UserHandler(ErrorStatus._INVALID_TOKEN);
        }
        return userDetails.getUsers().getId();
    }
    //token이 올바른지, 사용자가 존재하는지, 사용자가 있다면 activated 상태인지
    public Users validateUser(CustomUserDetails userDetails) {
        if (userDetails == null) {
            throw new UserHandler(ErrorStatus._INVALID_TOKEN);
        }

        Long userId = userDetails.getUsers().getId();
        return userRepository.findNotInactiveUserById(userId)
                .orElseThrow(() -> {
                    if (!userRepository.existsById(userId)) {
                        return new UserHandler(UserErrorStatus._USER_NOT_FOUND);
                    }
                    return new UserHandler(UserErrorStatus._USER_INACTIVE);
                });
    }

    public Users validateTempUser(CustomUserDetails userDetails) {

        // 1. JWT 토큰이 없거나 인증 안 된 요청 차단
        if (userDetails == null) {
            throw new UserHandler(ErrorStatus._INVALID_TOKEN);
        }

        // 2. JWT에 담긴 Users 객체에서 userId 꺼냄
        Long userId = userDetails.getUsers().getId();

        // 3. userId로 DB 조회 → 없으면 404
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new UserHandler(UserErrorStatus._USER_NOT_FOUND));

        // 4. 조회된 유저가 TEMP 상태인지 확인 → 아니면 에러
        //    (이미 프로필 완성한 ACTIVE 유저가 이 API 재호출하는 것 방지)
        if (user.getStatus() != UserStatus.TEMP) {
            throw new UserHandler(ErrorStatus._ALREADY_ACTIVE_USER);
        }

        // 5. TEMP 유저 반환 → 컨트롤러에서 서비스로 넘김
        return user;
    }

    public void validateNickNameNotDuplicate(String nickname) {
        if (userRepository.findByNickName(nickname).isPresent()) {
            throw new UserHandler(UserErrorStatus._DUPLICATE_NICKNAME);
        }
    }
}


