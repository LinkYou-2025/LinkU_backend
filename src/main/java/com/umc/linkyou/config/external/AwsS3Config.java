package com.umc.linkyou.config.external;

import com.umc.linkyou.config.properties.AwsProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

//awss3 폴더에 연관 파일들 적혀있음
@Slf4j
@Configuration
@RequiredArgsConstructor
public class AwsS3Config {

    private final AwsProperties awsProperties;

    @Bean
    public S3Client s3Client() {
        return S3Client.builder()
                .region(Region.of(awsProperties.region().staticRegion()))
                .build(); // EC2 IAM Role 자동 인증
    }
}

