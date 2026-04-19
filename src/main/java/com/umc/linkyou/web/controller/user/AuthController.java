package com.umc.linkyou.web.controller.user;

import com.umc.linkyou.apiPayload.ApiResponse;
import com.umc.linkyou.apiPayload.code.status.SuccessStatus;
import com.umc.linkyou.converter.UserConverter;
import com.umc.linkyou.domain.Users;
import com.umc.linkyou.service.email.EmailVerificationService;
import com.umc.linkyou.service.email.PasswordResetService;
import com.umc.linkyou.service.users.UserService;
import com.umc.linkyou.utils.UsersUtils;
import com.umc.linkyou.validation.annotation.ApiV1;
import com.umc.linkyou.web.dto.PasswordResetRequestDTO;
import com.umc.linkyou.web.dto.UserRequestDTO;
import com.umc.linkyou.web.dto.UserResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "auth-controller", description = "인증 및 계정 관련 API")
@ApiV1
@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {

    private final UserService userService;
    private final EmailVerificationService emailVerificationService;
    private final PasswordResetService passwordResetService;
    private final UsersUtils usersUtils;

    @Operation(
            summary = "회원 가입 (이메일)",
            description = """
                    이메일 회원가입을 진행합니다.
                    - 이메일, 닉네임, 비밀번호, 성별, 직업, 사용 목적, 관심사를 입력받습니다.
                    - 회원가입이 완료되면 생성된 사용자 정보와 생성 시각을 반환합니다.
                    - 중복 이메일 또는 중복 닉네임이면 예외를 반환합니다.
                    """
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "이메일 회원가입 성공")
    })
    @PostMapping("/signup")
    public ApiResponse<UserResponseDTO.JoinResultDTO> join(@RequestBody @Valid UserRequestDTO.JoinDTO request){
        Users user = userService.joinUser(request);
        return ApiResponse.onSuccess(UserConverter.toJoinResultDTO(user));
    }

    @Operation(
            summary = "유저 로그인",
            description = """
                    이메일과 비밀번호로 로그인합니다.
                    - 로그인에 성공하면 액세스 토큰과 리프레시 토큰을 반환합니다.
                    - 이메일 또는 비밀번호가 올바르지 않으면 로그인 실패 예외를 반환합니다.
                    """
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "유저 로그인 성공")
    })
    @PostMapping("/login")
    public ApiResponse<UserResponseDTO.LoginResultDTO> login(@RequestBody @Valid UserRequestDTO.LoginRequestDTO request) {
        return ApiResponse.onSuccess(userService.loginUser(request));
    }

    @Operation(
            summary = "토큰 재발급",
            description = """
                    Refresh Token을 사용해 액세스 토큰과 리프레시 토큰을 재발급합니다.
                    - 헤더의 `Refresh-Token` 값을 사용합니다.
                    - 유효하지 않거나 만료된 Refresh Token이면 예외를 반환합니다.
                    """
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "토큰 재발급 성공")
    })
    @PostMapping("/token/reissue")
    public ApiResponse<UserResponseDTO.TokenPair> reissueToken(@RequestHeader("Refresh-Token") String refreshToken) {
        return ApiResponse.onSuccess(userService.reissueRefreshToken(refreshToken));
    }


    @Operation(
            summary = "이메일 인증 코드 전송",
            description = """
                    `email`로 회원가입용 이메일 인증 코드를 전송합니다.
                    - 인증 코드는 Redis에 저장되며 10분 동안 유효합니다.
                    - 이미 가입된 이메일이면 중복 가입 에러를 반환합니다.
                    - 메일 전송에 실패하면 인증 코드 전송 실패 에러를 반환합니다.
                    """
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "202", description = "이메일 인증 코드 전송 성공")
    })
    @PostMapping("/email/code")
    public ApiResponse<String> sendCode(@RequestParam("email") @Valid String email) {
        emailVerificationService.sendCode(email);
        return ApiResponse.of(SuccessStatus._VERIFICATION_CODE_SENT, "이메일로 인증 코드가 전송되었습니다.");
    }

    @Operation(
            summary = "이메일 인증 코드 검증",
            description = """
                    `email`과 `code`를 입력받아 이메일 인증 코드를 검증합니다.
                    - Redis에 저장된 인증 코드와 일치하면 이메일 인증을 완료합니다.
                    - Redis에 해당 이메일의 인증 코드가 없으면 인증 코드 만료 에러를 반환합니다.
                    - 인증 코드 만료 시간은 10분입니다.
                    """
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "202", description = "이메일 인증 코드 검증 성공")
    })
    @GetMapping("/email/verify")
    public ApiResponse<String> verifyCode(@RequestParam("email") @Valid String email,
                                          @RequestParam("code") String authCode) {
        emailVerificationService.verifyCode(email, authCode);
        return ApiResponse.of(SuccessStatus._EMAIL_VERIFICATION_SUCCESS, "이메일 인증이 완료되었습니다.");
    }

    @Operation(
            summary = "닉네임 중복 확인",
            description = """
                    `nickname`의 중복 여부를 확인합니다.
                    - 이미 사용 중인 닉네임이면 중복 닉네임 에러를 반환합니다.
                    - 사용 가능한 닉네임이면 성공 응답과 안내 문구를 반환합니다.
                    """
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "202", description = "닉네임 중복 확인 성공")
    })
    @GetMapping("/check-nickname")
    public ApiResponse<String> checkNickname(@RequestParam String nickname) {
        userService.validateNickNameNotDuplicate(nickname);
        return ApiResponse.of(SuccessStatus._NICKNAME_AVAILABLE, "사용 가능한 닉네임 입니다.");
    }

    @Operation(
            summary = "비밀번호 재설정 링크 전송",
            description = """
                    등록된 이메일로 비밀번호 재설정 페이지 링크를 전송합니다.
                    - 일반 로그인 계정에만 비밀번호 재설정 링크를 전송합니다.
                    - 가입되지 않은 이메일이면 사용자 없음 에러를 반환합니다.
                    - 소셜 전용 계정이면 비밀번호 재설정 대신 소셜 로그인 유도 에러를 반환합니다.
                    - 재설정 링크는 Redis에 저장된 토큰 기준으로 10분 동안 유효합니다.
                    """
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "비밀번호 재설정 링크 전송 성공")
    })
    @PostMapping("/password/reset/send")
    public ApiResponse<String> sendPasswordResetLink(@RequestParam("email") String email) {
        passwordResetService.sendResetLink(email);
        return ApiResponse.of(SuccessStatus._RESET_LINK_SENT, "비밀번호 재설정 링크가 이메일로 전송되었습니다.");
    }

    @Operation(
            summary = "비밀번호 재설정",
            description = """
                    토큰, 새 비밀번호, 비밀번호 확인을 입력해 비밀번호를 변경합니다.
                    - `token`은 메일의 비밀번호 재설정 링크에서 전달받은 값입니다.
                    - 새 비밀번호와 비밀번호 확인이 일치하면 비밀번호를 변경합니다.
                    - 토큰이 없거나 만료되면 비밀번호 재설정이 불가능합니다.
                    """
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "비밀번호 재설정 성공")
    })
    @PutMapping("/password/reset")
    public ApiResponse<Void> resetPassword(@RequestBody @Valid PasswordResetRequestDTO request) {
        passwordResetService.resetPassword(request.getToken(), request.getNewPassword(), request.getConfirmPassword());
        return ApiResponse.onSuccess(null);
    }
}
