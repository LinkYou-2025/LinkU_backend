package com.umc.linkyou.oauth2.mobile.controller;

import com.umc.linkyou.apiPayload.ApiResponse;
import com.umc.linkyou.apiPayload.code.status.ErrorStatus;
import com.umc.linkyou.service.users.UserWithdrawService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

/**
 * 카카오 연결 해제 웹훅 처리를 위한 컨트롤러
 * 사용자가 카카오톡 설정에서 연결 해제를 할 경우 카카오 서버가 이 엔드포인트로 POST 요청을 보냅니다.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/webhooks/kakao")
@RequiredArgsConstructor
public class KakaoWebhookController {

    private final UserWithdrawService userWithdrawService;

    @Value("${kakao.admin-key}")
    private String adminKey;

    @PostMapping("/unlink")
    public ApiResponse<String> kakaoUnlink(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody Map<String, Object> payload) {

        // 1. 보안 검증 (Header의 KakaoAK {ADMIN_KEY} 확인)
        if (authHeader == null || !authHeader.equals("KakaoAK " + adminKey)) {
            log.warn("권한 없는 카카오 웹훅 요청 거부");
            // ApiResponse 형식을 유지하며 에러 응답
            return ApiResponse.onFailure(
                    ErrorStatus._UNAUTHORIZED.getCode(),
                    ErrorStatus._UNAUTHORIZED.getMessage(),
                    null);
        }

        // 2. 카카오 유저 ID(user_id) 추출 및 검증
        String kakaoExternalId = Optional.ofNullable(payload)
                .map(p -> p.get("user_id"))
                .map(Object::toString)
                .filter(id -> !id.isBlank())
                .orElse(null);

        if (kakaoExternalId == null) {
            log.error("웹훅 페이로드에 유효한 user_id가 누락되었습니다.");
            return ApiResponse.onFailure(
                    ErrorStatus._BAD_REQUEST.getCode(),
                    "웹훅 페이로드에 유효한 user_id가 누락되었습니다.",
                    null);
        }

        log.info("카카오 연결 해제 웹훅 수신 확정 - 카카오 고유번호(externalId): {}", kakaoExternalId);

        try {
            // 3. 탈퇴 및 연동 해제 비즈니스 로직 실행
            userWithdrawService.handleKakaoUnlinkWebhook(kakaoExternalId);
            return ApiResponse.onSuccess("카카오 연결 해제 처리가 완료되었습니다.");

        } catch (Exception e) {
            log.error("웹훅 처리 중 내부 오류 발생: {}", e.getMessage());
            return ApiResponse.onFailure(
                    ErrorStatus._INTERNAL_SERVER_ERROR.getCode(),
                    ErrorStatus._INTERNAL_SERVER_ERROR.getMessage(),
                    e.getMessage());
        }
    }
}
