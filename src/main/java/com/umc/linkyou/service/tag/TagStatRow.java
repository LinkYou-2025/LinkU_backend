package com.umc.linkyou.service.tag;

import com.umc.linkyou.domain.enums.KeywordType;

public record TagStatRow(KeywordType type, Long refId, Long totalCount) {}
