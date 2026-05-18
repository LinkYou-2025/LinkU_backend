package com.umc.linkyou.infra.gemini.prompt.linku;


public class CategoryClassifyPrompt {

    private final String domain;
    private final String title;
    private final String content;
    private final String categoryList;

    public CategoryClassifyPrompt(String domain, String title, String content, String categoryList) {
        this.domain = domain;
        this.title = title;
        this.content = content;
        this.categoryList = categoryList;
    }

    public String render() {
        return String.format("""
                다음은 특정 URL에서 가져온 정보입니다.

                🌐 도메인: %s
                📝 제목: %s
                📄 본문(일부): %s

                위 정보를 기반으로 아래 카테고리 목록 중 하나를 선택하고,
                해당 웹페이지의 핵심 키워드 3~5개를 해시태그 형식(#)으로 작성하세요.
                본문보다 도메인, 제목이 연관 가능성이 높습니다.
                각 정보 중 null인 것은 참고하지 마십시오.

                카테고리 목록:
                %s

                ✅ JSON 형식 예시:
                {
                  "categoryId": 2,
                  "keywords": "#키워드1, #키워드2, #키워드3"
                }

                ⚠ JSON 외 다른 내용 없이 출력하세요.
                """,
                domain, title, content, categoryList);
    }
}
