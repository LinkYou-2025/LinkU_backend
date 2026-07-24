package com.umc.linkyou.repository.recommend;

import com.umc.linkyou.domain.recommend.UserProfileRefreshQueue;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface UserProfileRefreshQueueRepository extends JpaRepository<UserProfileRefreshQueue, Long> {

    // user_id 가 PK라 이미 큐에 있으면 requested_at만 갱신(중복 upsert는 사실상 무시).
    @Transactional
    @Modifying(clearAutomatically = true)
    @Query(value = """
            INSERT INTO user_profile_refresh_queue (user_id, requested_at)
            VALUES (:userId, now())
            ON CONFLICT (user_id) DO UPDATE SET requested_at = now()
            """, nativeQuery = true)
    void enqueue(@Param("userId") Long userId);

    // 오래 기다린 유저부터 chunk 단위로 드레인.
    List<UserProfileRefreshQueue> findAllByOrderByRequestedAtAsc(Pageable pageable);
}
