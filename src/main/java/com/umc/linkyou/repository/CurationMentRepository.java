package com.umc.linkyou.repository;

import com.umc.linkyou.domain.classification.CurationMent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CurationMentRepository extends JpaRepository<CurationMent, Long> {
    List<CurationMent> findAllByEmotion_EmotionId(Long emotionId);
}