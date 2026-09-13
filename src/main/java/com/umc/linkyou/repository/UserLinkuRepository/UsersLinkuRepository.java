package com.umc.linkyou.repository.UserLinkuRepository;

import com.umc.linkyou.domain.mapping.UsersLinku;
import com.umc.linkyou.repository.dto.TagCountRow;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface UsersLinkuRepository  extends JpaRepository<UsersLinku, Long>, UsersLinkuRepositoryCustom {
    // 동일 유저가 같은 링크를 중복 저장했을 수 있어 단일 결과(Optional)로는 2건 이상일 때 예외가 난다.
    // 존재 여부 확인은 "1개 이상 있으면 존재"로 처리해야 하므로 목록으로 조회한다.
    List<UsersLinku> findByUserIdAndLinku_LinkuUrlOrderByCreatedAtDesc(Long userId, String url);

    List<UsersLinku> findByUser_IdAndLinku_LinkuId(Long userId, Long linkuId);

    List<UsersLinku> findByUser_Id(Long userId);

    long countByUser_Id(Long userId);

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

    // 홈 화면 "최근 열람" 목록용: 열람 기록이 있으면 lastViewedAt, 없으면(생성만 하고 아직 안 본 링크) createdAt을 사용
    @Query("""
            SELECT ul FROM UsersLinku ul
            JOIN FETCH ul.linku l
            LEFT JOIN FETCH l.domain
            WHERE ul.user.id = :userId
            ORDER BY COALESCE(ul.lastViewedAt, ul.createdAt) DESC
            """)
    List<UsersLinku> findRecentByUserId(@Param("userId") Long userId, Pageable pageable);

    List<UsersLinku> findByUser_IdAndLastViewedAtIsNull(Long userId);

    // 후보 URL 중 사용자가 이미 저장한 것을 찾는다 (외부 추천에 userLinkuId를 채워주기 위함)
    @Query("""
            SELECT ul FROM UsersLinku ul
            JOIN FETCH ul.linku l
            WHERE ul.user.id = :userId AND l.linkuUrl IN :urls
            """)
    List<UsersLinku> findByUserIdAndLinkuUrlIn(@Param("userId") Long userId, @Param("urls") List<String> urls);

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
