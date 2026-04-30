package com.umc.linkyou.service.gemini;

import com.fasterxml.jackson.core.type.TypeReference;
import com.umc.linkyou.gemini.prompt.common.PromptComposer;
import com.umc.linkyou.gemini.service.GeminiCurationService;
import com.umc.linkyou.gemini.service.GeminiService;
import com.umc.linkyou.web.dto.curation.RecommendedLinkResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("GeminiCurationService 테스트")
class GeminiCurationServiceTest {

    @Mock private GeminiService geminiService;
    @Mock private PromptComposer promptComposer;

    @InjectMocks
    private GeminiCurationService geminiCurationService;

    @Nested
    @DisplayName("외부 링크 추천 (getExternalRecommendations)")
    class GetExternalRecommendations {

        @Nested
        @DisplayName("성공 케이스")
        class Success {

            @Test
            @DisplayName("직무와 태그를 기반으로 중복 도메인을 제외한 추천 리스트를 반환한다")
            void 외부링크_추천_성공() {
                // given
                List<String> recentUrls = List.of("https://naver.com/news/1");
                List<String> tags = List.of("Spring", "Java");

                List<Map<String, String>> mockRawList = List.of(
                        Map.of("title", "추천 아티클", "url", "https://tistory.com/tech")
                );

                given(promptComposer.composeCuration(any())).willReturn("Full Prompt Content");
                given(geminiService.callAndParseList(anyString(), anyString(), any(TypeReference.class)))
                        .willReturn(mockRawList);

                // when
                List<RecommendedLinkResponse> results =
                        geminiCurationService.getExternalRecommendations(recentUrls, tags, 1, "Developer", "Male");

                // then
                assertThat(results).hasSize(1);
                assertThat(results.get(0).getDomain()).isEqualTo("tistory.com");
                assertThat(results.get(0).getTitle()).isEqualTo("추천 아티클");
            }
        }

        @Nested
        @DisplayName("실패 케이스")
        class Failure {

            @Test
            @DisplayName("AI 응답에서 URL이 누락된 항목은 필터링되어 반환되지 않는다")
            void URL누락항목_필터링() {
                // given
                List<Map<String, String>> mockRawList = List.of(
                        Map.of("title", "잘못된 응답", "url", "") // URL이 비어있는 경우
                );

                // [수정] 인자 매칭을 any()로 변경하여 유연하게 대응
                given(promptComposer.composeCuration(any())).willReturn("any prompt");

                given(geminiService.callAndParseList(
                        anyString(),
                        any(),        // anyString() 대신 any()를 써야 null 허용됨
                        any(TypeReference.class)
                )).willReturn(mockRawList);

                // when
                List<RecommendedLinkResponse> results =
                        geminiCurationService.getExternalRecommendations(List.of(), List.of(), 1, "Dev", "N");

                // then
                assertThat(results).isEmpty();
            }
        }
    }
}
