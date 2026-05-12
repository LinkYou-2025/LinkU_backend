package com.umc.linkyou.gemini.prompt.linku;

import com.umc.linkyou.gemini.prompt.common.PromptTemplate;

public class LinkSummaryPrompt implements PromptTemplate {
    private final String content;
    private final String categoryList;

    public LinkSummaryPrompt(String content, String categoryList) {
        this.content = content;
        this.categoryList = categoryList;
    }

    @Override
    public String render() {
        return String.format("""
            제공된 웹페이지 본문을 객관적으로 분석하여 아래 항목을 생성하세요.

            📄 본문:
            %s

            # 지시 사항:
            1. 콘텐츠의 내용을 가장 잘 나타내는 제목(title)을 작성하세요.
            2. 내용을 3줄 내외로 요약(summary)하세요.
            3. 아래 카테고리 목록 중 가장 적합한 하나를 골라 ID(categoryId)를 반환하세요.
            4. 핵심 키워드 3~5개를 해시태그 형식(#)으로 작성하세요.

            [카테고리 목록]
            %s

            # 응답 형식 (JSON):
            {
              "title": "객관적인 제목",
              "summary": "내용 요약",
              "categoryId": 2,
              "keywords": "#키워드1, #키워드2"
            }
            """, content, categoryList);
    }
}
