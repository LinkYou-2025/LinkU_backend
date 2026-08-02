package com.umc.linkyou.converter;

import com.umc.linkyou.awss3.AwsS3Service;
import com.umc.linkyou.domain.Curation;
import com.umc.linkyou.domain.CurationSectionInfo;
import com.umc.linkyou.domain.Linku;
import com.umc.linkyou.domain.classification.Category;
import com.umc.linkyou.domain.classification.Domain;
import com.umc.linkyou.domain.enums.CurationLinkuType;
import com.umc.linkyou.domain.mapping.CurationLinku;
import com.umc.linkyou.domain.mapping.UsersLinku;
import com.umc.linkyou.web.dto.curation.CurationDetailResponse;
import com.umc.linkyou.web.dto.curation.CurationLatestResponse;
import com.umc.linkyou.web.dto.curation.CurationListResponse;
import com.umc.linkyou.web.dto.curation.CurationSectionResponse;
import com.umc.linkyou.web.dto.curation.RecommendedLinkResponse;

import java.util.List;

public class CurationConverter {

    public static RecommendedLinkResponse toRecommendedLinkResponse(CurationLinku entity, AwsS3Service awsS3Service) {
        UsersLinku usersLinku = entity.getUsersLinku();
        Linku linku = usersLinku != null ? usersLinku.getLinku() : null;
        Domain domain = linku != null ? linku.getDomain() : null;
        Category category = linku != null ? linku.getCategory() : null;
        return RecommendedLinkResponse.builder()
                .userLinkuId(usersLinku != null ? usersLinku.getUserLinkuId() : null)
                .url(entity.getUrl())
                .title(entity.getTitle())
                .domain(domain != null ? domain.getName() : null)
                .domainImageUrl(domain != null ? awsS3Service.resolveUrl(domain.getImage()) : null)
                .imageUrl(entity.getImageUrl())
                .categories(category != null ? List.of(category.getCategoryName()) : null)
                .type(CurationLinkuType.INTERNAL)
                .build();
    }

    // 외부 추천: 도메인 브랜딩(Domain)이 URL 기반 별도 조회로 해석된 경우
    public static RecommendedLinkResponse toRecommendedLinkResponse(
            String url, String title, String imageUrl, Domain domain, AwsS3Service awsS3Service) {
        return RecommendedLinkResponse.builder()
                .url(url)
                .title(title)
                .imageUrl(imageUrl)
                .domain(domain != null ? domain.getName() : null)
                .domainImageUrl(domain != null ? awsS3Service.resolveUrl(domain.getImage()) : null)
                .type(CurationLinkuType.EXTERNAL)
                .build();
    }

    // 연도별 히스토리 항목 변환 (해당 월에 큐레이션이 없으면 month만 채운다)
    public static CurationListResponse toCurationListResponse(Curation curation, String month, String thumbnailUrl) {
        if (curation == null) {
            return CurationListResponse.builder().month(month).build();
        }
        return CurationListResponse.builder()
                .curationId(curation.getCurationId())
                .month(month)
                .thumbnailUrl(thumbnailUrl)
                .build();
    }

    public static CurationLatestResponse toCurationLatestResponse(Curation curation, String thumbnailUrl) {
        return CurationLatestResponse.builder()
                .curationId(curation.getCurationId())
                .month(curation.getBaseMonth())
                .thumbnailUrl(thumbnailUrl)
                .build();
    }

    public static CurationSectionResponse toCurationSectionResponse(CurationSectionInfo info) {
        return CurationSectionResponse.builder()
                .section(info.getSectionNumber())
                .title(info.getTitle())
                .description(info.getDescription())
                .imageUrl(info.getImageUrl())
                .build();
    }

    public static CurationDetailResponse toCurationDetailResponse(Curation curation) {
        return CurationDetailResponse.builder()
                .curationId(curation.getCurationId())
                .month(curation.getBaseMonth())
                .headerMent(curation.getHeaderMent())
                .footerMent(curation.getFooterMent())
                .mentReady(curation.getHeaderMent() != null && !curation.getHeaderMent().isBlank()
                        && curation.getFooterMent() != null && !curation.getFooterMent().isBlank())
                .build();
    }
}
