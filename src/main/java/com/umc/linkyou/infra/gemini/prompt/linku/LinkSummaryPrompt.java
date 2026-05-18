package com.umc.linkyou.infra.gemini.prompt.linku;


public class LinkSummaryPrompt {

    private final String content;
    private final String situationList;
    private final String emotionList;
    private final String categoryList;

    public LinkSummaryPrompt(String content, String situationList, String emotionList, String categoryList) {
        this.content = content;
        this.situationList = situationList;
        this.emotionList = emotionList;
        this.categoryList = categoryList;
    }

    public String render() {
        return String.format("""
                다음 웹페이지 본문을 기반으로 다음 항목을 모두 생성해 주세요.

                📄 본문:
                %s

                🔸 제공된 목록에서 하나씩 선택하여 ID만 사용해 주세요.

                상황 (Situation):
                %s

                감정 (Emotion):
                %s

                카테고리 (Category):
                %s

                ✅ 응답 형식(JSON):
                {
                  "title": "...",
                  "summary": "...",
                  "situationId": 7,
                  "emotionId": 5,
                  "categoryId": 2,
                  "keywords": "#키워드1, #키워드2, ..."
                }

                ⚠ JSON 외 텍스트 없이 출력하세요. 설명이나 해설 금지.
                ⚠ 모든 응답은 한국어로 자연스럽게 작성해 주세요.
                """,
                content, situationList, emotionList, categoryList);
    }
}
