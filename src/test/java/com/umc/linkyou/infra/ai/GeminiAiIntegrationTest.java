package com.umc.linkyou.infra.ai;

import com.umc.linkyou.domain.Curation;
import com.umc.linkyou.gemini.dto.SummaryResultDTO; // [수정] 새로운 DTO 임포트
import com.umc.linkyou.gemini.service.GeminiCurationService; // [수정] 추가
import com.umc.linkyou.gemini.service.GeminiLinkuService;
import com.umc.linkyou.repository.CurationRepository;
import com.umc.linkyou.repository.EmotionRepository;
import com.umc.linkyou.repository.classification.CategoryRepository;
import com.umc.linkyou.repository.classification.SituationRepository;
import com.umc.linkyou.service.curation.CurationServiceImpl;
import com.umc.linkyou.web.dto.curation.CurationDetailResponse;
import com.umc.linkyou.web.dto.curation.RecommendedLinkResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;


@ActiveProfiles("test")
@SpringBootTest(properties = "spring.sql.init.mode=never")
@DisplayName("Gemini AI 통합 테스트")
class GeminiAiIntegrationTest {

    @Autowired GeminiLinkuService geminiLinkuService;
    @Autowired GeminiCurationService geminiCurationService;
    @Autowired CurationServiceImpl curationService;

    @Autowired CategoryRepository categoryRepository;
    @Autowired CurationRepository curationRepository;

    private static final String TEST_URL = "https://m.blog.naver.com/mellybird/223971731299";
    private static final Long TEST_USER_ID = 47L;

    @Nested
    @DisplayName("GeminiLinkuService: 링크 분석 테스트")
    class LinkuAnalysis {

        @Test
        @DisplayName("성공 - 실제 DB 데이터를 바탕으로 링크 요약 및 카테고리 분류를 수행한다")
        void getFullAnalysis_Success() {
            // Given
            assertThat(categoryRepository.findAll()).isNotEmpty();

            // When
            // [수정 포인트] 타입을 SummaryResultDTO로 변경
            SummaryResultDTO result = geminiLinkuService.getFullAnalysis(TEST_URL);

            // Then
            System.out.println("=== GeminiLinkuService 분석 결과 ===");
            System.out.println("title      : " + result.getTitle());
            System.out.println("summary    : " + result.getSummary());
            System.out.println("categoryId : " + result.getCategoryId());
            System.out.println("keywords   : " + result.getKeywords());

            assertThat(result.getTitle()).isNotBlank();
            assertThat(result.getSummary()).isNotBlank();
            assertThat(result.getKeywords()).contains("#");
            assertThat(categoryRepository.existsById(result.getCategoryId())).isTrue();
        }
    }

    @Nested
    @DisplayName("CurationService: 멘트 및 추천 테스트")
    class CurationAndRecommendation {

        @Test
        @DisplayName("성공 - 유저의 큐레이션 데이터를 바탕으로 AI 멘트를 생성한다")
        void getCurationDetail_Success() {
            // Given
            Curation curation = curationRepository.findTopByUser_IdOrderByCreatedAtDesc(TEST_USER_ID)
                    .orElse(null);
            assumeTrue(curation != null, "테스트 유저의 큐레이션 데이터가 없습니다.");

            // When
            CurationDetailResponse result = curationService.getCurationDetail(curation.getCurationId());

            // Then
            assertThat(result.getHeaderMent()).isNotBlank();
            assertThat(result.getFooterMent()).isNotBlank();
            assertThat(result.getHeaderMent()).doesNotContain("(닉네임)");
        }

        @Test
        @DisplayName("성공 - 구글 검색(Grounding)을 통해 외부 링크를 추천받는다")
        void getExternalRecommendations_Success() {
            // Given
            Curation curation = curationRepository.findTopByUser_IdOrderByCreatedAtDesc(TEST_USER_ID)
                    .orElse(null);
            assumeTrue(curation != null);

            // When
            // [수정 포인트] 리팩토링된 GeminiCurationService 혹은 관련 로직 호출
            // 기존 externalRecommendService가 리팩토링된 버전을 쓰는지 확인 필요
            List<RecommendedLinkResponse> results = geminiCurationService.getExternalRecommendations(
                    List.of(TEST_URL), List.of("맛집", "여행"), 3, "Developer", "Male"
            );

            // Then
            System.out.println("=== 외부 링크 추천 결과 ===");
            results.forEach(link -> System.out.println(link.getTitle() + " : " + link.getUrl()));

            assertThat(results).isNotEmpty();
            assertThat(results.get(0).getDomain()).isNotBlank();
        }
    }
}
