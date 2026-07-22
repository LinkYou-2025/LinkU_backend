package com.umc.linkyou.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("cloud.aws.ses")
public record SesProperties(
        String from,
        String accessKey,
        String secretKey,
        String region
) {}
