package com.umc.linkyou.service.common;

import com.umc.linkyou.domain.classification.Emotion;
import com.umc.linkyou.repository.EmotionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class EmotionTagMapper {

    private final EmotionRepository emotionRepository;

    private final Map<Long, String> emotionIdToName = new HashMap<>();

    @PostConstruct
    public void init() {
        for (Emotion emotion : emotionRepository.findAll()) {
            emotionIdToName.put(emotion.getEmotionId(), emotion.getName());
        }
    }

    public String getEmotionName(Long emotionId) {
        return emotionIdToName.getOrDefault(emotionId, "알 수 없음");
    }
}