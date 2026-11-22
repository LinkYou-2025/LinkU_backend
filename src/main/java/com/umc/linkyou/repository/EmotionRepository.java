package com.umc.linkyou.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.umc.linkyou.domain.classification.Emotion;

@Repository
public interface EmotionRepository extends JpaRepository<Emotion, Long> {
    Optional<Emotion> findById(Long emotionId);
}
