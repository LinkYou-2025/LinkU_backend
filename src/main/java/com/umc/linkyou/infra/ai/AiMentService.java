package com.umc.linkyou.infra.ai;

// 큐레이션 상단 및 하단 멘트 생성
public interface AiMentService {
    record MentResult(String header, String footer) {}
    MentResult generateMent(String emotionName);
}
