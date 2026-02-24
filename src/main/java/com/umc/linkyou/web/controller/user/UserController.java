package com.umc.linkyou.web.controller.user;

import com.umc.linkyou.apiPayload.ApiResponse;
import com.umc.linkyou.apiPayload.code.status.ErrorStatus;
import com.umc.linkyou.apiPayload.code.status.SuccessStatus;
import com.umc.linkyou.apiPayload.exception.GeneralException;
import com.umc.linkyou.config.security.jwt.CustomUserDetails;
import com.umc.linkyou.converter.UserConverter;
import com.umc.linkyou.domain.Users;
import com.umc.linkyou.service.users.TermsAgreementService;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "user-controller", description = "사용자 관련 API")
@Slf4j
@ApiV1
@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
public class UserController {

    private final UserService userService;
    private final UsersUtils usersUtils;
    private final TermsAgreementService termsAgreementService;

    @Operation(
            summary = "회원 가입",
            description = "새로운 사용자를 등록합니다. 이메일, 비밀번호, 닉네임, 성별, 직업, 목적, 관심사 등을 입력받습니다."
    )
    @PostMapping("/join")
    public ApiResponse<UserResponseDTO.JoinResultDTO> join(@RequestBody @Valid UserRequestDTO.JoinDTO request){
        Users user = userService.joinUser(request);
        return ApiResponse.onSuccess(UserConverter.toJoinResultDTO(user));
    }

    @Operation(
            summary = "유저 로그인",
            description = "이메일과 비밀번호로 로그인합니다. 성공 시 Access Token과 Refresh Token을 반환합니다."
    )
    @PostMapping("/login")
    public ApiResponse<UserResponseDTO.LoginResultDTO> login(@RequestBody @Valid UserRequestDTO.LoginRequestDTO request) {
        return ApiResponse.onSuccess(userService.loginUser(request));
    }

    @Operation(
            summary = "토큰 재발급",
            description = "Refresh Token을 사용하여 새로운 Access Token과 Refresh Token을 발급받습니다."
    )
    @PostMapping("/reissue")
    public ApiResponse<UserResponseDTO.TokenPair> reissueToken(@RequestHeader("Refresh-Token") String refreshToken) {
        return ApiResponse.onSuccess(userService.reissueRefreshToken(refreshToken));
    }

    @Operation(
            summary = "닉네임 중복 확인",
            description = "입력한 닉네임이 이미 사용 중인지 확인합니다."
    )
    @GetMapping("/check-nickname")
    public ApiResponse<String> checkNickname(@RequestParam String nickname) {
        userService.validateNickNameNotDuplicate(nickname);
        return ApiResponse.of(SuccessStatus._NICKNAME_AVAILABLE, "사용 가능한 닉네임 입니다.");
    }

    @Operation(
            summary = "이메일 인증 코드 전송",
            description = "입력한 이메일 주소로 인증 코드를 전송합니다."
    )
    @PostMapping("/emails/code")
    public ApiResponse<String> sendCode(@RequestParam("email") @Valid String email) {
        userService.sendCode(email);
        return ApiResponse.of(SuccessStatus._VERIFICATION_CODE_SENT, "이메일로 인증 코드가 전송되었습니다.");
    }

    @Operation(
            summary = "이메일 인증 코드 검증",
            description = "전송받은 인증 코드를 검증합니다."
    )
    @GetMapping("/emails/verify")
    public ApiResponse<EmailVerificationResponse> verifyCode(@RequestParam("email") @Valid String email,
                                                             @RequestParam("code") String authCode) {
        EmailVerificationResponse response = userService.verifyCode(email, authCode);
        return ApiResponse.of(SuccessStatus._EMAIL_VERIFICATION_SUCCESS, response);
    }

    @Operation(
            summary = "마이페이지 조회",
            description = "사용자의 프로필 정보를 조회합니다."
    )
    @GetMapping("/me")
    public ApiResponse<UserResponseDTO.UserProfileSummaryDto> getUserInfo(@AuthenticationPrincipal CustomUserDetails userDetails) {
        Long newUserId = userDetails.getUsers().getId();  //보안 문제 때문에 추가함
        return ApiResponse.onSuccess(userService.userInfo(newUserId));
    }

    @Operation(
            summary = "마이페이지 수정",
            description = "사용자의 프로필 정보(닉네임, 성별, 직업, 목적, 관심사 등)를 수정합니다."
    )
    @PatchMapping("/profile")
    public ApiResponse<String> updateUserProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody UserRequestDTO.UpdateProfileDTO updateDTO
    ) {
        Long userId = usersUtils.getAuthenticatedUserId(userDetails);
        userService.updateUserProfile(userId, updateDTO);

        return ApiResponse.onSuccess("성공입니다.", "마이페이지가 수정되었습니다.");
    }

    @Operation(
            summary = "임시 비밀번호 발급",
            description = "등록된 이메일 주소로 임시 비밀번호를 전송합니다."
    )
    @PostMapping("password/temp")
    public ApiResponse<String> tempPassword(@RequestParam("email") @Valid String email) {
        userService.sendTempPassword(email);
        return ApiResponse.of(SuccessStatus._TEMP_PASSWORD_SENT, "등록된 이메일로 임시 비밀번호를 전송했습니다.");
    }

    @Operation(
            summary = "회원 탈퇴",
            description = "사용자 계정을 비활성화하고 탈퇴 처리합니다. 탈퇴 사유를 입력받습니다."
    )
    @PostMapping("/inactive")
    public ApiResponse<UserResponseDTO.withDrawalResultDTO> withdrawMe(@AuthenticationPrincipal CustomUserDetails userDetails
     ,@RequestBody UserRequestDTO.DeleteReasonDTO deleteReasonDTO
    ) {
        Long userId = usersUtils.getAuthenticatedUserId(userDetails);
        Users user = userService.withdrawUser(userId,deleteReasonDTO);
        return ApiResponse.onSuccess(UserConverter.toWithDrawalResultDTO(user));
    }
//    @Operation(summary = "🧪 테스트: 즉시 완전 삭제", description = "10일 경과 INACTIVE 사용자 즉시 삭제")
//    @PostMapping("/test/delete-inactive")
//    public ApiResponse<String> testDeleteInactive(@AuthenticationPrincipal CustomUserDetails userDetails) {
//        Long userId = usersUtils.getAuthenticatedUserId(userDetails);
//        userService.testImmediateDelete(userId);
//        return ApiResponse.onSuccess("즉시 삭제 완료", "10일 경과 INACTIVE 사용자 CASCADE 삭제됨");
//    }

    @Operation(
            summary = "소셜 프로필 완성",
            description = "OAuth 로그인 후 TEMP(소셜로그인) 상태 사용자의 프로필을 완성합니다.<br/>"
                    + "닉네임, 성별, 직업, 목적, 관심사를 입력받아 status를 ACTIVE로 변경하고 초기 폴더를 생성합니다.<br/>"
                    + "**Authorization 헤더에 OAUTH에서 받은 JWT 토큰 필요 (status=TEMP 사용자만 가능)**"
    )
    @PatchMapping("/social/complete")
    public ApiResponse<UserResponseDTO.JoinResultDTO> completeSocialProfile(
            @RequestBody @Valid UserRequestDTO.SocialCompleteDTO request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        if (userDetails == null) {
            throw new GeneralException(ErrorStatus._UNAUTHORIZED);
        }

        String email = userDetails.getUsername();

        Users user = userService.socialCompleteProfile(email, request);
        return ApiResponse.onSuccess(UserConverter.toJoinResultDTO(user));
    }

    // 전체 약관 한번에 동의
    @Operation(
            summary = "약관 동의",
            description = "TERMS_OF_USE, PRIVACY_POLICY, MARKETING를 enum으로 입력받습니다."
    )
    @PostMapping("/terms/agree")
    public ApiResponse<UserResponseDTO.TermsStatusDTO> termsAgreeBatch(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody @Valid UserRequestDTO.TermsAgreeDTO request
    ){
        UserResponseDTO.TermsStatusDTO result = termsAgreementService.termsAgreeBatch(request,userDetails);
        return ApiResponse.onSuccess(result);
    }
    @PatchMapping("/terms/agree")
    @Operation(summary = "약관 개별 변경")
    public ApiResponse<UserResponseDTO.TermsStatusDTO> updateTermsAgree(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody @Valid UserRequestDTO.SingleTermUpdateDTO request) {
        return ApiResponse.onSuccess(
                termsAgreementService.updateTermsAgree(userDetails, request.getTermsType(), request.getIsAgreed())
        );
    }

    @GetMapping("/terms/status")
    @Operation(summary = "약관 상태 조회")
    public ApiResponse<UserResponseDTO.TermsStatusDTO> getTermsStatus(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ApiResponse.onSuccess(termsAgreementService.getTermsStatus(userDetails));
    }

}
