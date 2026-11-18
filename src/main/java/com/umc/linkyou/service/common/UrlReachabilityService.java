package com.umc.linkyou.service.common;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.umc.linkyou.infra.net.SafeUrlFetcher;
import com.umc.linkyou.utils.CanonicalUrlPolicy;
import com.umc.linkyou.utils.UrlUtils;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class UrlReachabilityService {

    private final SafeUrlFetcher safeUrlFetcher;

    @Async("urlCheckTaskExecutor")
    public CompletableFuture<Optional<String>> resolveAsync(String url) {
        return CompletableFuture.completedFuture(resolve(url));
    }

    // AI 추천 주소는 홈 리다이렉트 200을 주는 경우에 접속 불가로 본다.
    public Optional<String> resolve(String url) {
        String inputUrl = UrlUtils.normalizeUrl(url);
        return safeUrlFetcher
                .probe(inputUrl)
                .filter(probe -> !CanonicalUrlPolicy.collapsedToRoot(inputUrl, probe.finalUrl()))
                .map(probe -> CanonicalUrlPolicy.followTrustedRedirects(inputUrl, probe.hops()));
    }
}
