package com.umc.linkyou.service.common;

import com.umc.linkyou.infra.parser.LinkToImageService;
import com.umc.linkyou.infra.parser.NaverBlogIframeFetcher;
import lombok.RequiredArgsConstructor;
import org.jsoup.nodes.Document;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

// 대표 이미지 추출을 전용 풀(imageFetchTaskExecutor)에서 비동기로 실행한다. 실패 시 null로 완료된다.
@Component
@RequiredArgsConstructor
public class ImageFetchService {

    private final LinkToImageService linkToImageService;

    @Async("imageFetchTaskExecutor")
    public CompletableFuture<String> fetchAsync(String url, String title) {
        try {
            return CompletableFuture.completedFuture(
                    linkToImageService.getRelatedImageFromUrl(url, title));
        } catch (Exception e) {
            return CompletableFuture.completedFuture(null);
        }
    }

    // 이미 fetch한 Document를 재사용해 대표 이미지를 추출한다.
    @Async("imageFetchTaskExecutor")
    public CompletableFuture<String> fetchAsync(String url, Document doc) {
        return fetchAsyncInternal(url, doc, null);
    }

    // 이미 fetch된 Naver iframe도 함께 받아 중복 네트워크 요청을 피한다.
    @Async("imageFetchTaskExecutor")
    public CompletableFuture<String> fetchAsync(
            String url, Document doc, NaverBlogIframeFetcher.Result iframePage) {
        return fetchAsyncInternal(url, doc, iframePage);
    }

    private CompletableFuture<String> fetchAsyncInternal(
            String url, Document doc, NaverBlogIframeFetcher.Result iframePage) {
        try {
            return CompletableFuture.completedFuture(
                    linkToImageService.getRelatedImageFromUrl(url, null, doc, iframePage));
        } catch (Exception e) {
            return CompletableFuture.completedFuture(null);
        }
    }
}
