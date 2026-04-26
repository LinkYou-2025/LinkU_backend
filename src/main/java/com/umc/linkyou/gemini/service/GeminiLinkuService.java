package com.umc.linkyou.gemini.service;

import com.umc.linkyou.gemini.dto.ClassifyResultDTO;
import com.umc.linkyou.gemini.dto.SummaryResultDTO;
import com.umc.linkyou.gemini.prompt.common.PromptComposer;
import com.umc.linkyou.gemini.prompt.linku.CategoryClassifyPrompt;
import com.umc.linkyou.gemini.prompt.linku.LinkSummaryPrompt;
import com.umc.linkyou.repository.EmotionRepository;
import com.umc.linkyou.repository.classification.SituationRepository;
import com.umc.linkyou.utils.parser.TitleDomainParser;
import com.umc.linkyou.utils.parser.WebContentExtractor;
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
    private final SituationRepository situationRepository;
    private final EmotionRepository emotionRepository;

    /**
     * 단순 분류 (Linku 생성 시 사용)
     */
    public ClassifyResultDTO classify(String url) {
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
                "당신은 주어진 카테고리별로 폴더를 분류하는 전문가다.",
                finalUserPrompt,
                ClassifyResultDTO.class
        );
    }
    /**
     * 링크 전체 분석 (객관적 정보 중심)
     */
    public SummaryResultDTO getFullAnalysis(String url) {
        String pageContent = webContentExtractor.extractTextFromUrl(url);
        if (pageContent != null && pageContent.length() > 2800) {
            pageContent = pageContent.substring(0, 2800);
        }

        String categoryList = getCategoryListString();

        // [수정] 상황, 감정 목록을 넣지 않고 본문과 카테고리만 전달
        LinkSummaryPrompt instruction = new LinkSummaryPrompt(pageContent, categoryList);

        return geminiService.callAndParse(
                "당신은 웹 콘텐츠 메타데이터 추출 전문가입니다.",
                promptComposer.compose(instruction),
                SummaryResultDTO.class
        );
    }

    private String getCategoryListString() {
        return categoryRepository.findAll().stream()
                .map(c -> "- id: " + c.getCategoryId() + ", name: \"" + c.getCategoryName() + "\"")
                .collect(Collectors.joining("\n"));
    }
}
