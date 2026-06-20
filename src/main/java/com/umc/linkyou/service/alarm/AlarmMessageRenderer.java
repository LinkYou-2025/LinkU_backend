package com.umc.linkyou.service.alarm;

import java.util.Map;

public class AlarmMessageRenderer {
    private AlarmMessageRenderer() {}

    public static String render(String template, Map<String, String> values) {
        if (values == null || values.isEmpty()) {
            return template;
        }

        String result = template;
        for (Map.Entry<String, String> entry : values.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}",
                    entry.getValue());
        }

        return result;
    }
}
