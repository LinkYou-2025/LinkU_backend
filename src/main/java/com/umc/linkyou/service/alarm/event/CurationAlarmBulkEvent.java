package com.umc.linkyou.service.alarm.event;

import com.umc.linkyou.web.dto.alarm.FcmBulkTarget;

import java.util.List;

/**
 * 큐레이션 알림 청크(최대 100건) 단위 일괄 발송 이벤트
 * - 유저별 개인화 본문(nickname)을 유지한 채 FCM 호출 한 번으로 묶어 보낸다
 */
public record CurationAlarmBulkEvent(List<FcmBulkTarget> targets) {
}
