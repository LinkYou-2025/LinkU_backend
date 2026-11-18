package com.umc.linkyou.infra.gemini.prompt.linku;

public record LinkSummaryPrompt(String content) {

    public String render() {
        return String.format(
                """
                다음 웹페이지 본문을 기반으로 아래 작업을 수행하세요.

                1. 핵심 내용을 간결하게 요약
                2. 페이지 종류(pageType)를 아래 중 하나로 판별
                   - CONTENT: 글, 기사, 문서 등 실제 콘텐츠
                   - LOGIN: 로그인이나 회원가입을 요구하는 화면
                   - ERROR: 없는 페이지, 삭제된 글, 오류 안내
                   - PAYWALL: 유료 구독, 성인 인증 등 접근 제한 안내
                   - OTHER: 쿠키 동의, 중간 안내, 빈 페이지 등 콘텐츠가 아닌 화면

                본문:
                %s

                출력 예시:
                {
                  "summary": "...",
                  "pageType": "CONTENT"
                }
                """,
                content);
    }
}
