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
}
