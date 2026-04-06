package com.umc.linkyou.repository.LogRepository;

import com.umc.linkyou.domain.enums.KeywordType;
import com.umc.linkyou.domain.log.KeywordMonthlyCount;
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
    void upsertCount(@Param("userId") Long userId,
                     @Param("type") String type,
                     @Param("refId") Long refId,
                     @Param("baseMonth") String baseMonth);

    List<KeywordMonthlyCount> findAllByUser_IdAndBaseMonth(Long userId, String baseMonth);

    // 큐레이션 상세: 해당 월 top 3 태그
    List<KeywordMonthlyCount> findTop3ByUser_IdAndBaseMonthOrderByCountDesc(Long userId, String baseMonth);

    // 큐레이션 상세: 해당 월 top 감정
    KeywordMonthlyCount findTopByUser_IdAndBaseMonthAndTypeOrderByCountDesc(Long userId, String baseMonth, KeywordType type);

    // 내부 추천: 해당 월 감정 목록
    List<KeywordMonthlyCount> findAllByUser_IdAndBaseMonthAndType(Long userId, String baseMonth, KeywordType type);

    // 외부 추천: 유저 전체 top 3 키워드
    List<KeywordMonthlyCount> findTop3ByUser_IdOrderByCountDesc(Long userId);
}
