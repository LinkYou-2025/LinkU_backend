package com.umc.linkyou.web.controller.user;

import com.umc.linkyou.apiPayload.ApiResponse;
import com.umc.linkyou.apiPayload.code.status.SuccessStatus;
import com.umc.linkyou.config.security.jwt.CustomUserDetails;
import com.umc.linkyou.converter.UserConverter;
import com.umc.linkyou.domain.Users;
import com.umc.linkyou.service.users.UserService;
import com.umc.linkyou.utils.UsersUtils;
import com.umc.linkyou.validation.annotation.ApiV1;
import com.umc.linkyou.web.dto.EmailVerificationResponse;
import com.umc.linkyou.web.dto.UserRequestDTO;
import com.umc.linkyou.web.dto.UserResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "auth-controller", description = "인증 및 계정 관련 API")
@ApiV1
@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {

    private final UserService userService;
    private final UsersUtils usersUtils;

    @Operation(summary = "회원 가입 (이메일)", description = "이메일을 통해 회원가입을 진행합니다.")
    @PostMapping("/signup")
    public ApiResponse<UserResponseDTO.JoinResultDTO> join(@RequestBody @Valid UserRequestDTO.JoinDTO request){
        Users user = userService.joinUser(request);
        return ApiResponse.onSuccess(UserConverter.toJoinResultDTO(user));
    }

    @Operation(summary = "유저 로그인")
    @PostMapping("/login")
    public ApiResponse<UserResponseDTO.LoginResultDTO> login(@RequestBody @Valid UserRequestDTO.LoginRequestDTO request) {
        return ApiResponse.onSuccess(userService.loginUser(request));
    }

    @Operation(summary = "토큰 재발급", description = "Refresh Token을 사용하여 토큰을 재발급합니다.")
    @PostMapping("/token/reissue")
    public ApiResponse<UserResponseDTO.TokenPair> reissueToken(@RequestHeader("Refresh-Token") String refreshToken) {
        return ApiResponse.onSuccess(userService.reissueRefreshToken(refreshToken));
    }


    @Operation(summary = "이메일 인증 코드 전송")
    @PostMapping("/email/code")
    // TODO: SendGrid 환경변수 누락으로 현재 이메일 발송 안 됨
    public ApiResponse<String> sendCode(@RequestParam("email") @Valid String email) {
        userService.sendCode(email);
        return ApiResponse.of(SuccessStatus._VERIFICATION_CODE_SENT, "이메일로 인증 코드가 전송되었습니다.");
    }

    //TODO : get 방식은 url 쿼리파라미터 노출 문제가 있습니다. post 방식으로 변경 요청드립니다.
    @Operation(summary = "이메일 인증 코드 검증")
    @GetMapping("/email/verify")
    public ApiResponse<EmailVerificationResponse> verifyCode(@RequestParam("email") @Valid String email,
                                                             @RequestParam("code") String authCode) {
        EmailVerificationResponse response = userService.verifyCode(email, authCode);
        return ApiResponse.of(SuccessStatus._EMAIL_VERIFICATION_SUCCESS, response);
    }

    @Operation(summary = "닉네임 중복 확인")
    @GetMapping("/check-nickname")
    public ApiResponse<String> checkNickname(@RequestParam String nickname) {
        userService.validateNickNameNotDuplicate(nickname);
        return ApiResponse.of(SuccessStatus._NICKNAME_AVAILABLE, "사용 가능한 닉네임 입니다.");
    }
}
