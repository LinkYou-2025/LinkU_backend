package com.umc.linkyou.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@Getter
@Setter
@ConfigurationProperties("jwt.token")
public class JwtProperties {
    //private String secretKey="";
    private Keys keys;
    private String issuer;
    private Expiration expiration;

    @Getter
    @Setter
    public static class Expiration{
        private Long access;
        private Long refresh;
    }

    @Getter
    @Setter
    public static class Keys {
        private String access;
        private String refresh;
    }
}