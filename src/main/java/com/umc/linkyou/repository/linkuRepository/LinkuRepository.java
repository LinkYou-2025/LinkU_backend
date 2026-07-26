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

    // 크롤링 이미지(imgUrl)는 외부 URL이므로 images 테이블에 source_type=EXTERNAL로 먼저 적재한 뒤,
    // 그 image_id를 linkus.image_id로 연결한다 (imgUrl이 null이면 new_image CTE가 0행이라 image_id도 NULL).
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
        WITH new_image AS (
            INSERT INTO images (source_type, location, created_at, updated_at)
            SELECT 'EXTERNAL', :imgUrl, now(), now()
            WHERE :imgUrl IS NOT NULL
            RETURNING id
        )
        INSERT INTO linkus
            (linku_url, title, image_id, total_view_count, category_id, domain_id, emotion_id, situation_id, created_at, updated_at)
        VALUES
            (:url, :title, (SELECT id FROM new_image), 0, :categoryId, :domainId, :emotionId, :situationId, now(), now())
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
