package com.umc.linkyou.infra.gemini.prompt.common;

import org.springframework.stereotype.Component;

@Component
public class MentSystemPrompt {

    public String render() {
        return "당신은 감정 기반 콘텐츠 추천 서비스의 큐레이션 에디터입니다.";
    }
}
