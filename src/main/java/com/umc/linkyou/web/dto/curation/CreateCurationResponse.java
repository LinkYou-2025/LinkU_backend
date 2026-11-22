package com.umc.linkyou.web.dto.curation;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CreateCurationResponse {
    private Long curationId;
    private String month;
    private String thumbnailUrl;
    private LocalDateTime createdAt;
}
