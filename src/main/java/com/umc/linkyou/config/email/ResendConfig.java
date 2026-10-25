package com.umc.linkyou.config.email;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.resend.Resend;
import com.umc.linkyou.config.properties.ResendProperties;

@Configuration
public class ResendConfig {

    @Bean
    public Resend resend(ResendProperties resendProperties) {
        return new Resend(resendProperties.apiKey());
    }
}
