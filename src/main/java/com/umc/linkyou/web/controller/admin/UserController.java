package com.umc.linkyou.web.controller.admin;

import com.umc.linkyou.apiPayload.ApiResponse;
import com.umc.linkyou.jwt.CustomUserDetails;
import com.umc.linkyou.converter.UserConverter;
import com.umc.linkyou.domain.Users;
import com.umc.linkyou.service.users.UserWithdrawService;
import com.umc.linkyou.validation.annotation.ApiV1;
import com.umc.linkyou.web.dto.UserResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.umc.linkyou.jwt.CurrentUser;
import org.springframework.web.bind.annotation.*;

@Tag(name = "user-controller", description = "사용자 관련 API")
@Slf4j
@ApiV1
@RestController("UserAdminController")
@RequiredArgsConstructor
@RequestMapping("/users")
public class UserController {

    private final UserWithdrawService userWithdrawService;


    @Operation(summary = "🧪 테스트: 즉시 완전 삭제", description = "10일 경과 INACTIVE 사용자 즉시 삭제")
    @PostMapping("/test/delete-inactive")
    public ApiResponse<UserResponseDTO.withDrawalResultDTO> testDeleteInactive(@CurrentUser CustomUserDetails userDetails) {
        Long userId = userDetails.getUserId();
        Users user = userWithdrawService.testImmediateDelete(userId);

        return ApiResponse.onSuccess(UserConverter.toWithDrawalResultDTO(user));
    }


}
