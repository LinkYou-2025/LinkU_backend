package com.umc.linkyou.utils;

import org.springframework.data.util.Pair;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class EmotionSimilarityUtil {

    private static final Map<Pair<Long, Long>, Integer> EMOTION_SIMILARITY_MAP;

    static {
        Map<Pair<Long, Long>, Integer> map = new HashMap<>();

        // 정확 일치 (60점)
        for (long id = 1L; id <= 6L; id++) {
            map.put(Pair.of(id, id), 60);
        }

        // 유사 감정 (40점)
        map.put(Pair.of(1L, 3L), 40); // 즐거움 → 설렘
        map.put(Pair.of(3L, 1L), 40); // 설렘 → 즐거움
        map.put(Pair.of(4L, 2L), 40); // 슬픔 → 평온
        map.put(Pair.of(5L, 6L), 40); // 짜증 → 분노
        map.put(Pair.of(6L, 5L), 40); // 분노 → 짜증
        map.put(Pair.of(2L, 4L), 40); // 평온 → 슬픔
        map.put(Pair.of(2L, 1L), 40); // 평온 → 즐거움

        // 약한 연관 (20점)
        map.put(Pair.of(1L, 2L), 20); // 즐거움 → 평온
        map.put(Pair.of(3L, 2L), 20); // 설렘 → 평온
        map.put(Pair.of(4L, 5L), 20); // 슬픔 → 짜증
        map.put(Pair.of(5L, 4L), 20); // 짜증 → 슬픔
        map.put(Pair.of(6L, 2L), 20); // 분노 → 평온
        map.put(Pair.of(2L, 3L), 20); // 평온 → 설렘
        map.put(Pair.of(2L, 5L), 20); // 평온 → 짜증
        map.put(Pair.of(2L, 6L), 20); // 평온 → 분노

        EMOTION_SIMILARITY_MAP = Collections.unmodifiableMap(map);
    }

    /**
     * userEmotionId, selectedEmotionId 간 감정 유사도 점수 반환
     * 없으면 0 반환
     */
    public static int getSimilarityScore(Long userEmotionId, Long selectedEmotionId) {
        if (userEmotionId == null || selectedEmotionId == null) {
            return 0;
        }
        return EMOTION_SIMILARITY_MAP.getOrDefault(Pair.of(userEmotionId, selectedEmotionId), 0);
    }
}
