package com.umc.linkyou.config;

import com.umc.linkyou.config.properties.SesProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sesv2.SesV2Client;

@Configuration
public class SesConfig {

    @Bean
    public SesV2Client sesV2Client(SesProperties sesProperties) {
        return SesV2Client.builder()
                .region(Region.of(sesProperties.getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(
                                sesProperties.getAccessKey(),
                                sesProperties.getSecretKey()
                        )
                ))
                .build();
    }
}
