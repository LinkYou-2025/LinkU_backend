package com.umc.linkyou.repository;

import com.umc.linkyou.domain.RecentViewedLinku;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface RecentViewedLinkuRepository extends JpaRepository<RecentViewedLinku, Long> {

    // 링크 삭제 시 관련된 열람 로그를 모두 지워주는 메서드 (그대로 유지)
    void deleteByUser_IdAndLinku_LinkuId(Long userId, Long linkuId);

    // [신규] 홈 화면용: 유저가 본 링크 ID를 중복 없이 가장 최근 열람 시간(MAX) 순으로 정렬하여 페이징 조회
    @Query("SELECT r.linku.linkuId FROM RecentViewedLinku r WHERE r.user.id = :userId GROUP BY r.linku.linkuId ORDER BY MAX(r.viewedAt) DESC")
    List<Long> findDistinctLinkuIdsByUserId(@Param("userId") Long userId, Pageable pageable);

    // [신규] 스케줄러용: 특정 기준 시간(cutoff) 이전의 오래된 로그 일괄 삭제
    @Modifying
    @Query("DELETE FROM RecentViewedLinku r WHERE r.viewedAt < :cutoff")
    int deleteByViewedAtBefore(@Param("cutoff") LocalDateTime cutoff);

}
