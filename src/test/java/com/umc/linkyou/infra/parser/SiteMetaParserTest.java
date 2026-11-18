package com.umc.linkyou.infra.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("SiteMetaParser 테스트")
class SiteMetaParserTest {

    private static final String HOME = "https://example.com";

    @Nested
    @DisplayName("iconCandidates")
    class IconCandidates {

        @Test
        @DisplayName("apple-touch-icon을 먼저, 그다음 큰 icon 순으로 돌려주고 관례 경로를 마지막에 붙인다")
        void apple_touch_icon을_먼저_그다음_큰_icon_순으로_돌려준다() {
            Document doc =
                    html(
                            "<link rel=\"icon\" sizes=\"16x16\" href=\"/icon-16.png\">"
                                    + "<link rel=\"icon\" sizes=\"192x192\" href=\"/icon-192.png\">"
                                    + "<link rel=\"apple-touch-icon\" href=\"/touch.png\">");

            assertEquals(
                    List.of(
                            "https://example.com/touch.png",
                            "https://example.com/icon-192.png",
                            "https://example.com/icon-16.png",
                            "https://example.com/apple-touch-icon.png"),
                    SiteMetaParser.iconCandidates(doc, HOME));
        }

        @Test
        @DisplayName("shortcut icon도 icon으로 인식한다")
        void shortcut_icon도_icon으로_인식한다() {
            Document doc = html("<link rel=\"shortcut icon\" href=\"/fav.png\">");

            assertEquals(
                    List.of(
                            "https://example.com/fav.png",
                            "https://example.com/apple-touch-icon.png"),
                    SiteMetaParser.iconCandidates(doc, HOME));
        }

        @Test
        @DisplayName("아이콘 태그가 없으면 관례 경로만 돌려준다")
        void 아이콘_태그가_없으면_관례_경로만_돌려준다() {
            assertEquals(
                    List.of("https://example.com/apple-touch-icon.png"),
                    SiteMetaParser.iconCandidates(html(""), HOME));
        }
    }

    private static Document html(String head) {
        return Jsoup.parse("<html><head>" + head + "</head><body></body></html>", HOME);
    }
}
