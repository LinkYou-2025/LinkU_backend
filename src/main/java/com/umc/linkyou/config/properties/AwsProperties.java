package com.umc.linkyou.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("cloud.aws")
public record AwsProperties(
        Region region,
        S3 s3,
        Cloudfront cloudfront
) {
    public record Region(String staticRegion) {}
    public record S3(String bucket) {}
    public record Cloudfront(String domain) {}
}
