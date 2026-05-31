package com.umc.linkyou.service.gemini;

import com.umc.linkyou.infra.ai.dto.LinkuResultDTO;
import com.umc.linkyou.infra.gemini.prompt.common.PromptComposer;
import com.umc.linkyou.infra.gemini.service.GeminiLinkuService;
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

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("GeminiLinkuService 테스트")
class GeminiLinkuServiceTest {

    @Mock private GeminiService geminiService;
    @Mock private WebContentExtractor webContentExtractor;
    @Mock private TitleDomainParser titleDomainParser;
    @Mock private PromptComposer promptComposer;

    @InjectMocks
    private GeminiLinkuService geminiLinkuService;

    @Nested
    @DisplayName("URL 분석 (analyzeByUrl)")
    class AnalyzeByUrl {

        @Nested
        @DisplayName("성공 케이스")
        class Success {

            @Test
            @DisplayName("제목과 본문이 있으면 분석 결과를 반환한다")
            void URL분석_성공() {
                String url = "https://tech-blog.com/post/1";
                LinkuResultDTO mockResult = new LinkuResultDTO(1L, "Spring,Java");

                given(titleDomainParser.parseUrl(url))
                        .willReturn(new TitleDomainParser.ParsedPageInfo("tech-blog.com", "Tech Article"));
                given(webContentExtractor.extractTextFromUrl(url)).willReturn("본문 내용");
                given(promptComposer.general()).willReturn("system prompt");
                given(geminiService.callAndParse(anyString(), anyString(), eq(LinkuResultDTO.class)))
                        .willReturn(mockResult);

                Optional<LinkuResultDTO> result = geminiLinkuService.analyzeByUrl(url, List.of());

                assertThat(result).isPresent();
                assertThat(result.get().categoryId()).isEqualTo(1L);
            }

            @Test
            @DisplayName("본문 추출에 실패해도 제목이 있으면 분석 결과를 반환한다")
            void 본문추출_실패시_제목으로_분석() {
                String url = "https://tech-blog.com/post/2";
                LinkuResultDTO mockResult = new LinkuResultDTO(2L, "Java");

                given(titleDomainParser.parseUrl(url))
                        .willReturn(new TitleDomainParser.ParsedPageInfo("tech-blog.com", "Tech Article"));
                given(webContentExtractor.extractTextFromUrl(url)).willReturn(null);
                given(promptComposer.general()).willReturn("system prompt");
                given(geminiService.callAndParse(anyString(), anyString(), eq(LinkuResultDTO.class)))
                        .willReturn(mockResult);

                Optional<LinkuResultDTO> result = geminiLinkuService.analyzeByUrl(url, List.of());

                assertThat(result).isPresent();
            }
        }

        @Nested
        @DisplayName("실패 케이스")
        class Failure {

            @Test
            @DisplayName("제목과 본문이 모두 없으면 빈 Optional을 반환한다")
            void 제목_본문_모두_없으면_빈결과() {
                String url = "https://empty.com";

                given(titleDomainParser.parseUrl(url))
                        .willReturn(new TitleDomainParser.ParsedPageInfo("empty.com", null));
                given(webContentExtractor.extractTextFromUrl(url)).willReturn(null);

                Optional<LinkuResultDTO> result = geminiLinkuService.analyzeByUrl(url, List.of());

                assertThat(result).isEmpty();
            }
        }
    }
}
