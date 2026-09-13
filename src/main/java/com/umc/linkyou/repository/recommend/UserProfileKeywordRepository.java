package com.umc.linkyou.repository.recommend;

import com.umc.linkyou.domain.recommend.UserProfileKeyword;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface UserProfileKeywordRepository extends JpaRepository<UserProfileKeyword, Long> {

    // 재계산할 때마다 그 유저의 이전 프로필 키워드를 통째로 지우고 새로 top-K를 채운다.
    @Transactional
    @Modifying(clearAutomatically = true)
    void deleteAllByUserId(Long userId);

    // uq_user_profile_keyword(user_id, keyword_id) 충돌 시 weight 갱신.
    @Transactional
    @Modifying(clearAutomatically = true)
    @Query(value = """
            INSERT INTO user_profile_keywords (user_id, keyword_id, weight)
            VALUES (:userId, :keywordId, :weight)
            ON CONFLICT (user_id, keyword_id)
            DO UPDATE SET weight = :weight
            """, nativeQuery = true)
    void upsertWeight(
            @Param("userId") Long userId,
            @Param("keywordId") Long keywordId,
            @Param("weight") int weight);
}
