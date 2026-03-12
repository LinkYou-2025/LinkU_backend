package com.umc.linkyou.service.users;

import com.umc.linkyou.config.security.jwt.CustomUserDetails;
import com.umc.linkyou.domain.Users;
import com.umc.linkyou.web.dto.EmailVerificationResponse;
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

    // 이메일 인증
    // 인증 코드 전송
    void sendCode(String toEmail);

    // 인증 코드 생성
    private String createCode() {
        return null;
    }

    // 인증 코드 검증
    EmailVerificationResponse verifyCode(String email, String authCode);

    // 임시 비밀번호 전송
    void sendTempPassword(String email);

    Users withdrawUser(Long userId, UserRequestDTO.DeleteReasonDTO deleteReasonDTO);

    UserResponseDTO.TokenPair reissueRefreshToken(String refreshToken);

    void testImmediateDelete(Long userId);
}
