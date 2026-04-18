package com.umc.linkyou.web.dto.curation;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CurationSectionResponse {
    private int section;
    private String title;
    private String description;
    private String imageUrl;
}
