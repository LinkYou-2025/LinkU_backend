package com.umc.linkyou.web.controller.admin;

import com.umc.linkyou.apiPayload.ApiResponse;
import com.umc.linkyou.jwt.CustomUserDetails;
import com.umc.linkyou.converter.UserConverter;
import com.umc.linkyou.domain.Users;
import com.umc.linkyou.service.users.UserWithdrawService;
import com.umc.linkyou.validation.annotation.ApiAdmin;
import com.umc.linkyou.web.dto.UserResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.umc.linkyou.jwt.CurrentUser;
import org.springframework.web.bind.annotation.*;

@Tag(name = "user-controller", description = "사용자 관련 API")
@Slf4j
@ApiAdmin
@RestController("UserAdminController")
@RequiredArgsConstructor
@RequestMapping("/users")
public class UserController {

    private final UserWithdrawService userWithdrawService;


    @Operation(
            summary = "🧪 [관리자 전용] 회원탈퇴 바로하기 (즉시 완전 삭제)",
            description = "유예 기간(14일) 없이 요청 계정을 즉시 완전 삭제합니다. 삭제 후 복구가 불가능합니다.\n"
                    + "**admin 권한 계정으로만 호출할 수 있습니다.** 반드시 테스트 계정으로 확인 후 사용해 주세요.")
    @PostMapping("/test/delete-inactive")
    public ApiResponse<UserResponseDTO.withDrawalResultDTO> testDeleteInactive(@CurrentUser CustomUserDetails userDetails) {
        Long userId = userDetails.getUserId();
        Users user = userWithdrawService.testImmediateDelete(userId);

        return ApiResponse.onSuccess(UserConverter.toWithDrawalResultDTO(user));
    }


}
