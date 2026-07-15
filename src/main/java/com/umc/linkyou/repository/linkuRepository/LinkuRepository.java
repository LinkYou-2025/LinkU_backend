package com.umc.linkyou.repository.linkuRepository;

import com.umc.linkyou.domain.Linku;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LinkuRepository extends JpaRepository<Linku, Long>, LinkuRepositoryCustom {

    @Modifying
    @Query("UPDATE Linku l SET l.totalViewCount = l.totalViewCount + 1 WHERE l.linkuId = :linkuId")
    void incrementTotalViewCount(@Param("linkuId") Long linkuId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
        INSERT INTO linkus
            (linku_url, title, img_url, total_view_count, category_id, domain_id, emotion_id, situation_id, created_at, updated_at)
        VALUES
            (:url, :title, :imgUrl, 0, :categoryId, :domainId, :emotionId, :situationId, now(), now())
        ON CONFLICT (linku_url) DO NOTHING
        """, nativeQuery = true)
    int insertIgnore(@Param("url") String url,
                     @Param("title") String title,
                     @Param("imgUrl") String imgUrl,
                     @Param("categoryId") Long categoryId,
                     @Param("domainId") Long domainId,
                     @Param("emotionId") Long emotionId,
                     @Param("situationId") Long situationId);


}
