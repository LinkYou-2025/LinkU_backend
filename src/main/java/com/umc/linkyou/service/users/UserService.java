package com.umc.linkyou.service.users;

import com.umc.linkyou.config.security.jwt.CustomUserDetails;
import com.umc.linkyou.domain.Users;
import com.umc.linkyou.web.dto.UserRequestDTO;
import com.umc.linkyou.web.dto.UserResponseDTO;
import jakarta.validation.Valid;

public interface UserService {

    Users joinUser(UserRequestDTO.JoinDTO request);
    //소셜로그인시 회원가입로직
    Users socialCompleteProfile(Users user, UserRequestDTO.SocialCompleteDTO request);

    UserResponseDTO.LoginResultDTO loginUser(UserRequestDTO.LoginRequestDTO request);

    void validateNickNameNotDuplicate(String nickname);

    // 마이페이지 조회
    UserResponseDTO.UserProfileSummaryDto userInfo(Long userId, String loginProvider);

    // 마이페이지 수정
    void updateUserProfile(Long userId, UserRequestDTO.UpdateProfileDTO updateDTO);

    UserResponseDTO.TokenPair reissueRefreshToken(String refreshToken);

}
