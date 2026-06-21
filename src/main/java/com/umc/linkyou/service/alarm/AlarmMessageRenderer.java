package com.umc.linkyou.service.alarm;

import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
public class AlarmMessageRenderer {
    private AlarmMessageRenderer() {}

    public static String render(String template, Map<String, String> values) {
        if (values == null || values.isEmpty()) {
            return template;
        }

        String result = template;
        for (Map.Entry<String, String> entry : values.entrySet()) {
            if (entry.getValue() != null) {
                result = result.replace("{" + entry.getKey() + "}", entry.getValue());
            }
        }

        if (result.matches(".*\\{[^}]+\\}.*")) {
            log.warn("알람 본문에 치환되지 않은 placeholder가 남았습니다: {}", result);
        }

        return result;
    }
}
