package com.umc.linkyou.domain.recommend;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 홈화면 추천 콘텐츠 프로필(user_content_profiles / user_profile_keywords) 재계산이 필요한
 * 유저를 표시해두는 dirty queue. 링크 저장/키워드 태깅 완료 시점에 upsert 되고,
 * {@code UserProfileRefreshWorker}가 chunk 단위로 드레인한 뒤 삭제한다.
 *
 * 전체 유저를 스캔해서 "누가 바뀌었는지" 추론하지 않기 위한 테이블 — service/common/README.md 참고.
 *
 * {@code failureCount}는 재시도 제한용 카운터다. {@code UserProfileRefreshWorker}가 처리에 실패할
 * 때마다 1씩 늘리고, 임계값에 도달하면 큐에서 포기(삭제)한다 — 안 그러면 계속 실패하는 유저 하나가
 * requestedAt 오름차순 정렬 특성상 매 드레인 주기마다 청크 앞쪽을 차지해 정상 유저 처리를 밀어낼 수
 * 있다. enqueue()가 다시 호출되면(새 요청) 0으로 리셋된다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Entity
@Table(name = "user_profile_refresh_queue")
public class UserProfileRefreshQueue {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;

    @Column(name = "failure_count", nullable = false)
    @Builder.Default
    private int failureCount = 0;
}
