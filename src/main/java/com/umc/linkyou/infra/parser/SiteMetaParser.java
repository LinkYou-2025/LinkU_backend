package com.umc.linkyou.infra.parser;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

// 앱은 아이콘을 24dp로 쓰지만 고밀도 화면에서는 48~72px로 그려져 큰 아이콘을 먼저 고른다.
public final class SiteMetaParser {

    private SiteMetaParser() {}

    // /favicon.ico는 대부분 16~32px이라 화질이 낮고 앱에서 렌더링을 보장할 수 없어 넣지 않는다.
    public static List<String> iconCandidates(Document doc, String homeUrl) {
        List<Element> touchIcons = new ArrayList<>();
        List<Element> icons = new ArrayList<>();
        for (Element link : doc.select("link[rel][href]")) {
            Set<String> rels =
                    Set.of(link.attr("rel").toLowerCase(Locale.ROOT).trim().split("\\s+"));
            if (rels.contains("apple-touch-icon")
                    || rels.contains("apple-touch-icon-precomposed")) {
                touchIcons.add(link);
            } else if (rels.contains("icon")) {
                icons.add(link);
            }
        }
        Comparator<Element> bySizeDesc =
                Comparator.comparingInt(SiteMetaParser::largestSize).reversed();
        touchIcons.sort(bySizeDesc);
        icons.sort(bySizeDesc);

        Set<String> candidates = new LinkedHashSet<>();
        touchIcons.forEach(link -> addIfHttp(candidates, link.absUrl("href")));
        icons.forEach(link -> addIfHttp(candidates, link.absUrl("href")));
        addIfHttp(candidates, stripTrailingSlash(homeUrl) + "/apple-touch-icon.png");
        return List.copyOf(candidates);
    }

    // "any"는 가장 큰 값으로 본다. sizes가 없으면 0이라 문서 순서가 유지된다.
    static int largestSize(Element link) {
        int max = 0;
        for (String size : link.attr("sizes").toLowerCase(Locale.ROOT).trim().split("\\s+")) {
            if (size.equals("any")) {
                return Integer.MAX_VALUE;
            }
            int x = size.indexOf('x');
            if (x <= 0) {
                continue;
            }
            try {
                max = Math.max(max, Integer.parseInt(size.substring(0, x)));
            } catch (NumberFormatException ignored) {
                // 잘못된 sizes 값은 무시
            }
        }
        return max;
    }

    private static void addIfHttp(Set<String> candidates, String url) {
        String lower = url.toLowerCase(Locale.ROOT);
        if (lower.startsWith("https://") || lower.startsWith("http://")) {
            candidates.add(url);
        }
    }

    private static String stripTrailingSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
