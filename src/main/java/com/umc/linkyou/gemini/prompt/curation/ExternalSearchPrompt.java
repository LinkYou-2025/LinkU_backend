package com.umc.linkyou.gemini.prompt.curation;

import com.umc.linkyou.gemini.prompt.common.PromptTemplate;

public class ExternalSearchPrompt implements PromptTemplate {
    private final String jobName;
    private final String gender;
    private final int limit;
    private final String excludedDomains;
    private final String recentUrls;
    private final String tagNames;

    public ExternalSearchPrompt(String jobName, String gender, int limit, String excludedDomains, String recentUrls, String tagNames) {
        this.jobName = jobName;
        this.gender = gender;
        this.limit = limit;
        this.excludedDomains = excludedDomains;
        this.recentUrls = recentUrls;
        this.tagNames = tagNames;
    }

    @Override
    public String render() {
        return String.format("""
        # AUDIENCE PROFILE
        - Job: %s
        - Gender: %s
        - Locale: Korea (KR), Language: Korean

        # TARGETING & QUALITY RULES
        1. 사용자의 직무(%s)와 관련된 실무 기술, 도구, 커리어 성장에 최적화된 콘텐츠를 찾으세요.
        2. 최신성: 최근 24개월 이내의 콘텐츠를 우선하세요.
        3. 신뢰성: 한국의 공신력 있는 기술 블로그, 뉴스 기사 등을 선호하며 광고/스팸/로그인 필수 페이지는 제외하세요.
        4. 제외 도메인: [%s] (이미 본 도메인이므로 절대 추천 금지)

        # TASK
        - 최근 본 링크: %s
        - 중요 태그: %s
        
        위 태그와 직무에 직결되는 실제 존재하는 공개 웹페이지를 정확히 %d개 추천하세요.
        반드시 [{"title":"...","url":"..."}] 형식의 JSON 배열만 출력하세요. 설명은 생략합니다.
        """, jobName, gender, jobName, excludedDomains, recentUrls, tagNames, limit);
    }
}
