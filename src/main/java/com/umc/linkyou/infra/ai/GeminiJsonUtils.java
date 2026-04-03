package com.umc.linkyou.infra.ai;

// Gemini 응답 JSON 추출 공통 유틸
public final class GeminiJsonUtils {

    private GeminiJsonUtils() {}

    public static String extractJson(String rawContent) {
        return extract(rawContent, '{', '}');
    }

    public static String extractJsonArray(String rawContent) {
        return extract(rawContent, '[', ']');
    }

    private static String extract(String rawContent, char open, char close) {
        if (rawContent == null || rawContent.isBlank()) {
            return null;
        }

        String content = rawContent.trim();

        // 마크다운 펜스 제거
        if (content.contains("```")) {
            content = content.replaceAll("(?m)^```.*$", "").trim();
        }

        int startIndex = content.indexOf(open);
        int endIndex   = content.lastIndexOf(close);

        if (startIndex == -1 || endIndex == -1 || startIndex >= endIndex) {
            return null;
        }

        return content.substring(startIndex, endIndex + 1).trim();
    }
}
