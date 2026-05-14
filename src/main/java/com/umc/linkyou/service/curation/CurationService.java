package com.umc.linkyou.service.curation;

import com.umc.linkyou.web.dto.curation.CurationDetailResponse;
import com.umc.linkyou.web.dto.curation.CurationLatestResponse;
import com.umc.linkyou.web.dto.curation.CurationListResponse;
import com.umc.linkyou.web.dto.curation.CurationSectionResponse;

import java.util.List;
import java.util.Optional;

public interface CurationService {

    CurationDetailResponse getCurationDetail(Long userId, Long curationId);

    // 모든 유저 큐레이션 생성
    void generateMonthlyCurationForAllUsers();
    // 단일 유저의 특정 월 큐레이션 생성
    void generateCurationForUser(Long userId, String month);

    List<CurationSectionResponse> getSectionInfo(String month);

    List<CurationListResponse> getMyCurationList(Long userId, int year);

    Optional<CurationLatestResponse> getLatestCuration(Long userId);
}
