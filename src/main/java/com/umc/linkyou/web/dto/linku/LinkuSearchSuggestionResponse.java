package com.umc.linkyou.web.dto.linku;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class LinkuSearchSuggestionResponse {
    private String title;          // 링크 제목
    private String domainImageUrl; // 도메인 로고 URL
    private String linkUrl;        // 실제 접속 URL
}