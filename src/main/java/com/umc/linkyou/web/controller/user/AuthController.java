package com.umc.linkyou.web.controller.user;

import com.umc.linkyou.apiPayload.ApiResponse;
import com.umc.linkyou.apiPayload.code.status.SuccessStatus;
import com.umc.linkyou.converter.UserConverter;
import com.umc.linkyou.domain.Users;
import com.umc.linkyou.service.email.EmailVerificationService;
import com.umc.linkyou.service.email.PasswordResetService;
import com.umc.linkyou.service.users.UserService;
import com.umc.linkyou.validation.annotation.ApiV1;
import com.umc.linkyou.web.api.AuthApi;
import com.umc.linkyou.web.dto.EmailRequestDTO;
import com.umc.linkyou.web.dto.PasswordResetRequestDTO;
import com.umc.linkyou.web.dto.UserRequestDTO;
import com.umc.linkyou.web.dto.UserResponseDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

@ApiV1
@RequiredArgsConstructor
public class AuthController implements AuthApi {

    private final UserService userService;
    private final EmailVerificationService emailVerificationService;
    private final PasswordResetService passwordResetService;

    @Override
    public ApiResponse<UserResponseDTO.JoinResultDTO> join(@RequestBody @Valid UserRequestDTO.JoinDTO request) {
        Users user = userService.joinUser(request);
        return ApiResponse.onSuccess(UserConverter.toJoinResultDTO(user));
    }

    @Override
    public ApiResponse<UserResponseDTO.LoginResultDTO> login(@RequestBody @Valid UserRequestDTO.LoginRequestDTO request) {
        return ApiResponse.onSuccess(userService.loginUser(request));
    }

    @Override
    public ApiResponse<UserResponseDTO.TokenPair> reissueToken(@RequestHeader("Refresh-Token") String refreshToken) {
        return ApiResponse.onSuccess(userService.reissueRefreshToken(refreshToken));
    }

    @Override
    public ApiResponse<String> sendCode(@RequestBody @Valid EmailRequestDTO.CodeSendDTO request) {
        emailVerificationService.sendCode(request.email());
        return ApiResponse.of(SuccessStatus._VERIFICATION_CODE_SENT, "이메일로 인증 코드가 전송되었습니다.");
    }

    @Override
    public ApiResponse<String> verifyCode(@RequestBody @Valid EmailRequestDTO.CodeVerifyDTO request) {
        emailVerificationService.verifyCode(request.email(), request.code());
        return ApiResponse.of(SuccessStatus._EMAIL_VERIFICATION_SUCCESS, "이메일 인증이 완료되었습니다.");
    }

    @Override
    public ApiResponse<String> checkNickname(@RequestParam String nickname) {
        userService.validateNickNameNotDuplicate(nickname);
        return ApiResponse.of(SuccessStatus._NICKNAME_AVAILABLE, "사용 가능한 닉네임 입니다.");
    }

    @Override
    public ApiResponse<String> sendPasswordResetLink(@RequestBody @Valid EmailRequestDTO.ResetLinkDTO request) {
        passwordResetService.sendResetLink(request.email());
        return ApiResponse.of(SuccessStatus._RESET_LINK_SENT, "비밀번호 재설정 링크가 이메일로 전송되었습니다.");
    }

    @Override
    public ApiResponse<Void> resetPassword(@RequestBody @Valid PasswordResetRequestDTO request) {
        passwordResetService.resetPassword(request.getToken(), request.getNewPassword(), request.getConfirmPassword());
        return ApiResponse.onSuccess(null);
    }
}
