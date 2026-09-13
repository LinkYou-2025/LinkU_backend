package com.umc.linkyou.repository.keywordRepository;

import com.umc.linkyou.domain.enums.KeywordType;
import com.umc.linkyou.domain.log.KeywordMonthlyCount;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface KeywordMonthlyCountRepository extends JpaRepository<KeywordMonthlyCount, Long> {

    // 운영 DB는 PostgreSQL이라 MySQL 전용 ON DUPLICATE KEY UPDATE 문법을 못 쓴다.
    // 테이블명도 실제 스키마(keyword_monthly_counts, V1__init.sql)와 맞춰 복수형으로 수정.
    // 충돌 기준 컬럼은 uq_keyword_monthly 유니크 제약(user_id, type, ref_id, base_month)과 동일.
    @Transactional
    @Modifying(clearAutomatically = true)
    @Query(value = """
            INSERT INTO keyword_monthly_counts (user_id, type, ref_id, base_month, count)
            VALUES (:userId, :type, :refId, :baseMonth, 1)
            ON CONFLICT (user_id, type, ref_id, base_month)
            DO UPDATE SET count = keyword_monthly_counts.count + 1
            """, nativeQuery = true)
    void upsertCount(
            @Param("userId") Long userId,
            @Param("type") String type,
            @Param("refId") Long refId,
            @Param("baseMonth") String baseMonth);

    // 해당 월 top 3 태그
    @Query("""
            SELECT k
            FROM KeywordMonthlyCount k
            WHERE k.user.id = :userId
            AND k.baseMonth = :baseMonth
            ORDER BY k.count DESC
            """)
    List<KeywordMonthlyCount> findTopByUserIdAndBaseMonth(
            @Param("userId") Long userId,
            @Param("baseMonth") String baseMonth,
            Pageable pageable);

    // 큐레이션 상세: 해당 월 top 감정 or 상황
    @Query("""
            SELECT k
            FROM KeywordMonthlyCount k
            WHERE k.user.id = :userId
            AND k.baseMonth = :baseMonth
            AND k.type = :type
            ORDER BY k.count DESC
            """)
    List<KeywordMonthlyCount> findTopByUserIdAndBaseMonthAndType(
            @Param("userId") Long userId,
            @Param("baseMonth") String baseMonth,
            @Param("type") KeywordType type,
            Pageable pageable);

}