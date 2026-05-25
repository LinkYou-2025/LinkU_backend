package com.umc.linkyou.infra.gemini.prompt.curation;

import com.umc.linkyou.infra.ai.dto.ExternalSearchRequest;

public record ExternalSearchPrompt(ExternalSearchRequest request) {

    private static final int FETCH_BUFFER = 2;

    public String render() {
        return String.format("""
                # Audience Profile
                - Job: %s
                - Gender: %s
                - Locale: Korea (KR), language: Korean

                # User Tags
                %s

                # Requirements
                - Recommend exactly %d publicly accessible web pages closely related to the tags and job above.
                - Prefer actionable content (tutorial, checklist, guide, trend summary, case study).
                - Titles must clearly reflect the core topic — avoid clickbait or exaggerated expressions.

                # Output Format
                [{"title":"...","url":"..."}]
                """,
                safe(request.jobName()),
                safe(request.gender()),
                (request.tagNames() == null || request.tagNames().isEmpty()) ? "(none)" : String.join(", ", request.tagNames()),
                request.limit() + FETCH_BUFFER);
    }

    private static String safe(String s) {
        return (s == null || s.isBlank()) ? "General" : s;
    }
}
