package com.umc.linkyou.repository.recommend;

import com.umc.linkyou.domain.recommend.UserContentProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface UserContentProfileRepository extends JpaRepository<UserContentProfile, Long> {

    // user_id 가 PK라 upsert 충돌 기준은 user_id 하나.
    @Transactional
    @Modifying(clearAutomatically = true)
    @Query(value = """
            INSERT INTO user_content_profiles (user_id, profile_tsquery_text, profile_text, updated_at)
            VALUES (:userId, :profileTsqueryText, :profileText, now())
            ON CONFLICT (user_id)
            DO UPDATE SET profile_tsquery_text = :profileTsqueryText,
                          profile_text = :profileText,
                          updated_at = now()
            """, nativeQuery = true)
    void upsertProfile(
            @Param("userId") Long userId,
            @Param("profileTsqueryText") String profileTsqueryText,
            @Param("profileText") String profileText);
}
