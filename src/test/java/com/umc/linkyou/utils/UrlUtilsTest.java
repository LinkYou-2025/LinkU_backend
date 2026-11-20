package com.umc.linkyou.utils;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class UrlUtilsTest {

    @Test
    void 스킴과_호스트만_소문자로_바꾸고_경로와_쿼리는_유지한다() {
        String result = UrlUtils.normalizeUrl("HTTPS://WWW.YouTube.COM/Watch?v=dQw4w9WgXcQ");

        assertThat(result).isEqualTo("https://www.youtube.com/Watch?v=dQw4w9WgXcQ");
    }

    @Test
    void 포트와_프래그먼트도_대소문자를_보존한다() {
        String result = UrlUtils.normalizeUrl("Http://Example.COM:8080/Path#Section");

        assertThat(result).isEqualTo("http://example.com:8080/Path#Section");
    }

    @Test
    void 호스트_뒤에_바로_쿼리가_와도_쿼리는_유지한다() {
        String result = UrlUtils.normalizeUrl("https://Example.com?Q=ABC");

        assertThat(result).isEqualTo("https://example.com?Q=ABC");
    }

    @Test
    void 루트가_아닌_경로의_끝_슬래시는_제거한다() {
        assertThat(UrlUtils.normalizeUrl("  https://Example.com/Blog/Post/  "))
                .isEqualTo("https://example.com/Blog/Post");
        assertThat(UrlUtils.normalizeUrl("https://Example.com/")).isEqualTo("https://example.com/");
    }

    @Test
    void 사용자_정보는_대소문자를_유지한다() {
        assertThat(UrlUtils.normalizeUrl("https://User:Pass@Example.COM/A"))
                .isEqualTo("https://User:Pass@example.com/A");
    }

    @Test
    void URL_형식이_아니면_공백만_제거한다() {
        assertThat(UrlUtils.normalizeUrl("  Not A Url ")).isEqualTo("Not A Url");
        assertThat(UrlUtils.normalizeUrl(null)).isNull();
    }
}
