package com.umc.linkyou.web.controller.user;

import com.umc.linkyou.apiPayload.ApiResponse;
import com.umc.linkyou.config.security.jwt.CustomUserDetails;
import com.umc.linkyou.converter.UserConverter;
import com.umc.linkyou.domain.Users;
import com.umc.linkyou.service.users.TermsAgreementService;
import com.umc.linkyou.service.users.UserService;
import com.umc.linkyou.service.users.UserWithdrawService;
import com.umc.linkyou.utils.UsersUtils;
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

    // 전체 약관 한번에 동의
    @Override
    public ApiResponse<UserResponseDTO.TermsStatusDTO> termsAgreeBatch(CustomUserDetails userDetails, UserRequestDTO.TermsAgreeDTO request) {
        return ApiResponse.onSuccess(termsAgreementService.termsAgreeBatch(request, userDetails));
    }

    // 개별 약관 변경
    @Override
    public ApiResponse<UserResponseDTO.TermsStatusDTO> updateTermsAgree(CustomUserDetails userDetails, UserRequestDTO.SingleTermUpdateDTO request) {
        return ApiResponse.onSuccess(termsAgreementService.updateTermsAgree(userDetails, request.getTermsType(), request.getIsAgreed()));
    }

    //약관 상태 조회
    @Override
    public ApiResponse<UserResponseDTO.TermsStatusDTO> getTermsStatus(CustomUserDetails userDetails) {
        return ApiResponse.onSuccess(termsAgreementService.getTermsStatus(userDetails));
    }

}
