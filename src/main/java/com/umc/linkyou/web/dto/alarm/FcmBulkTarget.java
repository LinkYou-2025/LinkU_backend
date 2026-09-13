package com.umc.linkyou.web.dto.alarm;

/**
 * 개인화된 알림을 여러 유저에게 한 번의 FCM 배치 호출로 보내기 위한 발송 대상
 */
public record FcmBulkTarget(
        Long userId,
        FcmSendRequestDTO requestDTO
) {
}
