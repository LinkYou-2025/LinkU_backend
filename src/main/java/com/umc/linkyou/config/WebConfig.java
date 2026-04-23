package com.umc.linkyou.config;

import com.umc.linkyou.validation.annotation.ApiAdmin;
import com.umc.linkyou.validation.annotation.ApiManager;
import com.umc.linkyou.validation.annotation.ApiV1;
import com.umc.linkyou.validation.annotation.ApiV2;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        // ApiV1 어노테이션이 있으면 /api/v1 추가
        configurer.addPathPrefix("/api/v1",
                c -> c.isAnnotationPresent(ApiV1.class));
        configurer.addPathPrefix("/api/v2",
                c -> c.isAnnotationPresent(ApiV2.class));
        configurer.addPathPrefix("/api/v1/admin",
                c -> c.isAnnotationPresent(ApiAdmin.class));
        // 매니저 전용 접두사 추가
        configurer.addPathPrefix("/api/v1/manage",
                c -> c.isAnnotationPresent(ApiManager.class));
    }
}

