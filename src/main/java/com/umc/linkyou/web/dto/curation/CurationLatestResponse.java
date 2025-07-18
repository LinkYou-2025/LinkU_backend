package com.umc.linkyou.web.dto.curation;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CurationLatestResponse {
    private Long curationId;
    private String month; // e.g., "2025-07"
    private String thumbnailUrl;
}