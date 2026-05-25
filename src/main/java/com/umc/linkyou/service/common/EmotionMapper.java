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
public class EmotionMapper {

    private final EmotionRepository emotionRepository;
    private final Map<Long, String> emotionIdToName = new HashMap<>();
    private final Map<String, Long> emotionNameToId = new HashMap<>();

    @PostConstruct
    public void init() {
        for (Emotion emotion : emotionRepository.findAll()) {
            emotionIdToName.put(emotion.getEmotionId(), emotion.getName());
            emotionNameToId.put(emotion.getName(), emotion.getEmotionId());
        }
    }

    public String getEmotionName(Long emotionId) {
        return emotionIdToName.getOrDefault(emotionId, "");
    }

    public Long getEmotionId(String name) {
        return emotionNameToId.get(name);
    }
}