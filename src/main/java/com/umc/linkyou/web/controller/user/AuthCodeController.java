package com.umc.linkyou.web.controller.user;

import com.umc.linkyou.apiPayload.ApiResponse;
import com.umc.linkyou.service.users.AuthCodeService;
import com.umc.linkyou.validation.annotation.ApiV1;
import com.umc.linkyou.web.dto.UserResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name="auth-controller", description = "소셜로그인시 임시코드와 accessToken, refreshToken을 교환하는 api")
@ApiV1
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthCodeController {
    private final AuthCodeService authCodeService;

    @Operation(
            summary = "1회용 code를 request param으로 넣으면 accessToken + refreshToken으로 교환합니다. 30초 이내 1회만 사용 가능합니다."
    )
    @PostMapping("/token/exchange")
    public ApiResponse<UserResponseDTO.TokenPair> exchangeCode(@RequestParam String code){
        return ApiResponse.onSuccess(authCodeService.exchangeCode(code));
    }
}
