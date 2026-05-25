package com.umc.linkyou.service.gemini;

import com.fasterxml.jackson.core.type.TypeReference;
import com.umc.linkyou.infra.ai.dto.ExternalLinkResultDTO;
import com.umc.linkyou.infra.ai.dto.ExternalSearchRequest;
import com.umc.linkyou.infra.gemini.prompt.common.PromptComposer;
import com.umc.linkyou.infra.gemini.service.GeminiExternalSearchService;
import com.umc.linkyou.infra.gemini.service.GeminiService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("GeminiExternalSearchService 테스트")
class GeminiCurationServiceTest {

    @Mock private GeminiService geminiService;
    @Mock private PromptComposer promptComposer;

    @InjectMocks
    private GeminiExternalSearchService geminiExternalSearchService;

    @Nested
    @DisplayName("외부 링크 추천 (searchExternalLinks)")
    class SearchExternalLinks {

        @Nested
        @DisplayName("성공 케이스")
        class Success {

            @Test
            @DisplayName("태그와 직무를 기반으로 외부 링크 추천 리스트를 반환한다")
            void 외부링크_추천_성공() {
                List<ExternalLinkResultDTO> mockRawList = List.of(
                        ExternalLinkResultDTO.builder().title("추천 아티클").url("https://tistory.com/tech").build()
                );

                given(promptComposer.externalSearch()).willReturn("system prompt");
                given(geminiService.callAndParseWithSearch(anyString(), anyString(), any(TypeReference.class)))
                        .willReturn(mockRawList);

                ExternalSearchRequest request = new ExternalSearchRequest(List.of("Spring", "Java"), 5, "Developer", "MALE");
                List<ExternalLinkResultDTO> results = geminiExternalSearchService.searchExternalLinks(request);

                assertThat(results).hasSize(1);
                assertThat(results.get(0).getUrl()).isEqualTo("https://tistory.com/tech");
                assertThat(results.get(0).getTitle()).isEqualTo("추천 아티클");
            }
        }

        @Nested
        @DisplayName("실패 케이스")
        class Failure {

            @Test
            @DisplayName("AI 응답에서 URL이 누락된 항목은 필터링되어 반환되지 않는다")
            void URL누락항목_필터링() {
                List<ExternalLinkResultDTO> mockRawList = List.of(
                        ExternalLinkResultDTO.builder().title("잘못된 응답").url("").build()
                );

                given(promptComposer.externalSearch()).willReturn("system prompt");
                given(geminiService.callAndParseWithSearch(anyString(), anyString(), any(TypeReference.class)))
                        .willReturn(mockRawList);

                ExternalSearchRequest request = new ExternalSearchRequest(List.of(), 5, "Dev", "N");
                List<ExternalLinkResultDTO> results = geminiExternalSearchService.searchExternalLinks(request);

                assertThat(results).isEmpty();
            }
        }
    }
}
