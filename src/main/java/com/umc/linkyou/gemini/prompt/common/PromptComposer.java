package com.umc.linkyou.gemini.prompt.common;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PromptComposer {

    @Qualifier("generalSystemPrompt")
    private final PromptTemplate generalSystemPrompt;

    @Qualifier("curationSystemPrompt")
    private final PromptTemplate curationSystemPrompt;

    /**
     * 일반 분석(분류, 요약)용 조합
     */
    public String compose(PromptTemplate domainPrompt) {
        return build(generalSystemPrompt, domainPrompt);
    }

    /**
     * 큐레이션(외부 검색 추천)용 조합
     */
    public String composeCuration(PromptTemplate domainPrompt) {
        return build(curationSystemPrompt, domainPrompt);
    }

    private String build(PromptTemplate system, PromptTemplate domain) {
        return String.format("""
            %s

            # Task Instruction
            %s
            
            # Result (JSON only):
            """, system.render(), domain.render());
    }
}
