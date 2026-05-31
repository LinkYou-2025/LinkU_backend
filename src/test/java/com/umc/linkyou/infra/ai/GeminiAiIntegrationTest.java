package com.umc.linkyou.infra.ai;

import com.fasterxml.jackson.core.type.TypeReference;
import com.umc.linkyou.domain.classification.Category;
import com.umc.linkyou.domain.classification.Emotion;
import com.umc.linkyou.domain.classification.Situation;
import com.umc.linkyou.infra.ai.dto.MentResultDTO;
import com.umc.linkyou.infra.ai.dto.AiArticleResultDTO;
import com.umc.linkyou.infra.ai.dto.LinkuResultDTO;
import com.umc.linkyou.infra.ai.dto.ExternalLinkResultDTO;
import com.umc.linkyou.infra.ai.dto.ExternalSearchRequest;
import com.umc.linkyou.infra.gemini.prompt.common.PromptComposer;
import com.umc.linkyou.infra.gemini.service.GeminiArticleService;
import com.umc.linkyou.infra.gemini.service.GeminiExternalSearchService;
import com.umc.linkyou.infra.gemini.service.GeminiLinkuService;
import com.umc.linkyou.infra.gemini.service.GeminiMentService;
import com.umc.linkyou.infra.gemini.service.GeminiService;
import com.umc.linkyou.infra.parser.TitleDomainParser;
import com.umc.linkyou.infra.parser.WebContentExtractor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.linkyou.apiPayload.exception.GeneralException;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("Gemini AI 서비스 단위 테스트")
class GeminiAiIntegrationTest {

    // GeminiArticleService: URL → 제목·요약·감정·상황·카테고리 전체 분석
    @Nested
    @DisplayName("GeminiArticleService: 아티클 전체 분석")
    class ArticleAnalysis {

        @Mock private WebContentExtractor webContentExtractor;
        @Mock private GeminiService geminiService;
        @Mock private PromptComposer promptComposer;
        @InjectMocks private GeminiArticleService geminiArticleService;

        private final List<Situation> situations = List.of(
                Situation.builder().id(1L).name("업무·학습").build()
        );
        private final List<Emotion> emotions = List.of(
                Emotion.builder().emotionId(1L).name("기쁨").build()
        );
        @Test
        @DisplayName("성공 - 페이지 본문 추출 성공 시 AI 분석 결과를 반환한다")
        void analyzeByUrl_success() {
            // given
            String url = "https://tech-blog.com/spring-tips";
            AiArticleResultDTO mockResult = new AiArticleResultDTO(
                    "Spring 팁 모음", "요약 내용입니다.", 1L, 1L, "#Spring #Java"
            );

            given(webContentExtractor.extractTextFromUrl(url)).willReturn("Spring 관련 본문 내용...");
            given(promptComposer.general()).willReturn("system prompt");
            given(geminiService.callAndParse(anyString(), anyString(), eq(AiArticleResultDTO.class)))
                    .willReturn(mockResult);

            // when
            AiArticleResultDTO result = geminiArticleService.analyzeByUrl(url, situations, emotions);

            // then
            assertThat(result.title()).isEqualTo("Spring 팁 모음");
            assertThat(result.summary()).isEqualTo("요약 내용입니다.");
            assertThat(result.keywords()).contains("#");
        }

        @Test
        @DisplayName("실패 - 페이지 본문이 null이면 GeneralException을 던진다")
        void analyzeByUrl_nullContent_throwsGeneralException() {
            // given
            given(webContentExtractor.extractTextFromUrl(any())).willReturn(null);

            // when & then
            assertThatThrownBy(() ->
                    geminiArticleService.analyzeByUrl("https://empty.com", situations, emotions))
                    .isInstanceOf(GeneralException.class);
        }

        @Test
        @DisplayName("실패 - 페이지 본문이 공백만 있으면 GeneralException을 던진다")
        void analyzeByUrl_blankContent_throwsGeneralException() {
            // given
            given(webContentExtractor.extractTextFromUrl(any())).willReturn("   ");

            // when & then
            assertThatThrownBy(() ->
                    geminiArticleService.analyzeByUrl("https://blank.com", situations, emotions))
                    .isInstanceOf(GeneralException.class);
        }
    }

    // GeminiLinkuService: URL → 카테고리·키워드 분류
    @Nested
    @DisplayName("GeminiLinkuService: URL 카테고리 분류")
    class LinkuCategoryClassification {

        @Mock private TitleDomainParser titleDomainParser;
        @Mock private WebContentExtractor webContentExtractor;
        @Mock private GeminiService geminiService;
        @Mock private PromptComposer promptComposer;
        @InjectMocks private GeminiLinkuService geminiLinkuService;

        private final List<Category> categories = List.of(
                Category.builder().categoryId(1L).categoryName("기술·IT").build()
        );

        @Test
        @DisplayName("성공 - 제목이 있는 URL은 Gemini가 카테고리와 키워드를 반환한다")
        void classifyCategory_success() {
            // given
            String url = "https://tech.com/post/1";
            LinkuResultDTO mockResult = new LinkuResultDTO(1L, "#Spring #Java");

            given(titleDomainParser.parseUrl(url))
                    .willReturn(new TitleDomainParser.ParsedPageInfo("tech.com", "Spring Boot 활용 가이드"));
            given(webContentExtractor.extractTextFromUrl(url)).willReturn("Spring Boot 관련 본문...");
            given(promptComposer.general()).willReturn("system prompt");
            given(geminiService.callAndParse(anyString(), anyString(), eq(LinkuResultDTO.class)))
                    .willReturn(mockResult);

            // when
            Optional<LinkuResultDTO> result = geminiLinkuService.analyzeByUrl(url, categories);

            // then
            assertThat(result).isPresent();
            assertThat(result.get().categoryId()).isEqualTo(1L);
            assertThat(result.get().keywords()).contains("#");
        }

        @Test
        @DisplayName("엣지 케이스 - 제목이 없어도 본문이 있으면 AI를 호출한다")
        void classifyCategory_blankTitle_withContent_callsAi() {
            // given
            String url = "https://example.com";
            LinkuResultDTO mockResult = new LinkuResultDTO(3L, "#디자인");

            given(titleDomainParser.parseUrl(url))
                    .willReturn(new TitleDomainParser.ParsedPageInfo("example.com", ""));
            given(webContentExtractor.extractTextFromUrl(url)).willReturn("페이지 본문 내용...");
            given(promptComposer.general()).willReturn("system prompt");
            given(geminiService.callAndParse(anyString(), anyString(), eq(LinkuResultDTO.class)))
                    .willReturn(mockResult);

            // when
            Optional<LinkuResultDTO> result = geminiLinkuService.analyzeByUrl(url, categories);

            // then
            assertThat(result).isPresent();
            assertThat(result.get().categoryId()).isEqualTo(3L);
        }

        @Test
        @DisplayName("실패 - 제목·본문 모두 없으면 Optional.empty()를 반환한다")
        void classifyCategory_noTitleNoContent_returnsEmpty() {
            // given — 제목·본문 모두 없는 경우 (webContentExtractor는 기본 null 반환)
            given(titleDomainParser.parseUrl(any()))
                    .willReturn(new TitleDomainParser.ParsedPageInfo("example.com", null));

            // when
            Optional<LinkuResultDTO> result = geminiLinkuService.analyzeByUrl("https://unknown.com", List.of());

            // then
            assertThat(result).isEmpty();
        }
    }

    //  GeminiMentService: 감정 이름 → 큐레이션 헤더·푸터 멘트 생성
    @Nested
    @DisplayName("GeminiMentService: 큐레이션 멘트 생성")
    class MentGeneration {

        @Mock private GeminiService geminiService;
        @Mock private PromptComposer promptComposer;
        @InjectMocks private GeminiMentService geminiMentService;

        @Test
        @DisplayName("성공 - 감정 이름을 기반으로 헤더·푸터 멘트를 반환한다")
        void generateMent_success() {
            // given
            MentResultDTO mockResult = new MentResultDTO("오늘도 수고했어요!", "더 많은 링크를 저장해보세요.");

            given(promptComposer.mention()).willReturn("ment system prompt");
            given(geminiService.callAndParseCreative(anyString(), anyString(), eq(MentResultDTO.class)))
                    .willReturn(mockResult);

            // when
            MentResultDTO result = geminiMentService.generateMent("기쁨");

            // then
            assertThat(result.header()).isEqualTo("오늘도 수고했어요!");
            assertThat(result.footer()).isEqualTo("더 많은 링크를 저장해보세요.");
        }
    }

    // GeminiExternalSearchService: 태그 → 외부 링크 검색·추천
    @Nested
    @DisplayName("GeminiExternalSearchService: 외부 링크 추천")
    class ExternalLinkSearch {

        @Mock private GeminiService geminiService;
        @Mock private PromptComposer promptComposer;
        @InjectMocks private GeminiExternalSearchService geminiExternalSearchService;

        @Test
        @DisplayName("성공 - 태그·직무·성별 기반으로 외부 링크 목록을 반환한다")
        void searchExternalLinks_success() {
            // given
            List<ExternalLinkResultDTO> mockRaw = List.of(
                    ExternalLinkResultDTO.builder().title("Spring 공식 문서").url("https://spring.io/docs").build(),
                    ExternalLinkResultDTO.builder().title("Java 튜토리얼").url("https://docs.oracle.com/javase/tutorial").build()
            );

            given(promptComposer.externalSearch()).willReturn("search system prompt");
            given(geminiService.callAndParseWithSearch(anyString(), anyString(), any(TypeReference.class)))
                    .willReturn(mockRaw);

            // when
            List<ExternalLinkResultDTO> results = geminiExternalSearchService.searchExternalLinks(
                    new ExternalSearchRequest(List.of("Spring", "Java"), 3, "Developer", "Male"));

            // then
            assertThat(results).hasSize(2);
            assertThat(results.get(0).getUrl()).isEqualTo("https://spring.io/docs");
            assertThat(results.get(0).getTitle()).isEqualTo("Spring 공식 문서");
        }

        @Test
        @DisplayName("실패 - URL이 비어있는 항목은 필터링되어 결과에서 제외된다")
        void searchExternalLinks_emptyUrl_filtered() {
            // given
            List<ExternalLinkResultDTO> mockRaw = List.of(
                    ExternalLinkResultDTO.builder().title("유효한 링크").url("https://valid.com").build(),
                    ExternalLinkResultDTO.builder().title("URL 없는 링크").url("").build()
            );

            given(promptComposer.externalSearch()).willReturn("search system prompt");
            given(geminiService.callAndParseWithSearch(anyString(), anyString(), any(TypeReference.class)))
                    .willReturn(mockRaw);

            // when
            List<ExternalLinkResultDTO> results = geminiExternalSearchService.searchExternalLinks(
                    new ExternalSearchRequest(List.of("태그1"), 5, "Designer", "Female"));

            // then
            assertThat(results).hasSize(1);
            assertThat(results.get(0).getUrl()).isEqualTo("https://valid.com");
        }

        @Test
        @DisplayName("엣지 케이스 - limit보다 많은 결과가 있으면 limit 수만큼만 반환한다")
        void searchExternalLinks_respectsLimit() {
            // given
            List<ExternalLinkResultDTO> mockRaw = List.of(
                    ExternalLinkResultDTO.builder().title("링크1").url("https://link1.com").build(),
                    ExternalLinkResultDTO.builder().title("링크2").url("https://link2.com").build(),
                    ExternalLinkResultDTO.builder().title("링크3").url("https://link3.com").build(),
                    ExternalLinkResultDTO.builder().title("링크4").url("https://link4.com").build()
            );

            given(promptComposer.externalSearch()).willReturn("system prompt");
            given(geminiService.callAndParseWithSearch(anyString(), anyString(), any(TypeReference.class)))
                    .willReturn(mockRaw);

            // when
            List<ExternalLinkResultDTO> results = geminiExternalSearchService.searchExternalLinks(
                    new ExternalSearchRequest(List.of("태그"), 2, "PM", "Male"));

            // then
            assertThat(results).hasSize(2);
        }
    }
}
