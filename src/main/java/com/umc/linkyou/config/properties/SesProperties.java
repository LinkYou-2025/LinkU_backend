package com.umc.linkyou.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@Getter
@Setter
@ConfigurationProperties(prefix = "cloud.aws.ses")
public class SesProperties {
    private String from;
    private String accessKey;
    private String secretKey;
    private String region;
}
