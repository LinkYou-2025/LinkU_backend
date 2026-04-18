package com.umc.linkyou.web.dto.curation;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class CurationDetailResponse {
    private Long curationId;
    private String month;
    private List<String> topTags;
    private String headerMent;
    private String footerMent;
    private boolean mentReady;
}