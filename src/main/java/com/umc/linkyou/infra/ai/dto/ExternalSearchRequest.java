package com.umc.linkyou.infra.ai.dto;

import java.util.List;

public record ExternalSearchRequest(
        List<String> tagNames,
        int limit,
        String jobName,
        String gender
) {}
