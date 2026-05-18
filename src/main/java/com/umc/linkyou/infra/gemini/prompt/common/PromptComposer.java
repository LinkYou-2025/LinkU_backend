package com.umc.linkyou.infra.gemini.prompt.common;

import org.springframework.stereotype.Component;

@Component
public class PromptComposer {

    private final GeneralSystemPrompt generalSystemPrompt;
    private final ExternalSearchSystemPrompt externalSearchSystemPrompt;
    private final MentSystemPrompt mentSystemPrompt;

    public PromptComposer(
            GeneralSystemPrompt generalSystemPrompt,
            ExternalSearchSystemPrompt externalSearchSystemPrompt,
            MentSystemPrompt mentSystemPrompt
    ) {
        this.generalSystemPrompt = generalSystemPrompt;
        this.externalSearchSystemPrompt = externalSearchSystemPrompt;
        this.mentSystemPrompt = mentSystemPrompt;
    }

    public String general() {
        return generalSystemPrompt.render();
    }

    public String externalSearch() {
        return externalSearchSystemPrompt.render();
    }

    public String mention() {
        return mentSystemPrompt.render();
    }
}
