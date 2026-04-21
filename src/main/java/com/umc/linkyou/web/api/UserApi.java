package com.umc.linkyou.web.api;

import com.umc.linkyou.apiPayload.ApiResponse;
import com.umc.linkyou.config.security.jwt.CustomUserDetails;
import com.umc.linkyou.web.dto.UserRequestDTO;
import com.umc.linkyou.web.dto.UserResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "user-controller", description = "사용자 마이페이지 및 설정 관련 API")
@RequestMapping("/users")
public interface UserApi {

    @Operation(summary = "마이페이지 조회", description = "로그인한 사용자의 프로필 정보 및 활동 요약을 조회합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "프로필 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음 (USERS404)")
    })
    @GetMapping("/me")
    ApiResponse<UserResponseDTO.UserProfileSummaryDto> getUserInfo(@AuthenticationPrincipal CustomUserDetails userDetails);

    @Operation(summary = "마이페이지 수정", description = "닉네임, 성별, 직업, 관심사 등 프로필 정보를 변경합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "프로필 수정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이미 존재하는 닉네임 (USERS403)")
    })
    @PatchMapping("/profile")
    ApiResponse<String> updateUserProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody @Valid UserRequestDTO.UpdateProfileDTO updateDTO);

    @Operation(summary = "회원 탈퇴 (비활성화)", description = "계정을 INACTIVE 상태로 변경하고 탈퇴 사유를 저장합니다. 데이터는 30일 후 완전 삭제됩니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "탈퇴 처리 성공")
    })
    @PostMapping("/inactive")
    ApiResponse<UserResponseDTO.withDrawalResultDTO> withdrawMe(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody @Valid UserRequestDTO.DeleteReasonDTO deleteReasonDTO);

    @Operation(summary = "소셜 프로필 완성", description = "소셜 로그인 직후 TEMP 상태인 유저의 필수 정보를 입력받아 가입을 완료합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "가입 완료 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "이미 ACTIVE 상태인 유저 (USERS4001)")
    })
    @PatchMapping("/social/complete")
    ApiResponse<UserResponseDTO.JoinResultDTO> completeSocialProfile(
            @RequestBody @Valid UserRequestDTO.SocialCompleteDTO request,
            @AuthenticationPrincipal CustomUserDetails userDetails);

    @Operation(summary = "약관 동의 (일괄)", description = "서비스 이용을 위한 필수/선택 약관에 대해 일괄적으로 동의를 진행합니다.")
    @PostMapping("/terms/agree")
    ApiResponse<UserResponseDTO.TermsStatusDTO> termsAgreeBatch(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody @Valid UserRequestDTO.TermsAgreeDTO request);

    @Operation(summary = "약관 개별 변경")
    @PatchMapping("/terms/agree")
    ApiResponse<UserResponseDTO.TermsStatusDTO> updateTermsAgree(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody @Valid UserRequestDTO.SingleTermUpdateDTO request);

    @Operation(summary = "약관 상태 조회", description = "현재 사용자가 어떤 약관에 동의했는지 목록을 조회합니다.")
    @GetMapping("/terms/status")
    ApiResponse<UserResponseDTO.TermsStatusDTO> getTermsStatus(
            @AuthenticationPrincipal CustomUserDetails userDetails);
}
