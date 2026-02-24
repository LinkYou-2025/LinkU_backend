package com.umc.linkyou.config;

import com.umc.linkyou.validation.annotation.ApiV1;
import com.umc.linkyou.validation.annotation.ApiV2;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        configurer.addPathPrefix("/api/v1",
                c -> c.isAnnotationPresent(ApiV1.class));
        configurer.addPathPrefix("/api/v2",
                c -> c.isAnnotationPresent(ApiV2.class));
    }
}

