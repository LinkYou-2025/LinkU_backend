package com.umc.linkyou.repository.curationRepository;

import com.umc.linkyou.domain.classification.CurationMent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CurationMentRepository extends JpaRepository<CurationMent, Long> {
    List<CurationMent> findAllByEmotion_EmotionId(Long emotionId);
}