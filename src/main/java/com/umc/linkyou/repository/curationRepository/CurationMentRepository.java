package com.umc.linkyou.repository.curationRepository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.umc.linkyou.domain.classification.CurationMent;

public interface CurationMentRepository extends JpaRepository<CurationMent, Long> {

    @Query(
            """
            SELECT cm FROM CurationMent cm
            WHERE cm.emotion.emotionId = :emotionId
            """)
    List<CurationMent> findAllByEmotionId(@Param("emotionId") Long emotionId);
}
