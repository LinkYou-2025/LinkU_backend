package com.umc.linkyou.infra.gemini.prompt.curation;


import java.util.List;

public class ExternalSearchPrompt {

    private final List<String> tagNames;
    private final int limit;
    private final String jobName;
    private final String gender;

    public ExternalSearchPrompt(List<String> tagNames, int limit, String jobName, String gender) {
        this.tagNames = tagNames;
        this.limit = limit;
        this.jobName = jobName;
        this.gender = gender;
    }

    public String render() {
        return String.format("""
                # Audience Profile
                - Job: %s
                - Gender: %s
                - Locale: Korea (KR), language: Korean

                사용자 중요 태그: %s

                요구사항:
                - 위 태그와 직무에 직결되는 주제 위주로, 실제 존재하는 공개 웹페이지를 정확히 %d개 추천.
                - 실무 적용 가능성 높은 콘텐츠(튜토리얼/체크리스트/가이드/트렌드 요약/사례연구) 선호.
                - 제목은 과장/낚시성 표현을 피하고 핵심 주제를 명확히 드러내는 자료만.

                형식: [{"title":"...","url":"..."}]
                """,
                safe(jobName),
                safe(gender),
                (tagNames == null || tagNames.isEmpty()) ? "(없음)" : String.join(", ", tagNames),
                limit);
    }

    private String safe(String s) {
        return (s == null || s.isBlank()) ? "General" : s;
    }
}
