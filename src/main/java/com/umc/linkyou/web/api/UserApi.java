package com.umc.linkyou.web.api;

import com.umc.linkyou.apiPayload.ApiResponse;
import com.umc.linkyou.apiPayload.code.status.ErrorStatus;
import com.umc.linkyou.apiPayload.code.status.SuccessStatus;
import com.umc.linkyou.apiPayload.code.status.user.UserErrorStatus;
import com.umc.linkyou.config.security.jwt.CustomUserDetails;
import com.umc.linkyou.validation.annotation.swagger.ApiErrorCode;
import com.umc.linkyou.validation.annotation.swagger.ApiSuccessCode;
import com.umc.linkyou.web.dto.UserRequestDTO;
import com.umc.linkyou.web.dto.UserResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "user-controller", description = "사용자 마이페이지 및 설정 관련 API")
@RequestMapping("/users")
public interface UserApi {

    @Operation(
            summary = "마이페이지 조회",
            description = """
                    로그인한 사용자의 프로필 정보 및 활동 요약을 조회합니다.
                    - 사용자의 이메일, 닉네임, 직업, 사용 목적, 관심사 목록을 반환합니다.
                    - 현재 로그인한 소셜 제공자(GENERAL, KAKAO 등) 정보를 포함합니다.
                    """
    )
    @ApiSuccessCode(SuccessStatus._OK)
    @ApiErrorCode(userErrorStatus = {UserErrorStatus._USER_NOT_FOUND})
    @GetMapping("/me")
    ApiResponse<UserResponseDTO.UserProfileSummaryDto> getUserInfo(
            @AuthenticationPrincipal CustomUserDetails userDetails);

    @Operation(
            summary = "마이페이지 수정",
            description = """
                    사용자의 프로필 정보를 변경합니다.
                    - 닉네임 변경 시 중복 여부를 체크합니다.
                    - 직업(Job ID), 관심사 리스트, 사용 목적 리스트를 전체 업데이트합니다.
                    """
    )
    @ApiSuccessCode(SuccessStatus._OK)
    @ApiErrorCode(errorStatus = {ErrorStatus._BAD_REQUEST}) // 잘못된 Job ID 등
    @ApiErrorCode(userErrorStatus = {UserErrorStatus._USER_NOT_FOUND, UserErrorStatus._DUPLICATE_NICKNAME})
    @PatchMapping("/profile")
    ApiResponse<String> updateUserProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody @Valid UserRequestDTO.UpdateProfileDTO updateDTO);

    @Operation(
            summary = "회원 탈퇴 (비활성화)",
            description = """
                    사용자 계정을 비활성화(INACTIVE) 처리합니다.
                    - 탈퇴 사유를 입력받아 저장합니다.
                    - 실제 데이터 삭제는 정책에 따라 유예 기간(예: 30일) 후에 진행됩니다.
                    """
    )
    @ApiSuccessCode(SuccessStatus._OK)
    @ApiErrorCode(userErrorStatus = {UserErrorStatus._USER_NOT_FOUND})
    @PostMapping("/inactive")
    ApiResponse<UserResponseDTO.withDrawalResultDTO> withdrawMe(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody @Valid UserRequestDTO.DeleteReasonDTO deleteReasonDTO);

    @Operation(
            summary = "소셜 프로필 완성",
            description = """
                    소셜 로그인 직후 TEMP 상태인 사용자의 필수 추가 정보를 입력받습니다.
                    - 닉네임, 성별, 직업, 목적, 관심사를 입력받아 계정을 ACTIVE 상태로 전환합니다.
                    - 최초 1회만 호출 가능하며, 이미 ACTIVE인 경우 에러를 반환합니다.
                    """
    )
    @ApiSuccessCode(SuccessStatus._OK)
    @ApiErrorCode(errorStatus = {ErrorStatus._ALREADY_ACTIVE_USER, ErrorStatus._BAD_REQUEST})
    @ApiErrorCode(userErrorStatus = {UserErrorStatus._DUPLICATE_NICKNAME})
    @PatchMapping("/social/complete")
    ApiResponse<UserResponseDTO.JoinResultDTO> completeSocialProfile(
            @RequestBody @Valid UserRequestDTO.SocialCompleteDTO request,
            @AuthenticationPrincipal CustomUserDetails userDetails);

    @Operation(
            summary = "약관 동의 (일괄)",
            description = """
                    서비스 이용에 필요한 여러 약관에 대해 한꺼번에 동의를 진행합니다.
                    - 필수 약관 미동의 시 서비스 이용이 제한될 수 있습니다.
                    """
    )
    @ApiSuccessCode(SuccessStatus._OK)
    @ApiErrorCode(userErrorStatus = {UserErrorStatus._USER_NOT_FOUND, UserErrorStatus.INVALID_TERMS_TYPE})
    @PostMapping("/terms/agree")
    ApiResponse<UserResponseDTO.TermsStatusDTO> termsAgreeBatch(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody @Valid UserRequestDTO.TermsAgreeDTO request);

    @Operation(
            summary = "약관 개별 변경",
            description = "특정 약관(예: 마케팅 수신 동의)에 대한 동의 여부를 개별적으로 수정합니다."
    )
    @ApiSuccessCode(SuccessStatus._OK)
    @ApiErrorCode(userErrorStatus = {UserErrorStatus._USER_NOT_FOUND, UserErrorStatus.INVALID_TERMS_TYPE})
    @PatchMapping("/terms/agree")
    ApiResponse<UserResponseDTO.TermsStatusDTO> updateTermsAgree(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody @Valid UserRequestDTO.SingleTermUpdateDTO request);

    @Operation(
            summary = "약관 상태 조회",
            description = "사용자가 현재 어떤 약관에 동의했는지 전체 목록과 상태를 조회합니다."
    )
    @ApiSuccessCode(SuccessStatus._OK)
    @ApiErrorCode(userErrorStatus = {UserErrorStatus._USER_NOT_FOUND})
    @GetMapping("/terms/status")
    ApiResponse<UserResponseDTO.TermsStatusDTO> getTermsStatus(
            @AuthenticationPrincipal CustomUserDetails userDetails);

    @Operation(
            summary = "회원탈퇴 복구 (계정 활성화)",
            description = """
                탈퇴 유예 기간(14일) 내에 있는 사용자의 계정을 다시 활성화합니다.
                - 상태를 ACTIVE로 변경하고 탈퇴 사유 및 날짜를 초기화합니다.
                - 탈퇴 유예 기간 내 사용자의 계정을 다시 ACTIVE 상태로 복구합니다."""
    )
    @ApiSuccessCode(SuccessStatus._OK)
    @ApiErrorCode(userErrorStatus = {UserErrorStatus._USER_NOT_FOUND})
    @PostMapping("/recover")
    ApiResponse<UserResponseDTO.withDrawalResultDTO> recoverMe(
            @AuthenticationPrincipal CustomUserDetails userDetails);
}
