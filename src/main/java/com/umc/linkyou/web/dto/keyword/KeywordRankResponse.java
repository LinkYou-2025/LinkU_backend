package com.umc.linkyou.web.dto.keyword;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class KeywordRankResponse {
    private String name;
    private long count;
}
