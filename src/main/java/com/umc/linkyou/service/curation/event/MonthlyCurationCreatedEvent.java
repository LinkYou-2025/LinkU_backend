package com.umc.linkyou.service.curation.event;

/**
 * 월간 큐레이션 생성 완료 이벤트
 *
 * <p>큐레이션 저장과 top log 저장이 끝난 뒤, 커밋 이후 외부 추천 후처리를
 * 분리하기 위해 발행한다.
 */
public record MonthlyCurationCreatedEvent(
        Long curationId
) {
}
