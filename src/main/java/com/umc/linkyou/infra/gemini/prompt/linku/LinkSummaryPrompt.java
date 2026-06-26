package com.umc.linkyou.infra.gemini.prompt.linku;

public record LinkSummaryPrompt(String content) {

    public String render() {
        return String.format("""
                다음 웹페이지 본문을 기반으로 핵심 내용을 간결하게 요약해 주세요.

                본문:
                %s

                출력 예시:
                {
                  "summary": "..."
                }
                """,
                content);
    }
}
