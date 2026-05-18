package com.umc.linkyou.infra.ai.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ExternalLinkDTO {
    private final String title;
    private final String url;
}
