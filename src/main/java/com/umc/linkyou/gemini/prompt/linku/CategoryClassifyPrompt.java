package com.umc.linkyou.gemini.prompt.linku;

import com.umc.linkyou.gemini.prompt.common.PromptTemplate;

public class CategoryClassifyPrompt implements PromptTemplate {
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

    @Override
    public String render() {
        return String.format("""
            URL 정보를 기반으로 카테고리 ID를 선택하고 핵심 키워드 3~5개를 추출하세요.
            - 도메인: %s
            - 제목: %s
            - 본문 일부: %s
            
            [카테고리 목록]
            %s
            
            출력 형식: {"categoryId": 1, "keywords": "#해시태그"}
            """, domain, title, content, categoryList);
    }
}
