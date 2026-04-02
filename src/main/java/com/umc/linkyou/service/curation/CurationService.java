package com.umc.linkyou.service.curation;

import com.umc.linkyou.domain.Curation;
import com.umc.linkyou.web.dto.curation.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface CurationService {
    Curation createCuration(Long userId, CreateCurationRequest request);
    CurationDetailResponse getCurationDetail(Long curationId);
    void generateMonthlyCurationForAllUsers(); // batch

    /** 2025-02 ~ 2025-07까지 시드 데이터 생성 (이미 존재하면 스킵) */
    void seedFebToJul2025(boolean materializeExternal);

    Optional<CurationLatestResponse> getLatestCuration(Long userId);

    Page<CurationListResponse> getMyCurationList(Long userId, Pageable pageable);

    List<CurationAnalyticsDTO.KeywordCountResponse> getMonthlyTopKeywords(Long userId);
    List<CurationAnalyticsDTO.KeywordLinkResponse> getLinksByKeyword(Long userId, String keyword);
    List<CurationAnalyticsDTO.UnreadLinkResponse> getLastMonthUnreadLinks(Long userId);
}