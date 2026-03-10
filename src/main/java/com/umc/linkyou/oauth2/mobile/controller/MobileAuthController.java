package com.umc.linkyou.oauth2.mobile.controller;

import com.umc.linkyou.apiPayload.ApiResponse;
import com.umc.linkyou.oauth2.mobile.dto.MobileLoginRequest;
import com.umc.linkyou.oauth2.mobile.dto.MobileLoginResponse;
import com.umc.linkyou.oauth2.mobile.service.GoogleMobileAuthService;
import com.umc.linkyou.oauth2.mobile.service.KakaoMobileAuthService;  // ✅ 추가
import com.umc.linkyou.oauth2.mobile.service.NaverMobileAuthService;  // ✅ 추가
import com.umc.linkyou.validation.annotation.ApiV1;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Tag(name = "Mobile Auth", description = "모바일 소셜 로그인 API")
@ApiV1
@RestController
@RequiredArgsConstructor
@RequestMapping("/auth/mobile")
public class MobileAuthController {

    private final GoogleMobileAuthService googleService;
    private final KakaoMobileAuthService kakaoService;
    private final NaverMobileAuthService naverService;

    @Operation(summary = "구글 로그인 (앱)", description = "Android/iOS Google Sign-In SDK에서 받은 **ID Token** 전달")
    @PostMapping("/google")
    public ApiResponse<MobileLoginResponse> googleLogin(@Valid @RequestBody MobileLoginRequest request) {
        MobileLoginResponse result = googleService.login(request.token());
        return ApiResponse.onSuccess(result);
    }

    @Operation(summary = "카카오 로그인 (앱)", description = "카카오 SDK에서 받은 **accessToken** 전달")
    @PostMapping("/kakao")
    public ApiResponse<MobileLoginResponse> kakaoLogin(@Valid @RequestBody MobileLoginRequest request) {
        log.info("✅ 카카오 컨트롤러 HIT! 토큰: {}", request.token().substring(0, 20));
        MobileLoginResponse result = kakaoService.login(request.token());
        return ApiResponse.onSuccess(result);
    }

    @Operation(summary = "네이버 로그인 (앱)", description = "네이버 SDK에서 받은 **accessToken** 전달")
    @PostMapping("/naver")
    public ApiResponse<MobileLoginResponse> naverLogin(@Valid @RequestBody MobileLoginRequest request) {
        MobileLoginResponse result = naverService.login(request.token());
        return ApiResponse.onSuccess(result);
    }
}
