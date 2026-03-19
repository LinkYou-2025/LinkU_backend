package com.umc.linkyou.oauth2.mobile.controller;

import com.umc.linkyou.service.users.UserWithdrawService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

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

    // application.properties 또는 application.yml에 등록된 kakao.admin-key 값을 주입받습니다.
    @Value("${kakao.admin-key}")
    private String adminKey;

    @PostMapping("/unlink")
    public ResponseEntity<Void> kakaoUnlink(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody Map<String, Object> payload) {


        // 1. 보안 검증 (Header의 KakaoAK {ADMIN_KEY} 확인)
        // 카카오 서버는 "Authorization: KakaoAK ${ADMIN_KEY}" 형식으로 헤더를 보냅니다.
        if (authHeader == null || !authHeader.equals("KakaoAK " + adminKey)) {
            //권한 없는 카카오 웹훅 요청 거부
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // 2. 카카오 유저 ID(user_id) 추출
        // payload 내부의 "user_id" 값은 카카오 회원번호(Long 타입 등)입니다.
        if (payload == null || !payload.containsKey("user_id")) {
            //웹훅 페이로드에 user_id가 누락되었습니다.
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        String kakaoExternalId = payload.get("user_id").toString();
        //카카오 연결 해제 웹훅 수신 확정 - 카카오 고유번호(externalId)

        try {
            // 3. 탈퇴 및 연동 해제 비즈니스 로직 실행
            userWithdrawService.handleKakaoUnlinkWebhook(kakaoExternalId);
            //유저(externalId: {}) 웹훅 처리 성공
            return ResponseEntity.ok().build();

        } catch (Exception e) {
            //웹훅 처리 중 내부 오류 발생:
            // 카카오는 5xx 에러를 받으면 재시도를 시도할 수 있으므로 상황에 따라 적절히 응답합니다.
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
