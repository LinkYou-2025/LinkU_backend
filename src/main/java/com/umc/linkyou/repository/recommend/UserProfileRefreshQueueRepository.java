package com.umc.linkyou.repository.recommend;

import com.umc.linkyou.domain.recommend.UserProfileRefreshQueue;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

public interface UserProfileRefreshQueueRepository extends JpaRepository<UserProfileRefreshQueue, Long> {

    // user_id 가 PK라 이미 큐에 있으면 requested_at만 갱신(중복 upsert는 사실상 무시).
    // failure_count도 0으로 리셋한다 — 재요청은 이전 실패 이력과 무관한 새 요청이므로, 실패가
    // 쌓여 포기 직전이던 유저도 다시 저장하면 재시도 기회를 새로 얻는다.
    // clearAutomatically는 일부러 안 씀 — enqueue()는 링크 저장 트랜잭션 중간에 호출되는데,
    // 이 옵션을 켜면 그 시점에 persistence context 전체가 detach되어 이후 지연 로딩 접근에서
    // LazyInitializationException이 날 수 있다.
    @Transactional
    @Modifying
    @Query(value = """
            INSERT INTO user_profile_refresh_queue (user_id, requested_at, failure_count)
            VALUES (:userId, now(), 0)
            ON CONFLICT (user_id) DO UPDATE SET requested_at = now(), failure_count = 0
            """, nativeQuery = true)
    void enqueue(@Param("userId") Long userId);

    // 오래 기다린 유저부터 chunk 단위로 드레인.
    List<UserProfileRefreshQueue> findAllByOrderByRequestedAtAsc(Pageable pageable);

    // 처리 시작 시점에 읽은 requestedAt과 일치할 때만 삭제한다. 처리 도중 같은 유저가 다시
    // enqueue되면(requested_at 갱신) 여기 조건이 안 맞아 0건 삭제되고, 그 요청은 큐에 남아
    // 다음 드레인에서 다시 처리된다 — deleteById(userId)로 무조건 지우면 그 재요청이 유실된다.
    @Transactional
    int deleteByUserIdAndRequestedAt(Long userId, LocalDateTime requestedAt);

    // 처리 실패 시 재시도 카운터 +1. requestedAt이 일치할 때만 올린다 — 처리 도중 같은 유저가
    // 다시 enqueue돼 requestedAt/failure_count가 이미 리셋됐다면, 그 새 요청에 옛 실패 이력을
    // 잘못 얹지 않기 위해서다(deleteByUserIdAndRequestedAt과 동일한 보호 패턴).
    @Transactional
    @Modifying
    @Query("""
            UPDATE UserProfileRefreshQueue q
            SET q.failureCount = q.failureCount + 1
            WHERE q.userId = :userId AND q.requestedAt = :requestedAt
            """)
    int incrementFailureCount(@Param("userId") Long userId, @Param("requestedAt") LocalDateTime requestedAt);
}
