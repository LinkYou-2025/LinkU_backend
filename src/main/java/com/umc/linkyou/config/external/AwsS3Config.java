package com.umc.linkyou.config.external;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.umc.linkyou.config.properties.AwsProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
//awss3 폴더에 연관 파일들 적혀있음
@Slf4j
@Configuration
@RequiredArgsConstructor
public class AwsS3Config {

    private final AwsProperties awsProperties;

    @Bean
    public AmazonS3Client amazonS3Client() {
        BasicAWSCredentials awsCreds = new BasicAWSCredentials(
                awsProperties.getCredentials().getAccessKey(),
                awsProperties.getCredentials().getSecretKey()
        );
        return (AmazonS3Client) AmazonS3ClientBuilder.standard()
                .withRegion(awsProperties.getRegion().getStaticRegion())
                .withCredentials(new AWSStaticCredentialsProvider(awsCreds))
                .build();
    }
}

