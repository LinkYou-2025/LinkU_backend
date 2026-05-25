package com.umc.linkyou.service.curation;

import com.umc.linkyou.web.dto.curation.CurationDetailResponse;
import com.umc.linkyou.web.dto.curation.CurationLatestResponse;
import com.umc.linkyou.web.dto.curation.CurationListResponse;
import com.umc.linkyou.web.dto.curation.CurationSectionResponse;

import java.util.List;
import java.util.Optional;

public interface CurationService {

    // 단일 유저의 특정 월 큐레이션 생성
    boolean generateCurationForUser(Long userId, String month);

    // 연도별 12개 큐레이션 히스토리 (없는 달은 빈 상태)
    List<CurationListResponse> getCurationList(Long userId, int year);

    // 유저의 최근 큐레이션 정보를 가져옴
    Optional<CurationLatestResponse> getLatestCuration(Long userId);

    // 월별 섹션 정보 조회 (제목, 설명, 대표 이미지)
    List<CurationSectionResponse> getCurationSections(String month);

    // 유저의 큐레이션 detail 정보를 가져옴
    CurationDetailResponse getCurationDetail(Long userId, Long curationId);
}
