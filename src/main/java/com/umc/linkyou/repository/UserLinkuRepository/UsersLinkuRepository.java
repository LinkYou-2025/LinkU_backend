package com.umc.linkyou.repository.UserLinkuRepository;

import com.umc.linkyou.domain.mapping.UsersLinku;
import com.umc.linkyou.repository.dto.TagCountRow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UsersLinkuRepository  extends JpaRepository<UsersLinku, Long>, UsersLinkuRepositoryCustom {
    Optional<UsersLinku> findByUserIdAndLinku_LinkuUrl(Long userId, String url);

    List<UsersLinku> findByUser_IdAndLinku_LinkuId(Long userId, Long linkuId);

    List<UsersLinku> findByUser_Id(Long userId);

    @Query("""
            SELECT ul FROM UsersLinku ul
            JOIN FETCH ul.emotion
            JOIN FETCH ul.linku l
            LEFT JOIN FETCH l.aiArticle
            WHERE ul.user.id = :userId
            AND ul.createdAt >= :start AND ul.createdAt < :end
            """)
    List<UsersLinku> findAllByUserIdAndCreatedAtBetween(
            @Param("userId") Long userId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Modifying
    @Query("UPDATE UsersLinku ul SET ul.viewCount = ul.viewCount + 1, ul.lastViewedAt = :viewedAt WHERE ul.userLinkuId = :id")
    void incrementViewCount(Long id, LocalDateTime viewedAt);

    List<UsersLinku> findTop10ByUser_IdAndLastViewedAtIsNotNullOrderByLastViewedAtDesc(Long userId);

    List<UsersLinku> findByUser_IdAndLastViewedAtIsNull(Long userId);

    // 해당 기간 유저가 저장한 링크의 감정별 저장 횟수
    @Query("""
            SELECT new com.umc.linkyou.repository.dto.TagCountRow(ul.emotion.emotionId, COUNT(ul))
            FROM UsersLinku ul
            WHERE ul.user.id = :userId
            AND ul.emotion IS NOT NULL
            AND ul.createdAt >= :start AND ul.createdAt < :end
            GROUP BY ul.emotion.emotionId
            """)
    List<TagCountRow> countByEmotionForUserAndPeriod(
            @Param("userId") Long userId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    // 해당 기간 유저가 저장한 링크의 상황별 저장 횟수
    @Query("""
            SELECT new com.umc.linkyou.repository.dto.TagCountRow(ul.situation.id, COUNT(ul))
            FROM UsersLinku ul
            WHERE ul.user.id = :userId
            AND ul.situation IS NOT NULL
            AND ul.createdAt >= :start AND ul.createdAt < :end
            GROUP BY ul.situation.id
            """)
    List<TagCountRow> countBySituationForUserAndPeriod(
            @Param("userId") Long userId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Query("""
            SELECT ul FROM UsersLinku ul
            LEFT JOIN FETCH ul.emotion
            JOIN FETCH ul.linku l
            LEFT JOIN FETCH l.domain
            WHERE ul.user.id = :userId
            AND ul.lastViewedAt IS NULL
            AND ul.createdAt >= :start AND ul.createdAt < :end
            """)
    List<UsersLinku> findUnviewedByUserIdAndCreatedAtBetween(
            @Param("userId") Long userId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

}