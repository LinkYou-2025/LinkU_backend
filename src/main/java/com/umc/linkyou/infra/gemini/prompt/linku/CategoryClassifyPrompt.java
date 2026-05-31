package com.umc.linkyou.infra.gemini.prompt.linku;

public record CategoryClassifyPrompt(String domain, String title, String content, String categoryList) {

    public String render() {
        StringBuilder info = new StringBuilder();
        if (domain != null && !domain.isBlank())   info.append("도메인: ").append(domain).append("\n");
        if (title != null && !title.isBlank())     info.append("제목: ").append(title).append("\n");
        if (content != null && !content.isBlank()) info.append("본문(일부): ").append(content).append("\n");

        return String.format("""
                다음은 특정 URL에서 가져온 정보입니다.

                %s
                위 정보를 기반으로 아래 카테고리 목록 중 하나를 선택하고,
                해당 웹페이지의 핵심 키워드 3~5개를 해시태그 형식(#)으로 작성하세요.

                카테고리 목록:
                %s

                출력 예시:
                {
                  "categoryId": 2,
                  "keywords": "#키워드1, #키워드2, #키워드3"
                }
                """,
                info, categoryList);
    }
}
