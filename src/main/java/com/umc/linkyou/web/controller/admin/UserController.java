package com.umc.linkyou.web.controller.admin;

import com.umc.linkyou.apiPayload.ApiResponse;
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

    private final UserWithdrawService userWithdrawService;
    private final UsersUtils usersUtils;


    @Operation(summary = "🧪 테스트: 즉시 완전 삭제", description = "10일 경과 INACTIVE 사용자 즉시 삭제")
    @PostMapping("/test/delete-inactive")
    public ApiResponse<UserResponseDTO.withDrawalResultDTO> testDeleteInactive(@AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = usersUtils.getAuthenticatedUserId(userDetails);
        Users user = userWithdrawService.testImmediateDelete(userId);

        return ApiResponse.onSuccess(UserConverter.toWithDrawalResultDTO(user));
    }


}
