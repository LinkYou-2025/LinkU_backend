package com.umc.linkyou.service.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.BDDMockito.given;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.linkyou.infra.net.SafeUrlFetcher;

@ExtendWith(MockitoExtension.class)
@DisplayName("UrlReachabilityService 테스트")
class UrlReachabilityServiceTest {

    @InjectMocks private UrlReachabilityService urlReachabilityService;

    @Mock private SafeUrlFetcher safeUrlFetcher;

    private static final String ARTICLE = "https://blog.example.com/post/123";

    @Test
    @DisplayName("접속할 수 없으면 empty를 돌려준다")
    void 접속할_수_없으면_empty를_돌려준다() {
        given(safeUrlFetcher.probe(ARTICLE)).willReturn(Optional.empty());

        assertEquals(Optional.empty(), urlReachabilityService.resolve(ARTICLE));
    }

    @Test
    @DisplayName("경로가 있던 URL이 홈으로 떨어지면 empty를 돌려준다")
    void 경로가_있던_URL이_홈으로_떨어지면_empty를_돌려준다() {
        String home = "https://blog.example.com/";
        given(safeUrlFetcher.probe(ARTICLE))
                .willReturn(
                        Optional.of(
                                new SafeUrlFetcher.ProbeResult(
                                        home,
                                        List.of(
                                                new SafeUrlFetcher.RedirectHop(
                                                        ARTICLE, 302, home)))));

        assertEquals(Optional.empty(), urlReachabilityService.resolve(ARTICLE));
    }

    @Test
    @DisplayName("단축 URL은 리다이렉트를 정리한 URL을 돌려준다")
    void 단축_URL은_리다이렉트를_정리한_URL을_돌려준다() {
        String shortUrl = "https://bit.ly/abc";
        given(safeUrlFetcher.probe(shortUrl))
                .willReturn(
                        Optional.of(
                                new SafeUrlFetcher.ProbeResult(
                                        ARTICLE,
                                        List.of(
                                                new SafeUrlFetcher.RedirectHop(
                                                        shortUrl, 302, ARTICLE)))));

        assertEquals(Optional.of(ARTICLE), urlReachabilityService.resolve(shortUrl));
    }

    @Test
    @DisplayName("같은 사이트 안의 임시 이동은 따라가지 않고 입력 URL을 돌려준다")
    void 같은_사이트_안의_임시_이동은_따라가지_않고_입력_URL을_돌려준다() {
        String login = "https://blog.example.com/login?next=%2Fpost%2F123";
        given(safeUrlFetcher.probe(ARTICLE))
                .willReturn(
                        Optional.of(
                                new SafeUrlFetcher.ProbeResult(
                                        login,
                                        List.of(
                                                new SafeUrlFetcher.RedirectHop(
                                                        ARTICLE, 302, login)))));

        assertEquals(Optional.of(ARTICLE), urlReachabilityService.resolve(ARTICLE));
    }
}
