package com.umc.linkyou.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@Getter
@Setter
@ConfigurationProperties("cloud.aws")
public class AwsProperties {
    private Credentials credentials = new Credentials();
    private Region region = new Region();  // 초기화 필수!
    private S3 s3 = new S3();
    private Cloudfront cloudfront = new Cloudfront();

    @Getter @Setter public static class Credentials {
        private String accessKey;
        private String secretKey;
    }

    @Getter @Setter public static class Region {
        private String staticRegion;  // static 예약어 피함
    }

    @Getter @Setter public static class S3 {
        private String bucket;
    }

    @Getter @Setter public static class Cloudfront {
        private String domain;
    }
}
