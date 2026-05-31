package com.umc.linkyou.infra.ai.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

@Getter
@Builder
@Jacksonized
public class ExternalLinkResultDTO {
    private final String title;
    private final String url;
}
