package com.umc.linkyou.batch.curation;

import com.umc.linkyou.domain.Users;

/**
 * 월간 큐레이션 생성 배치에서 writer로 넘기는 생성 명령 객체
 */
public record MonthlyCurationBatchItem(
        Users user,
        String month,
        String thumbnailUrl
) {
}
