package com.umc.linkyou.repository.mapping;

import com.umc.linkyou.domain.Keyword;
import com.umc.linkyou.domain.Linku;
import com.umc.linkyou.domain.mapping.LinkuKeyword;
import com.umc.linkyou.repository.dto.KeywordCountRow;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface LinkuKeywordRepository extends JpaRepository<LinkuKeyword, Long> {
    boolean existsByLinkuAndKeyword(Linku linku, Keyword keyword);

    // 해당 월에 같은 직업 유저들이 저장한 링크의 키워드별 등장 횟수 상위 N건
    @Query("""
            SELECT new com.umc.linkyou.repository.dto.KeywordCountRow(k.name, COUNT(ul))
            FROM LinkuKeyword lk
            JOIN lk.keyword k
            JOIN lk.linku l
            JOIN l.usersLinku ul
            WHERE ul.user.job.id = :jobId
            AND ul.createdAt >= :start AND ul.createdAt < :end
            GROUP BY k.name
            ORDER BY COUNT(ul) DESC
            """)
    List<KeywordCountRow> findTopKeywordNamesByJobIdAndPeriod(
            @Param("jobId") Long jobId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            Pageable pageable);
}
