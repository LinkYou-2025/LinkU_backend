package com.umc.linkyou.web.dto.folder.linku;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
// 중폴더 내부에 있는 링크 응답 형식
public class LinkuSummaryDTO {
    private Long linkuId;
    private String title;
    private String url;
    private String keyword;
    private Long userLinkuId;
    private String linkuImageUrl;
    private Long emotionId;
    private Long categoryId;
    private Long situationId;
    private String createdAt;
    private String domainImageUrl;
    private String domainName;
}
