package com.umc.linkyou.infra.gemini.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.umc.linkyou.infra.ai.AiSearchService;
import com.umc.linkyou.infra.ai.dto.ExternalLinkDTO;
import com.umc.linkyou.infra.gemini.prompt.common.PromptComposer;
import com.umc.linkyou.infra.gemini.prompt.curation.ExternalSearchPrompt;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GeminiExternalSearchService implements AiSearchService {

    private final GeminiService geminiService;
    private final PromptComposer promptComposer;

    @Override
    public List<ExternalLinkDTO> searchExternalLinks(
            List<String> tagNames,
            int limit,
            String jobName,
            String gender
    ) {
        ExternalSearchPrompt prompt = new ExternalSearchPrompt(tagNames, limit, jobName, gender);

        List<Map<String, String>> rawList = geminiService.callAndParseList(
                promptComposer.externalSearch(),
                prompt.render(),
                new TypeReference<List<Map<String, String>>>() {}
        );

        return rawList.stream()
                .filter(m -> m.get("url") != null && !m.get("url").isBlank())
                .limit(limit)
                .map(m -> ExternalLinkDTO.builder()
                        .title(m.getOrDefault("title", "No Title"))
                        .url(m.get("url"))
                        .build())
                .collect(Collectors.toList());
    }
}
