package com.umc.linkyou.service.curation;

import com.umc.linkyou.domain.Curation;
import com.umc.linkyou.web.dto.curation.CreateCurationRequest;
import com.umc.linkyou.web.dto.curation.CurationDetailResponse;
import com.umc.linkyou.web.dto.curation.CurationLatestResponse;

import java.util.Optional;

public interface CurationService {
    Curation createCuration(Long userId, CreateCurationRequest request);
    CurationDetailResponse getCurationDetail(Long curationId);
    void generateMonthlyCurationForAllUsers(); // batch

    /** 2025-02 ~ 2025-07까지 시드 데이터 생성 (이미 존재하면 스킵) */
    void seedFebToJul2025(boolean materializeExternal);

    Optional<CurationLatestResponse> getLatestCuration(Long userId);
}