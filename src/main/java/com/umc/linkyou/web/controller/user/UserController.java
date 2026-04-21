package com.umc.linkyou.web.controller.user;

import com.umc.linkyou.apiPayload.ApiResponse;
import com.umc.linkyou.apiPayload.code.status.SuccessStatus;
import com.umc.linkyou.config.security.jwt.CustomUserDetails;
import com.umc.linkyou.converter.UserConverter;
import com.umc.linkyou.domain.Users;
import com.umc.linkyou.service.users.TermsAgreementService;
import com.umc.linkyou.service.users.UserService;
import com.umc.linkyou.service.users.UserWithdrawService;
import com.umc.linkyou.utils.UsersUtils;
import com.umc.linkyou.validation.annotation.ApiV1;
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
    private final UserWithdrawService userWithdrawService;
    private final UsersUtils usersUtils;
    private final TermsAgreementService termsAgreementService;

    @Operation(
            summary = "마이페이지 조회",
            description = "사용자의 프로필 정보를 조회합니다."
    )
    @GetMapping("/me")
    public ApiResponse<UserResponseDTO.UserProfileSummaryDto> getUserInfo(@AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = usersUtils.getAuthenticatedUserId(userDetails);
        String loginProvider = userDetails.getProvider();
        return ApiResponse.onSuccess(userService.userInfo(userId,loginProvider));
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
            summary = "회원 탈퇴",
            description = "사용자 계정을 비활성화하고 탈퇴 처리합니다. 탈퇴 사유를 입력받습니다."
    )
    @PostMapping("/inactive")
    public ApiResponse<UserResponseDTO.withDrawalResultDTO> withdrawMe(@AuthenticationPrincipal CustomUserDetails userDetails
     ,@RequestBody UserRequestDTO.DeleteReasonDTO deleteReasonDTO
    ) {
        Long userId = usersUtils.getAuthenticatedUserId(userDetails);
        Users user = userWithdrawService.withdrawUser(userId,deleteReasonDTO);
        return ApiResponse.onSuccess(UserConverter.toWithDrawalResultDTO(user));
    }
    @Operation(summary = "🧪 테스트: 즉시 완전 삭제", description = "10일 경과 INACTIVE 사용자 즉시 삭제")
    @PostMapping("/test/delete-inactive")
    public ApiResponse<UserResponseDTO.withDrawalResultDTO> testDeleteInactive(@AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = usersUtils.getAuthenticatedUserId(userDetails);
        Users user = userWithdrawService.testImmediateDelete(userId);

        return ApiResponse.onSuccess(UserConverter.toWithDrawalResultDTO(user));
    }

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
        Users user = usersUtils.validateTempUser(userDetails);
        Users updatedUser = userService.socialCompleteProfile(user, request);

        return ApiResponse.onSuccess(UserConverter.toJoinResultDTO(updatedUser));
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
