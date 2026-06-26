package com.umc.linkyou.infra.gemini.prompt.linku;

public record CategoryClassifyPrompt(String domain, String title, String content, String categoryList, String situationList, String emotionList) {

    public String render() {
        StringBuilder info = new StringBuilder();
        if (domain != null && !domain.isBlank())   info.append("도메인: ").append(domain).append("\n");
        if (title != null && !title.isBlank())     info.append("제목: ").append(title).append("\n");
        if (content != null && !content.isBlank()) info.append("본문(일부): ").append(content).append("\n");
        // 반드시 분류 후보군에서 situation과 emotion을 선택해야함.
        return String.format("""
                다음은 특정 URL에서 가져온 정보입니다.

                %s
                위 정보를 기반으로 아래 목록에서 각각 하나씩 선택하고,
                핵심 키워드 3~5개를 해시태그 형식(#)으로 작성하고,
                링크를 잘 표현하는 제목을 생성하세요.

                카테고리 목록:
                %s

                상황(Situation) 목록:
                %s

                감정(Emotion) 목록:
                %s

                출력 예시:
                {
                  "categoryId": 2,
                  "keywords": "#키워드1, #키워드2, #키워드3",
                  "title": "...",
                  "situationId": 3,
                  "emotionId": 1
                }
                """,
                info, categoryList, situationList, emotionList);
    }
}
