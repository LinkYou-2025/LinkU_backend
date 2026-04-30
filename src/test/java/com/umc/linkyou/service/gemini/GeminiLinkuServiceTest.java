package com.umc.linkyou.service.gemini;

import com.umc.linkyou.gemini.dto.SummaryResultDTO;
import com.umc.linkyou.gemini.prompt.common.PromptComposer;
import com.umc.linkyou.gemini.service.GeminiLinkuService;
import com.umc.linkyou.gemini.service.GeminiService;
import com.umc.linkyou.utils.parser.WebContentExtractor;
import com.umc.linkyou.repository.classification.CategoryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("GeminiLinkuService 테스트")
class GeminiLinkuServiceTest {

    @Mock private GeminiService geminiService;
    @Mock private WebContentExtractor webContentExtractor;
    @Mock private CategoryRepository categoryRepository;

    // PromptComposer Mock 객체 선언
    @Mock private PromptComposer promptComposer;

    @InjectMocks
    private GeminiLinkuService geminiLinkuService;

    @Nested
    @DisplayName("전체 분석 조회 (getFullAnalysis)")
    class GetFullAnalysis {

        @Nested
        @DisplayName("성공 케이스")
        class Success {

            @Test
            @DisplayName("정상적인 URL이 입력되면 분석 결과를 반환한다")
            void 전체분석_조회_성공() {
                // given
                String url = "https://tech-blog.com/post/1";
                SummaryResultDTO mockResult = new SummaryResultDTO();

                given(webContentExtractor.extractTextFromUrl(url)).willReturn("본문");

                // promptComposer 동작 정의
                given(promptComposer.compose(any())).willReturn("Full Prompt Content");

                given(geminiService.callAndParse(anyString(), anyString(), eq(SummaryResultDTO.class)))
                        .willReturn(mockResult);

                // when
                SummaryResultDTO result = geminiLinkuService.getFullAnalysis(url);

                // then
                assertThat(result).isNotNull();
            }
        }

        @Nested
        @DisplayName("실패 케이스")
        class Failure {

            @Test
            @DisplayName("본문 추출에 실패해도 안전하게 처리한다")
            void 본문추출_실패시_처리() {
                // given
                given(webContentExtractor.extractTextFromUrl(any())).willReturn(null);

                // promptComposer 동작 정의
                given(promptComposer.compose(any())).willReturn("Empty Content Prompt");

                given(geminiService.callAndParse(anyString(), anyString(), eq(SummaryResultDTO.class)))
                        .willReturn(new SummaryResultDTO());

                // when
                SummaryResultDTO result = geminiLinkuService.getFullAnalysis("https://invalid.com");

                // then
                assertThat(result).isNotNull();
            }
        }
    }
}
