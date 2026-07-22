package com.umc.linkyou.web.controller.user;

import com.umc.linkyou.apiPayload.ApiResponse;
import com.umc.linkyou.apiPayload.code.status.user.UserSuccessStatus;
import com.umc.linkyou.jwt.CurrentUser;
import com.umc.linkyou.jwt.CustomUserDetails;
import com.umc.linkyou.converter.UserConverter;
import com.umc.linkyou.domain.Users;
import com.umc.linkyou.service.users.TermsAgreementService;
import com.umc.linkyou.service.users.UserService;
import com.umc.linkyou.service.users.UserWithdrawService;
import com.umc.linkyou.validation.annotation.ApiV1;
import com.umc.linkyou.web.api.UserApi;
import com.umc.linkyou.web.dto.UserRequestDTO;
import com.umc.linkyou.web.dto.UserResponseDTO;
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
    private final TermsAgreementService termsAgreementService;

    @Override
    public ApiResponse<UserResponseDTO.UserProfileSummaryDto> getUserInfo(@CurrentUser CustomUserDetails userDetails) {
        return ApiResponse.onSuccess(UserSuccessStatus.USER_INFO_OK, userService.userInfo(userDetails.getUserId(), userDetails.getProvider()));
    }

    @Override
    public ApiResponse<Object> updateUserProfile(@CurrentUser CustomUserDetails userDetails, UserRequestDTO.UpdateProfileDTO updateDTO) {
        userService.updateUserProfile(userDetails.getUserId(), updateDTO);
        return ApiResponse.onSuccess(UserSuccessStatus.USER_PROFILE_UPDATED);
    }

    @Override
    public ApiResponse<UserResponseDTO.withDrawalResultDTO> withdrawMe(@CurrentUser CustomUserDetails userDetails, UserRequestDTO.DeleteReasonDTO deleteReasonDTO) {
        Users user = userWithdrawService.withdrawUser(userDetails.getUserId(), deleteReasonDTO);
        return ApiResponse.onSuccess(UserSuccessStatus.USER_WITHDRAW_OK, UserConverter.toWithDrawalResultDTO(user));
    }

    @Override
    public ApiResponse<UserResponseDTO.JoinResultDTO> completeSocialProfile(UserRequestDTO.SocialCompleteDTO request, @CurrentUser CustomUserDetails userDetails) {
        UserResponseDTO.JoinResultDTO result =
                userService.socialCompleteProfile(userDetails.getUserId(), userDetails.getProvider(), request);
        return ApiResponse.onSuccess(UserSuccessStatus.SOCIAL_PROFILE_COMPLETED, result);
    }

    @Override
    public ApiResponse<UserResponseDTO.TermsStatusDTO> updateTermsAgree(
            @CurrentUser CustomUserDetails userDetails,
            UserRequestDTO.TermsAgreeDTO request) {

        return ApiResponse.onSuccess(termsAgreementService.updateTermsAgree(userDetails, request));
    }

    @Override
    public ApiResponse<UserResponseDTO.TermsStatusDTO> getTermsStatus(@CurrentUser CustomUserDetails userDetails) {
        return ApiResponse.onSuccess(termsAgreementService.getTermsStatus(userDetails));
    }

    @Override
    public ApiResponse<UserResponseDTO.withDrawalResultDTO> recoverMe(@CurrentUser CustomUserDetails userDetails) {
        Users user = userWithdrawService.recoverUser(userDetails.getUserId());
        return ApiResponse.onSuccess(UserSuccessStatus.USER_RECOVER_OK, UserConverter.toWithDrawalResultDTO(user));
    }

    @Override
    public ApiResponse<UserResponseDTO.withDrawalResultDTO> testDeleteInactive(@CurrentUser CustomUserDetails userDetails) {
        Users user = userWithdrawService.testImmediateDelete(userDetails.getUserId());
        return ApiResponse.onSuccess(UserSuccessStatus.USER_TEST_IMMEDIATE_DELETE_OK, UserConverter.toWithDrawalResultDTO(user));
    }

    @Override
    public ApiResponse<Object> toggleMarketing(@CurrentUser CustomUserDetails userDetails) {
        termsAgreementService.toggleMarketing(userDetails);
        return ApiResponse.onSuccess(UserSuccessStatus.USER_MARKETING_AGREE_OK);
    }

}
