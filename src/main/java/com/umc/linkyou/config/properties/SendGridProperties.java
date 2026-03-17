package com.umc.linkyou.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@Getter
@Setter
@ConfigurationProperties(prefix = "spring.sendgrid")
public class SendGridProperties {
    private String apiKey;
    private String from;
    private Templates templates = new Templates();

    @Getter @Setter
    public static class Templates {
        private String verifyId; // spring.sendgrid.templates.verify-id 와 매핑
        private String tempId;   // spring.sendgrid.templates.temp-id 와 매핑
    }
}
