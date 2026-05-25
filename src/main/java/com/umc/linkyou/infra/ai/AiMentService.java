package com.umc.linkyou.infra.ai;

import com.umc.linkyou.infra.ai.dto.MentResultDTO;

public interface AiMentService {
    // 큐레이션 상단 및 하단 멘트 생성
    MentResultDTO generateMent(String emotionName);
}
