package com.umc.linkyou.web.dto.folder.linku;

import com.umc.linkyou.domain.classification.Domain;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
// 중폴더 내부에 있는 링크 응답 형식
public class LinkuSummaryDTO {
    private Long linkuId;
    private String title;
    private String url;
    private String keyword;
    private Long userLinkuId;
    private String linkuImageUrl;
    private String createdAt;
}
