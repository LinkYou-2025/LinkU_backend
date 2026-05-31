package com.umc.linkyou.service.keyword;

import com.umc.linkyou.domain.enums.KeywordType;

public record KeywordStatRow(KeywordType type, Long refId, Long totalCount) {}
