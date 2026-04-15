package com.umc.linkyou.infra.ai;

import com.umc.linkyou.domain.Curation;
import com.umc.linkyou.domain.classification.Category;
import com.umc.linkyou.domain.classification.Emotion;
import com.umc.linkyou.domain.classification.Situation;
import com.umc.linkyou.infra.ai.classifier.GeminiCategoryClassifier;
import com.umc.linkyou.infra.ai.dto.SummaryAnalysisResultDTO;
import com.umc.linkyou.infra.ai.summary.GeminiSummaryUtil;
import com.umc.linkyou.repository.CurationRepository;
import com.umc.linkyou.repository.EmotionRepository;
import com.umc.linkyou.repository.classification.CategoryRepository;
import com.umc.linkyou.repository.classification.SituationRepository;
import com.umc.linkyou.service.curation.CurationServiceImpl;
import com.umc.linkyou.service.curation.linku.ExternalRecommendServiceImpl;
import com.umc.linkyou.web.dto.curation.CurationDetailResponse;
import com.umc.linkyou.web.dto.curation.RecommendedLinkResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

// GOOGLE_APPLICATION_CREDENTIALS 설정 필요
@SpringBootTest(properties = "spring.sql.init.mode=never")
class GeminiAiIntegrationTest {

    @Autowired GeminiSummaryUtil geminiSummaryUtil;
    @Autowired GeminiCategoryClassifier geminiCategoryClassifier;
    @Autowired CurationServiceImpl curationService;
    @Autowired ExternalRecommendServiceImpl externalRecommendService;

    @Autowired SituationRepository situationRepository;
    @Autowired EmotionRepository emotionRepository;
    @Autowired CategoryRepository categoryRepository;
    @Autowired CurationRepository curationRepository;

    // 테스트할 URL
    private static final String TEST_URL = "https://m.blog.naver.com/mellybird/223971731299";

    // 테스트할 유저 ID
    private static final Long TEST_USER_ID = 47L;

    // GeminiSummaryUtil
    @Test
    @DisplayName("getFullAnalysis: 링크 요약,분류 결과를 반환한다")
    void getFullAnalysis_withRealDbData_returnsValidResult() throws Exception {
        List<Situation> situations = situationRepository.findAll();
        List<Emotion> emotions = emotionRepository.findAll();
        List<Category> categories = categoryRepository.findAll();

        assertThat(situations).as("DB에 Situation 데이터가 있어야 합니다").isNotEmpty();
        assertThat(emotions).as("DB에 Emotion 데이터가 있어야 합니다").isNotEmpty();
        assertThat(categories).as("DB에 Category 데이터가 있어야 합니다").isNotEmpty();

        SummaryAnalysisResultDTO result = geminiSummaryUtil.getFullAnalysis(
                TEST_URL, situations, emotions, categories
        );

        System.out.println("=== GeminiSummaryUtil 결과 ===");
        System.out.println("title      : " + result.getTitle());
        System.out.println("summary    : " + result.getSummary());
        System.out.println("situationId: " + result.getSituationId());
        System.out.println("emotionId  : " + result.getEmotionId());
        System.out.println("categoryId : " + result.getCategoryId());
        System.out.println("keywords   : " + result.getKeywords());

        assertThat(result.getTitle()).isNotBlank();
        assertThat(result.getSummary()).isNotBlank();
        assertThat(result.getKeywords()).isNotBlank();

        // 반환된 ID가 실제 DB에 존재하는 값인지 검증
        assertThat(situations).anyMatch(s -> s.getId().equals(result.getSituationId()));
        assertThat(emotions).anyMatch(e -> e.getEmotionId().equals(result.getEmotionId()));
        assertThat(categories).anyMatch(c -> c.getCategoryId().equals(result.getCategoryId()));
    }

    // GeminiCategoryClassifier
    @Test
    @DisplayName("classifyCategoryByUrl: DB 카테고리 목록으로 URL 분류 결과를 정상 반환한다")
    void classifyCategoryByUrl_withRealDbData_returnsValidResult() {
        List<Category> categories = categoryRepository.findAll();
        assertThat(categories).as("DB에 Category 데이터가 있어야 합니다").isNotEmpty();

        GeminiCategoryClassifier.CategoryResult result =
                geminiCategoryClassifier.classifyCategoryByUrl(TEST_URL, categories);

        System.out.println("=== GeminiCategoryClassifier 결과 ===");
        System.out.println("categoryId: " + (result != null ? result.getCategoryId() : "null"));
        System.out.println("keywords  : " + (result != null ? result.getKeywords() : "null"));

        assertThat(result).isNotNull();
        assertThat(result.getKeywords()).isNotBlank();

        // 반환된 categoryId가 실제 DB에 존재하는 값인지 검증
        assertThat(categories).anyMatch(c -> c.getCategoryId().equals(result.getCategoryId()));
    }

    // 큐레이션 멘트 생성
    @Test
    @DisplayName("getCurationDetail: 유저의 최근 큐레이션에 대해 Gemini 멘트가 생성된다")
    void getCurationDetail_forRealUser_returnsMent() {
        Curation curation = curationRepository.findTopByUser_IdOrderByCreatedAtDesc(TEST_USER_ID)
                .orElse(null);
        assumeTrue(curation != null, "TEST_USER_ID=" + TEST_USER_ID + " 의 큐레이션이 없어 테스트를 건너뜁니다.");

        CurationDetailResponse result = curationService.getCurationDetail(curation.getCurationId());

        System.out.println("=== 큐레이션 멘트 생성 결과 ===");
        System.out.println("curationId : " + result.getCurationId());
        System.out.println("month      : " + result.getMonth());
        System.out.println("topTags    : " + result.getTopTags());
        System.out.println("headerMent : " + result.getHeaderMent());
        System.out.println("footerMent : " + result.getFooterMent());

        assertThat(result.getHeaderMent()).isNotBlank();
        assertThat(result.getFooterMent()).isNotBlank();
        // (닉네임) 플레이스홀더가 실제 닉네임으로 치환됐는지 확인
        assertThat(result.getHeaderMent()).doesNotContain("(닉네임)");
        assertThat(result.getFooterMent()).doesNotContain("(닉네임)");
    }

    // 외부 링크 추천
    @Test
    @DisplayName("getExternalRecommendations: 유저 프로필과 태그 기반으로 외부 링크를 추천한다")
    void getExternalRecommendations_forRealUser_returnsLinks() {
        Curation curation = curationRepository.findTopByUser_IdOrderByCreatedAtDesc(TEST_USER_ID)
                .orElse(null);
        assumeTrue(curation != null, "TEST_USER_ID=" + TEST_USER_ID + " 의 큐레이션이 없어 테스트를 건너뜁니다.");

        List<RecommendedLinkResponse> results = externalRecommendService.getExternalRecommendations(
                TEST_USER_ID, curation.getCurationId(), 3
        );

        System.out.println("=== 외부 링크 추천 결과 ===");
        results.forEach(link -> {
            System.out.println("title  : " + link.getTitle());
            System.out.println("url    : " + link.getUrl());
            System.out.println("domain : " + link.getDomain());
            System.out.println();
        });

        assertThat(results).isNotNull();
        assertThat(results).isNotEmpty();
        assertThat(results).allSatisfy(link -> assertThat(link.getUrl()).isNotBlank());
    }
}
