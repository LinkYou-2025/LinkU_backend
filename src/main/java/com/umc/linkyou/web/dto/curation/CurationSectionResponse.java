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
public class CurationSectionResponse {
    private int section;
    private String title;
    private String description;
    private String imageUrl;
}
