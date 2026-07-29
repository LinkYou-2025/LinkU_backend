package com.umc.linkyou.web.dto.curation;

import com.umc.linkyou.domain.enums.CurationLinkuType;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder(toBuilder = true)
public class RecommendedLinkResponse {
    private Long userLinkuId;
    private String title;
    private String url;
    private String imageUrl; // 실제 콘텐츠 대표 이미지
    private String domain;
    private String domainImageUrl; // ex. 도메인 아이콘 이미지
    private List<String> categories; // 내부 추천만 포함됨
    private CurationLinkuType type; // INTERNAL / EXTERNAL
}