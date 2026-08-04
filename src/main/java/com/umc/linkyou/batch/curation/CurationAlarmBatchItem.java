package com.umc.linkyou.batch.curation;

/**
 * 큐레이션 알림 발송 배치에서 writer로 넘기는 발송 대상
 */
public record CurationAlarmBatchItem(
        Long userId,
        Long curationId,
        String nickname
) {
}
