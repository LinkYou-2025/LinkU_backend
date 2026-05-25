package com.umc.linkyou.repository.keywordRepository;

import com.umc.linkyou.domain.enums.KeywordType;
import com.umc.linkyou.domain.log.KeywordMonthlyCount;
import com.umc.linkyou.service.keyword.KeywordStatRow;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface KeywordMonthlyCountRepository extends JpaRepository<KeywordMonthlyCount, Long> {

    @Transactional
    @Modifying(clearAutomatically = true)
    @Query(value = """
            INSERT INTO keyword_monthly_count (user_id, type, ref_id, base_month, count)
            VALUES (:userId, :type, :refId, :baseMonth, 1)
            ON DUPLICATE KEY UPDATE count = count + 1
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

    // 큐레이션 상세: 해당 월 top 감정
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

    // 같은 직업 유저들의 키워드 합산 상위 N건
    @Query("""
            SELECT new com.umc.linkyou.service.keyword.KeywordStatRow(k.type, k.refId, SUM(k.count))
            FROM KeywordMonthlyCount k
            WHERE k.user.job.id = :jobId
            GROUP BY k.type, k.refId
            ORDER BY SUM(k.count) DESC
            """)
    List<KeywordStatRow> findTopKeywordByJobId(
            @Param("jobId") Long jobId,
            Pageable pageable);
}