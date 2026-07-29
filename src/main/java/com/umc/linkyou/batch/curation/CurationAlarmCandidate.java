package com.umc.linkyou.batch.curation;

/**
 * 큐레이션 알림 발송 배치의 reader 조회 결과
 * - 아직 CURATION_UPDATED 알림이 발송되지 않은 큐레이션을 대상으로 함
 */
public record CurationAlarmCandidate(
        Long curationId,
        Long userId,
        String nickname
) {
}
