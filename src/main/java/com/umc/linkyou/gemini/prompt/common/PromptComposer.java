package com.umc.linkyou.gemini.prompt.common;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PromptComposer {

    private final SystemPrompt systemPrompt;

    public String compose(PromptTemplate domainPrompt) {
        return String.format("""
            %s

            # Task Instruction
            %s
            
            # Result (JSON only):
            """, systemPrompt.render(), domainPrompt.render());
    }
}
