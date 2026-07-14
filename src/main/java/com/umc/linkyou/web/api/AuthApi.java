package com.umc.linkyou.web.api;

import com.umc.linkyou.apiPayload.ApiResponse;
import com.umc.linkyou.apiPayload.code.status.CommonErrorStatus;
import com.umc.linkyou.apiPayload.code.status.ErrorStatus;
import com.umc.linkyou.apiPayload.code.status.auth.AuthErrorStatus;
import com.umc.linkyou.apiPayload.code.status.auth.AuthSuccessStatus;
import com.umc.linkyou.apiPayload.code.status.user.UserErrorStatus;
import com.umc.linkyou.validation.annotation.swagger.ApiAuthSuccessCode;
import com.umc.linkyou.validation.annotation.swagger.ApiErrorCode;
import com.umc.linkyou.jwt.CurrentUser;
import com.umc.linkyou.jwt.CustomUserDetails;
import com.umc.linkyou.web.dto.EmailRequestDTO;
import com.umc.linkyou.web.dto.PasswordResetRequestDTO;
import com.umc.linkyou.web.dto.UserRequestDTO;
import com.umc.linkyou.web.dto.UserResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@Tag(name = "auth-controller", description = "인증 및 계정 관련 API")
@RequestMapping("/auth")
public interface AuthApi {

    @Operation(
            summary = "회원 가입 (이메일)",
            description = """
                    이메일 회원가입을 진행합니다.
                    - 이메일, 닉네임, 비밀번호, 성별, 직업, 사용 목적, 관심사를 입력받습니다.
                    - 회원가입이 완료되면 생성된 사용자 정보와 생성 시각을 반환합니다.
                    - 중복 이메일 또는 중복 닉네임이면 예외를 반환합니다.
                    """
    )
    @ApiAuthSuccessCode(AuthSuccessStatus.JOIN_SUCCESS)
    @ApiErrorCode(errorStatus = {ErrorStatus._BAD_REQUEST})
    @ApiErrorCode(userErrorStatus = {UserErrorStatus._DUPLICATE_JOIN_REQUEST, UserErrorStatus._DUPLICATE_NICKNAME})
    @PostMapping("/signup")
    ApiResponse<UserResponseDTO.JoinResultDTO> join(@RequestBody @Valid UserRequestDTO.JoinDTO request);

    @Operation(
            summary = "유저 로그인",
            description = """
                    이메일과 비밀번호로 로그인합니다.
                    - 로그인에 성공하면 액세스 토큰과 리프레시 토큰을 반환합니다.
                    - 이메일 또는 비밀번호가 올바르지 않으면 로그인 실패 예외를 반환합니다.
                    - `deviceId`는 클라이언트가 생성한 디바이스 고유 식별자입니다. 앱 최초 설치 시 UUID를 생성해 로컬에 저장하고 매 요청마다 동일한 값을 사용합니다.
                      - iOS: `ios-{UUID}` (예: ios-550e8400-e29b-41d4-a716-446655440000)
                      - Android: `android-{UUID}` (예: android-550e8400-e29b-41d4-a716-446655440000)
                    - 동일 계정으로 최대 3개 디바이스까지 동시 로그인이 가능하며, 초과 시 가장 오래된 세션이 자동 만료됩니다.
                    """
    )
    @ApiAuthSuccessCode(AuthSuccessStatus.LOGIN_SUCCESS)
    @ApiErrorCode(userErrorStatus = {UserErrorStatus._LOGIN_FAILED, UserErrorStatus._USER_INACTIVE, UserErrorStatus._SOCIAL_ACCOUNT_ONLY, UserErrorStatus._USER_NOT_FOUND})
    @PostMapping("/login")
    ApiResponse<UserResponseDTO.LoginResultDTO> login(@RequestBody @Valid UserRequestDTO.LoginRequestDTO request);

    @Operation(
            summary = "토큰 재발급",
            description = """
                    Refresh Token을 사용해 액세스 토큰과 리프레시 토큰을 재발급합니다.
                    - body의 `refreshToken`, `deviceId` 값을 사용합니다.
                    - `deviceId`는 로그인 시 사용한 값과 동일한 값을 전달해야 합니다. 불일치 시 인증 실패를 반환합니다.
                    - 재발급 시 기존 Refresh Token은 즉시 무효화되며 새 토큰이 발급됩니다 (Refresh Token Rotation).
                    - 유효하지 않거나 만료된 Refresh Token이면 예외를 반환합니다.
                    """
    )
    @ApiAuthSuccessCode(AuthSuccessStatus.TOKEN_REISSUE_SUCCESS)
    @ApiErrorCode(authErrorStatus = {AuthErrorStatus.UNAUTHORIZED})
    @ApiErrorCode(userErrorStatus = {
            UserErrorStatus._INVALID_REFRESH_TOKEN,
            UserErrorStatus._REFRESH_TOKEN_SESSION_INVALID,
            UserErrorStatus._REFRESH_TOKEN_PARSE_ERROR
    })
    @PostMapping("/token/reissue")
    ApiResponse<UserResponseDTO.TokenPair> reissueToken(@RequestBody @Valid UserRequestDTO.TokenReissueRequestDTO request);

    @Operation(
            summary = "이메일 인증 코드 전송",
            description = """
                    body의 `email`로 회원가입용 이메일 인증 코드를 전송합니다.
                    - 인증 코드는 Redis에 저장되며 10분 동안 유효합니다.
                    - 이미 가입된 이메일이면 중복 가입 에러를 반환합니다.
                    - 메일 전송에 실패하면 인증 코드 전송 실패 에러를 반환합니다.
                    """
    )
    @ApiAuthSuccessCode(AuthSuccessStatus.SEND_VERIFICATION_CODE)
    @ApiErrorCode(userErrorStatus = {UserErrorStatus._DUPLICATE_JOIN_REQUEST})
    @ApiErrorCode(commonErrorStatus = {CommonErrorStatus._TOO_MANY_REQUESTS})
    @ApiErrorCode(userErrorStatus = {UserErrorStatus._SEND_MAIL_FAILED})
    @PostMapping("/email/code")
    ApiResponse<Object> sendCode(@RequestBody @Valid EmailRequestDTO.CodeSendDTO request);

    @Operation(
            summary = "이메일 인증 코드 검증",
            description = """
                    `email`과 `code`를 입력받아 이메일 인증 코드를 검증합니다.
                    - Redis에 저장된 인증 코드와 일치하면 이메일 인증을 완료합니다.
                    - Redis에 해당 이메일의 인증 코드가 없으면 인증 코드 만료 에러를 반환합니다.
                    - 인증 코드 만료 시간은 10분입니다.
                    """
    )
    @ApiAuthSuccessCode(AuthSuccessStatus.EMAIL_VERIFICATION_SUCCESS)
    @ApiErrorCode(userErrorStatus = {UserErrorStatus._EXPIRED_VERIFICATION_CODE})
    @ApiErrorCode(userErrorStatus = {UserErrorStatus._VERIFICATION_FAILED})
    @PostMapping("/email/verify")
    ApiResponse<Object> verifyCode(@RequestBody @Valid EmailRequestDTO.CodeVerifyDTO request);

    @Operation(
            summary = "닉네임 중복 확인",
            description = """
                    `nickname`의 중복 여부를 확인합니다.
                    - 이미 사용 중인 닉네임이면 중복 닉네임 에러를 반환합니다.
                    - 사용 가능한 닉네임이면 성공 응답과 안내 문구를 반환합니다.
                    """
    )
    @ApiAuthSuccessCode(AuthSuccessStatus.NICKNAME_AVAILABLE)
    @ApiErrorCode(userErrorStatus = {UserErrorStatus._DUPLICATE_NICKNAME})
    @GetMapping("/check-nickname")
    ApiResponse<Object> checkNickname(@RequestParam String nickname);

    @Operation(
            summary = "비밀번호 재설정 링크 전송",
            description = """
                    body의 `email`로 비밀번호 재설정 링크를 전송합니다.
                    - 일반(이메일) 가입 계정에만 재설정 링크가 전송됩니다.
                    - 재설정 토큰은 Redis에 저장되며 10분 동안 유효합니다.
                    - 전송 쿨다운(60초)과 일일 전송 횟수 제한(5회)이 적용되어 남용을 방지합니다.
                    - 가입 여부 노출을 막기 위해, 존재하지 않는 이메일이어도 성공 응답을 반환합니다.
                    - 유효하지 않은 이메일 주소이거나 메일 전송에 실패하면 예외를 반환합니다.
                    """
    )
    @ApiAuthSuccessCode(AuthSuccessStatus.PASSWORD_RESET_LINK_SENT)
    @ApiErrorCode(userErrorStatus = {UserErrorStatus._INVALID_EMAIL_ADDRESS, UserErrorStatus._SEND_MAIL_FAILED})
    @ApiErrorCode(commonErrorStatus = {CommonErrorStatus._TOO_MANY_REQUESTS})
    @PostMapping("/password/reset/send")
    ApiResponse<Object> sendPasswordResetLink(@RequestBody @Valid EmailRequestDTO.ResetLinkDTO request);

    @Operation(hidden = true)
    @PostMapping("/password/reset")
    ApiResponse<Object> resetPassword(@RequestBody @Valid PasswordResetRequestDTO request);

    @Operation(
            summary = "로그아웃",
            description = """
                    현재 로그인된 사용자를 로그아웃합니다.
                    - 요청의 `deviceId`에 해당하는 현재 디바이스 세션만 로그아웃합니다.
                    - Authorization Bearer 헤더의 Access Token을 사용합니다.
                    - 현재 액세스 토큰을 블랙리스트에 등록하여 즉시 무효화합니다.
                    """
    )
    @ApiAuthSuccessCode(AuthSuccessStatus.LOGOUT_SUCCESS)
    @ApiErrorCode(userErrorStatus = {UserErrorStatus._USER_NOT_FOUND})
    @PostMapping("/logout")
    ApiResponse<Object> logout(@CurrentUser CustomUserDetails userDetails, @RequestParam String deviceId, HttpServletRequest request);
}
