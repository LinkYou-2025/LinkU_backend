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
public class CurationLatestResponse {
    private Long curationId;
    private String month; // e.g., "2025-07"
    private String thumbnailUrl;
}