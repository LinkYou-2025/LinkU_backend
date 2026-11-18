package com.umc.linkyou.infra.gemini.prompt.linku;

public record CategoryClassifyPrompt(
        String domain,
        String title,
        String content,
        String categoryList,
        String situationList,
        String emotionList) {

    public String render() {
        StringBuilder info = new StringBuilder();
        if (domain != null && !domain.isBlank()) info.append("도메인: ").append(domain).append("\n");
        if (title != null && !title.isBlank()) info.append("제목: ").append(title).append("\n");
        if (content != null && !content.isBlank())
            info.append("본문(일부): ").append(content).append("\n");
        return String.format(
                """
                다음은 특정 URL에서 가져온 정보입니다.

                %s
                위 정보를 기반으로 아래 작업을 수행하세요.

                1. 카테고리, 상황(Situation), 감정(Emotion) 목록에서 각각 하나씩 선택
                2. 핵심 키워드 3~5개를 해시태그 형식(#)으로 작성
                3. 링크를 잘 표현하는 제목 생성
                4. 페이지 종류(pageType)를 아래 중 하나로 판별
                   - CONTENT: 글, 기사, 문서 등 실제 콘텐츠
                   - LOGIN: 로그인이나 회원가입을 요구하는 화면
                   - ERROR: 없는 페이지, 삭제된 글, 오류 안내
                   - PAYWALL: 유료 구독, 성인 인증 등 접근 제한 안내
                   - OTHER: 쿠키 동의, 중간 안내, 빈 페이지 등 콘텐츠가 아닌 화면

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
                  "emotionId": 1,
                  "pageType": "CONTENT"
                }
                """,
                info, categoryList, situationList, emotionList);
    }
}
