package com.umc.linkyou.web.dto.curation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CurationDetailResponse {
    private Long curationId;
    private String month;
    private String headerMent;
    private String footerMent;
    private boolean mentReady;
}