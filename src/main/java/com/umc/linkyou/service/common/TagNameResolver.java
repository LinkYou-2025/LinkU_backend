package com.umc.linkyou.service.common;

import org.springframework.stereotype.Component;

import com.umc.linkyou.domain.enums.KeywordType;
import com.umc.linkyou.repository.classification.SituationRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class TagNameResolver {

    private final EmotionMapper emotionMapper;
    private final SituationRepository situationRepository;

    public String resolve(KeywordType type, Long refId) {
        if (type == KeywordType.EMOTION) {
            return emotionMapper.getEmotionName(refId);
        }
        return situationRepository.findNameById(refId).orElse("");
    }
}
