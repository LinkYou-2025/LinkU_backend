package com.umc.linkyou.infra.gemini.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.umc.linkyou.infra.ai.AiSearchService;
import com.umc.linkyou.infra.ai.dto.ExternalLinkResultDTO;
import com.umc.linkyou.infra.ai.dto.ExternalSearchRequest;
import com.umc.linkyou.infra.gemini.prompt.common.PromptComposer;
import com.umc.linkyou.infra.gemini.prompt.curation.ExternalSearchPrompt;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GeminiExternalSearchService implements AiSearchService {

    private final GeminiService geminiService;
    private final PromptComposer promptComposer;

    @Override
    public List<ExternalLinkResultDTO> searchExternalLinks(ExternalSearchRequest request) {
        List<ExternalLinkResultDTO> rawList = geminiService.callAndParseWithSearch(
                promptComposer.externalSearch(),
                new ExternalSearchPrompt(request).render(),
                new TypeReference<List<ExternalLinkResultDTO>>() {}
        );

        return rawList.stream()
                .filter(l -> l.getUrl() != null && !l.getUrl().isBlank())
                .map(l -> l.getTitle() != null ? l
                        : ExternalLinkResultDTO.builder().title("No Title").url(l.getUrl()).build())
                .limit(request.limit())
                .toList();
    }
}