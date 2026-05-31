package com.umc.linkyou.repository.curationRepository;

import com.umc.linkyou.domain.classification.CurationMent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CurationMentRepository extends JpaRepository<CurationMent, Long> {

    @Query("""
            SELECT cm FROM CurationMent cm
            WHERE cm.emotion.emotionId = :emotionId
            """)
    List<CurationMent> findAllByEmotionId(@Param("emotionId") Long emotionId);
}
