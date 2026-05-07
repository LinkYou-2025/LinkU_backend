package com.umc.linkyou.web.controller.user;

import com.umc.linkyou.apiPayload.ApiResponse;
import com.umc.linkyou.apiPayload.code.status.SuccessStatus;
import com.umc.linkyou.apiPayload.code.status.user.UserErrorStatus;
import com.umc.linkyou.config.security.jwt.CustomUserDetails;
import com.umc.linkyou.converter.UserConverter;
import com.umc.linkyou.domain.Users;
import com.umc.linkyou.service.users.TermsAgreementService;
import com.umc.linkyou.service.users.UserService;
import com.umc.linkyou.service.users.UserWithdrawService;
import com.umc.linkyou.utils.UsersUtils;
import com.umc.linkyou.validation.annotation.ApiV1;
import com.umc.linkyou.validation.annotation.swagger.ApiErrorCode;
import com.umc.linkyou.validation.annotation.swagger.ApiSuccessCode;
import com.umc.linkyou.web.api.UserApi;
import com.umc.linkyou.web.dto.UserRequestDTO;
import com.umc.linkyou.web.dto.UserResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Tag(name = "user-controller", description = "사용자 관련 API")
@Slf4j
@ApiV1
@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
public class UserController implements UserApi {

    private final UserService userService;
    private final UserWithdrawService userWithdrawService;
    private final UsersUtils usersUtils;
    private final TermsAgreementService termsAgreementService;

    // 마이페이지 정보 가져오기
    @Override
    public ApiResponse<UserResponseDTO.UserProfileSummaryDto> getUserInfo(CustomUserDetails userDetails) {
        Long userId = usersUtils.getAuthenticatedUserId(userDetails);
        return ApiResponse.onSuccess(userService.userInfo(userId, userDetails.getProvider()));
    }

    // 마이페이지 수정 api
    @Override
    public ApiResponse<String> updateUserProfile(CustomUserDetails userDetails, UserRequestDTO.UpdateProfileDTO updateDTO) {
        Long userId = usersUtils.getAuthenticatedUserId(userDetails);
        userService.updateUserProfile(userId, updateDTO);
        return ApiResponse.onSuccess("마이페이지가 수정되었습니다.");
    }

    // 회원 탈퇴 api
    @Override
    public ApiResponse<UserResponseDTO.withDrawalResultDTO> withdrawMe(CustomUserDetails userDetails, UserRequestDTO.DeleteReasonDTO deleteReasonDTO) {
        Long userId = usersUtils.getAuthenticatedUserId(userDetails);
        Users user = userWithdrawService.withdrawUser(userId, deleteReasonDTO);
        return ApiResponse.onSuccess(UserConverter.toWithDrawalResultDTO(user));
    }

    // 소셜로그인 완료 api
    @Override
    public ApiResponse<UserResponseDTO.JoinResultDTO> completeSocialProfile(UserRequestDTO.SocialCompleteDTO request, CustomUserDetails userDetails) {
        Users user = usersUtils.validateTempUser(userDetails);
        Users updatedUser = userService.socialCompleteProfile(user, request);
        return ApiResponse.onSuccess(UserConverter.toJoinResultDTO(updatedUser));
    }


    // 약관 일괄 변경/업데이트
    @Override
    public ApiResponse<UserResponseDTO.TermsStatusDTO> updateTermsAgree(
            CustomUserDetails userDetails,
            UserRequestDTO.TermsAgreeDTO request) {

        // 개별 파라미터가 아닌 DTO 전체를 서비스로 전달
        return ApiResponse.onSuccess(termsAgreementService.updateTermsAgree(userDetails, request));
    }

    //약관 상태 조회
    @Override
    public ApiResponse<UserResponseDTO.TermsStatusDTO> getTermsStatus(CustomUserDetails userDetails) {
        return ApiResponse.onSuccess(termsAgreementService.getTermsStatus(userDetails));
    }

    //회원탈퇴 복구 api
    @Override
    public ApiResponse<UserResponseDTO.withDrawalResultDTO> recoverMe(CustomUserDetails userDetails) {
        Long userId = usersUtils.getAuthenticatedUserId(userDetails);
        Users user = userWithdrawService.recoverUser(userId);
        return ApiResponse.onSuccess(UserConverter.toWithDrawalResultDTO(user));
    }
}
