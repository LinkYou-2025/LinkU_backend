package com.umc.linkyou.infra.gemini.prompt.linku;

public record CategoryClassifyPrompt(
        Long jobId,
        String domain,
        String title,
        String content,
        String categoryList,
        String situationList,
        String emotionList
) {
    public String render() {
        StringBuilder info = new StringBuilder();
        if (domain != null && !domain.isBlank()) info.append("도메인: ").append(domain).append("\n");
        if (title != null && !title.isBlank()) info.append("제목: ").append(title).append("\n");
        if (content != null && !content.isBlank()) info.append("본문(일부): ").append(content).append("\n");

        String situationRule = switch (jobId == null ? -1 : jobId.intValue()) {
            case 1 -> "job_id 1인 사용자는 situation 1~8만 선택";
            case 2 -> "job_id 2인 사용자는 situation 9~16만 선택";
            case 3 -> "job_id 3인 사용자는 situation 17~24만 선택";
            case 4 -> "job_id 4인 사용자는 situation 25~32만 선택";
            case 5 -> "job_id 5인 사용자는 situation 33~40만 선택";
            case 6 -> "job_id 6인 사용자는 situation 41~48만 선택";
            default -> "상황은 제공된 목록에서만 선택";
        };

        return String.format("""
                다음은 특정 URL에서 가져온 정보입니다.

                %s
                위 정보를 기반으로 아래 목록에서 각각 하나씩 선택하고,
                핵심 키워드 3~5개를 해시태그 형식(#)으로 작성하고,
                링크를 잘 표현하는 제목을 생성하세요.

                상황 선택 규칙:
                %s

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
                info, situationRule, categoryList, situationList, emotionList);
    }
}
