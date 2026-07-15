package com.umc.linkyou.web.dto.curation;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CurationDetailResponse {
    private Long curationId;
    private String month;
    private String headerMent;
    private String footerMent;
    private boolean mentReady;
}