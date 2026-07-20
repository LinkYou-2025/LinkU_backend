package com.umc.linkyou.web.dto.tag;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MyTagRankResponse {
    private String name;
    private int percent;
}
