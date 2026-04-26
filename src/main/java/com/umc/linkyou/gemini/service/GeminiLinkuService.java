package com.umc.linkyou.gemini.service;

import com.umc.linkyou.gemini.dto.GeminiAnalysisResult;
import com.umc.linkyou.gemini.prompt.common.PromptComposer;
import com.umc.linkyou.gemini.prompt.linku.CategoryClassifyPrompt;
import com.umc.linkyou.infra.parser.TitleDomainParser;
import com.umc.linkyou.infra.parser.WebContentExtractor;
import com.umc.linkyou.repository.classification.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GeminiLinkuService {

    private final GeminiService geminiService;
    private final PromptComposer promptComposer;
    private final WebContentExtractor webContentExtractor;
    private final TitleDomainParser titleDomainParser;
    private final CategoryRepository categoryRepository;

    public GeminiAnalysisResult classify(String url) {
        // 1. 도구(Infra)를 사용하여 데이터 수집
        TitleDomainParser.ParsedPageInfo pageInfo = titleDomainParser.parseUrl(url);
        String content = webContentExtractor.extractTextFromUrl(url);

        // 2. 카테고리 목록 준비
        String categoryList = categoryRepository.findAll().stream()
                .map(c -> "- id: " + c.getCategoryId() + ", name: " + c.getCategoryName())
                .collect(Collectors.joining("\n"));

        // 3. 프롬프트 생성 및 조합
        CategoryClassifyPrompt instruction = new CategoryClassifyPrompt(
                pageInfo.domain(), pageInfo.title(), content, categoryList);
        String finalUserPrompt = promptComposer.compose(instruction);

        // 4. 호출 및 결과 반환
        return geminiService.callAndParse(
                "당신은 카테고리 분류 전문가입니다.",
                finalUserPrompt,
                GeminiAnalysisResult.class
        );
    }
}
