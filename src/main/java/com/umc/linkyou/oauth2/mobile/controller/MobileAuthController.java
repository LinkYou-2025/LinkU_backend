package com.umc.linkyou.oauth2.mobile.controller;

import com.umc.linkyou.apiPayload.ApiResponse;
import com.umc.linkyou.oauth2.mobile.dto.MobileLoginRequest;
import com.umc.linkyou.oauth2.mobile.dto.MobileLoginResponse;
import com.umc.linkyou.oauth2.mobile.service.GoogleMobileAuthService;
import com.umc.linkyou.validation.annotation.ApiV1;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Mobile Auth", description = "모바일 소셜 로그인 API")
@ApiV1  // 기존 컨트롤러처럼!
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth/mobile")
public class MobileAuthController {
    private final GoogleMobileAuthService googleService;

    @Operation(summary = "구글 로그인 (앱)", description = "Android/iOS Google Sign-In SDK에서 받은 ID Token 전달")
    @PostMapping("/google")
    public ApiResponse<MobileLoginResponse> googleLogin(@Valid @RequestBody MobileLoginRequest request) {
        MobileLoginResponse result = googleService.login(request.token());
        return ApiResponse.onSuccess(result);  // ✅ 표준 형식!
    }

}
