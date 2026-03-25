package com.umc.linkyou.service;

import com.umc.linkyou.apiPayload.exception.GeneralException;
import com.umc.linkyou.infra.parser.TitleDomainParser;
import com.umc.linkyou.infra.parser.WebContentExtractor;
import com.umc.linkyou.web.dto.BlogTextResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.util.List;


@Service
@RequiredArgsConstructor
@Slf4j
public class BlogTextService {

    private final WebContentExtractor webContentExtractor;
    private final TitleDomainParser titleDomainParser;

    public BlogTextResponseDTO getBlogText(String url) {
        // 1. 도메인 및 제목 파싱 (robots.txt의 영향을 덜 받음)
        TitleDomainParser.ParsedPageInfo pageInfo = titleDomainParser.parseUrl(url);

        String cleanText;
        try {
            // 2. 본문 추출 시도
            String rawText = webContentExtractor.extractTextFromUrl(url);
            cleanText = rawText.replaceAll("[\r\n\t]", " ").replaceAll(" +", " ").trim();
        } catch (GeneralException e) {
            // robots.txt 차단(PROHIBITED)이나 추출 실패(FAILED) 시 처리
            log.warn("[데이터셋 수집 보완] 본문 추출 불가로 제목/도메인 대체 → URL: {}, 사유: {}", url, e.getMessage());

            // 제목이 있으면 제목을, 없으면 도메인을 본문 데이터로 대체 (학습용 최소 정보)
            cleanText = (pageInfo.title() != null && !pageInfo.title().isBlank())
                    ? "[본문추출불가] " + pageInfo.title()
                    : "[본문추출불가] " + pageInfo.domain();
        } catch (Exception e) {
            cleanText = "[알수없는에러] " + url;
        }

        return BlogTextResponseDTO.builder()
                .url(url)
                .title(pageInfo.title() != null ? pageInfo.title() : "제목없음")
                .domain(pageInfo.domain())
                .cleanText(cleanText)
                .textLength(cleanText.length())
                .crawledAt(LocalDateTime.now())
                .build();
    }

    public void writeCsvDirectly(List<String> urls, PrintWriter writer) {
        writer.println("URL,Title,Domain,CleanText,Length,CrawledAt");

        for (String url : urls) {
            try {
                BlogTextResponseDTO data = getBlogText(url);

                writer.printf("\"%s\",\"%s\",\"%s\",\"%s\",%d,\"%s\"\n",
                        data.getUrl(),
                        data.getTitle().replace("\"", "'"),
                        data.getDomain(),
                        data.getCleanText().replace("\"", "'"),
                        data.getTextLength(),
                        data.getCrawledAt());
            } catch (Exception e) {
                log.error("[CSV 작성 실패] URL: {}", url);
                // e.getMessage()가 null일 경우를 대비해 "Unknown Error" 등으로 처리
                String errorMessage = (e.getMessage() != null) ? e.getMessage().replace("\"", "'") : "Internal Server Error";
                writer.printf("\"%s\",\"FAILED\",\"ERROR\",\"%s\",0,\"%s\"\n",
                        url, errorMessage, LocalDateTime.now());
            }
        }
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        // 쌍따옴표를 홑따옴표로 바꾸거나 쌍따옴표 두 개("")로 만드는 방식 중 선택
        // 여기서는 가장 안전한 홑따옴표 치환 방식을 사용합니다.
        return value.replace("\"", "'");
    }
}
