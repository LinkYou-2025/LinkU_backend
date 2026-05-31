package com.umc.linkyou.service.curation.utils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ThumbnailUrlProvider {

    private final String baseUrl;
    private static final String EXTENSION = ".png";

    public ThumbnailUrlProvider(@Value("${cloud.aws.s3.base-url}") String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getUrlForMonth(String month) {
        String[] parts = month.split("-");
        return String.format("%s/curation/%s/%s%s", baseUrl, parts[0], parts[1], EXTENSION);
    }
}