package com.umc.linkyou.config.common;

import com.umc.linkyou.config.common.converter.StringToYearMonthConverter;
import com.umc.linkyou.jwt.CurrentUserArgumentResolver;
import com.umc.linkyou.validation.annotation.ApiAdmin;
import com.umc.linkyou.validation.annotation.ApiManager;
import com.umc.linkyou.validation.annotation.ApiV1;
import com.umc.linkyou.validation.annotation.ApiV2;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final CurrentUserArgumentResolver currentUserArgumentResolver;

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(currentUserArgumentResolver);
    }

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(new StringToYearMonthConverter());
    }

    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        // ApiV1 어노테이션이 있으면 /api/v1/admin 추가
        configurer.addPathPrefix("/api/v1/admin",
                c -> c.isAnnotationPresent(ApiAdmin.class));

        configurer.addPathPrefix("/api/v1/manage",
                c -> c.isAnnotationPresent(ApiManager.class));

        // 2. 그 다음 일반적인 경로 배치
        configurer.addPathPrefix("/api/v1",
                c -> c.isAnnotationPresent(ApiV1.class) && !c.isAnnotationPresent(ApiAdmin.class));

        configurer.addPathPrefix("/api/v2",
                c -> c.isAnnotationPresent(ApiV2.class));
    }
}

