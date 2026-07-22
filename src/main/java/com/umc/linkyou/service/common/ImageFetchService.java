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

    @Async("defaultTaskExecutor")
    public CompletableFuture<String> fetchAsync(String url, String title) {
        try {
            return CompletableFuture.completedFuture(
                    linkToImageService.getRelatedImageFromUrl(url, title));
        } catch (Exception e) {
            return CompletableFuture.completedFuture(null);
        }
    }
}
