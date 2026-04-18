package com.umc.linkyou.service.common;

import com.umc.linkyou.domain.enums.KeywordType;
import com.umc.linkyou.repository.mapping.SituationJobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class KeywordNameResolver {

    private final EmotionTagMapper emotionTagMapper;
    private final SituationJobRepository situationJobRepository;

    public String resolve(KeywordType type, Long refId) {
        if (type == KeywordType.EMOTION) {
            return emotionTagMapper.getEmotionName(refId);
        }
        return situationJobRepository.findById(refId)
                .map(sj -> sj.getSituation().getName())
                .orElse("");
    }
}

