package com.umc.linkyou.config.common.converter;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import org.springframework.core.convert.converter.Converter;

// "YYYY-MM" 형식의 문자열만 YearMonth로 변환한다.
public class StringToYearMonthConverter implements Converter<String, YearMonth> {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    @Override
    public YearMonth convert(String source) {
        if (source == null || source.isBlank()) {
            return null;
        }
        try {
            return YearMonth.parse(source.trim(), FORMATTER);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("month는 YYYY-MM 형식이어야 합니다. (예: 2026-08)");
        }
    }
}
