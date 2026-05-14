package com.umc.linkyou.web.controller.user;

import com.umc.linkyou.apiPayload.ApiResponse;
import com.umc.linkyou.apiPayload.code.status.auth.AuthSuccessStatus;
import com.umc.linkyou.converter.UserConverter;
import com.umc.linkyou.domain.Users;
import com.umc.linkyou.jwt.CurrentUser;
import com.umc.linkyou.jwt.CustomUserDetails;
import com.umc.linkyou.service.email.EmailVerificationService;
import com.umc.linkyou.service.email.PasswordResetService;
import com.umc.linkyou.service.users.UserService;
import com.umc.linkyou.validation.annotation.ApiV1;
import com.umc.linkyou.web.api.AuthApi;
import com.umc.linkyou.web.dto.EmailRequestDTO;
import com.umc.linkyou.web.dto.PasswordResetRequestDTO;
import com.umc.linkyou.web.dto.UserRequestDTO;
import com.umc.linkyou.web.dto.UserResponseDTO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestBody;
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
        return ApiResponse.onSuccess(AuthSuccessStatus.JOIN_SUCCESS, UserConverter.toJoinResultDTO(user));
    }

    @Override
    public ApiResponse<UserResponseDTO.LoginResultDTO> login(@RequestBody @Valid UserRequestDTO.LoginRequestDTO request) {
        return ApiResponse.onSuccess(AuthSuccessStatus.LOGIN_SUCCESS, userService.loginUser(request));
    }

    @Override
    public ApiResponse<UserResponseDTO.TokenPair> reissueToken(@RequestBody @Valid UserRequestDTO.TokenReissueRequestDTO request) {
        return ApiResponse.onSuccess(AuthSuccessStatus.TOKEN_REISSUE_SUCCESS, userService.reissueRefreshToken(request));
    }

    @Override
    public ApiResponse<Object> sendCode(@RequestBody @Valid EmailRequestDTO.CodeSendDTO request) {
        emailVerificationService.sendCode(request.email());
        return ApiResponse.onSuccess(
                AuthSuccessStatus.SEND_VERIFICATION_CODE);
    }

    @Override
    public ApiResponse<Object> verifyCode(@RequestBody @Valid EmailRequestDTO.CodeVerifyDTO request) {
        emailVerificationService.verifyCode(request.email(), request.code());
        return ApiResponse.onSuccess(
                AuthSuccessStatus.EMAIL_VERIFICATION_SUCCESS);
    }

    @Override
    public ApiResponse<Object> checkNickname(@RequestParam String nickname) {
        userService.checkNicknameAvailable(nickname);
        return ApiResponse.onSuccess(AuthSuccessStatus.NICKNAME_AVAILABLE);
    }

    @Override
    public ApiResponse<Object> sendPasswordResetLink(@RequestBody @Valid EmailRequestDTO.ResetLinkDTO request) {
        passwordResetService.sendResetLink(request.email());
        return ApiResponse.onSuccess(AuthSuccessStatus.PASSWORD_RESET_LINK_SENT);
    }

    @Override
    public ApiResponse<Object> resetPassword(@RequestBody @Valid PasswordResetRequestDTO request) {
        passwordResetService.resetPassword(request.getToken(), request.getNewPassword(), request.getConfirmPassword());
        return ApiResponse.onSuccess(AuthSuccessStatus.PASSWORD_RESET_SUCCESS);
    }

    @Override
    public ApiResponse<Object> logout(
            @CurrentUser CustomUserDetails userDetails,
            @RequestParam String deviceId,
            HttpServletRequest request
    ) {
        userService.logoutUser(userDetails.getUserId(), request, deviceId);
        return ApiResponse.onSuccess(AuthSuccessStatus.LOGOUT_SUCCESS);
    }
}
