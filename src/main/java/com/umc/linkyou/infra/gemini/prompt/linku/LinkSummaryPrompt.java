package com.umc.linkyou.infra.gemini.prompt.linku;

public record LinkSummaryPrompt(String content, String situationList, String emotionList) {

    public String render() {
        return String.format("""
                다음 웹페이지 본문을 기반으로 다음 항목을 모두 생성해 주세요.

                본문:
                %s

                제공된 목록에서 하나씩 선택하여 ID만 사용해 주세요.

                상황 (Situation):
                %s

                감정 (Emotion):
                %s

                출력 예시:
                {
                  "title": "...",
                  "summary": "...",
                  "situationId": 7,
                  "emotionId": 5,
                  "keywords": "#키워드1, #키워드2, ..."
                }
                """,
                content, situationList, emotionList);
    }
}
