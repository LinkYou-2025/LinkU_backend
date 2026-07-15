package com.umc.linkyou.web.dto.keyword;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class JobKeywordRankResponse {
    private String name;
    private long count;
}
