package com.umc.linkyou.service.common;

import com.umc.linkyou.infra.parser.LinkToImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
public class ImageFetchService {

    private final LinkToImageService linkToImageService;

    // ExternalRecommendWorker(externalRecoTaskExecutor)가 이 결과를 join()으로 기다리므로
    // 같은 풀을 쓰면 데드락 위험이 있어 전용 풀로 분리
    @Async("imageFetchTaskExecutor")
    public CompletableFuture<String> fetchAsync(String url, String title) {
        try {
            return CompletableFuture.completedFuture(
                    linkToImageService.getRelatedImageFromUrl(url, title));
        } catch (Exception e) {
            return CompletableFuture.completedFuture(null);
        }
    }
}
