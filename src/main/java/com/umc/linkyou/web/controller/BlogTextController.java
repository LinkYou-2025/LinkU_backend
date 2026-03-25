package com.umc.linkyou.web.controller;

import com.umc.linkyou.apiPayload.ApiResponse;
import com.umc.linkyou.service.BlogTextService;
import com.umc.linkyou.web.dto.BlogTextRequestDTO;
import com.umc.linkyou.web.dto.BlogTextResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/blogs/text")
@Tag(name = "Dataset API", description = "KoBART 데이터셋 수집 전용 API (DB 저장 미사용)")
public class BlogTextController {

    private final BlogTextService blogTextService;

    @PostMapping("/preview")
    @Operation(summary = "단일 블로그 크롤링 미리보기", description = "저장 없이 본문 추출 결과만 JSON으로 확인합니다.")
    public ApiResponse<BlogTextResponseDTO> previewBlogText(@RequestParam String url) {
        return ApiResponse.onSuccess(blogTextService.getBlogText(url));
    }

    @Operation(
            summary = "URL 리스트로 즉시 CSV 다운로드",
            description = "입력된 URL 리스트를 크롤링하여 DB 저장 없이 즉시 CSV로 내뱉습니다."
    )
    @PostMapping(value = "/export") // produces = "text/csv"는 브라우저가 인식하도록 유지
    public void exportDirectCsv(@RequestBody BlogTextRequestDTO request, HttpServletResponse response) throws IOException {

        // 1. 파일 설정
        String fileName = "blog_dataset_" + LocalDate.now() + ".csv";
        response.setContentType("text/csv; charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");

        // 2. BOM 처리 (엑셀 한글 깨짐 방지 핵심)
        response.getWriter().write('\ufeff');

        // 3. 실시간 크롤링 및 CSV 작성
        if (request.getUrls() != null && !request.getUrls().isEmpty()) {
            blogTextService.writeCsvDirectly(request.getUrls(), response.getWriter());
        }

        response.getWriter().flush();
        response.getWriter().close();
        // void이므로 return ApiResponse... 를 하지 않습니다.
    }
}
