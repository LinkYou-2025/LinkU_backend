package com.umc.linkyou.service.curation;

import com.umc.linkyou.domain.Curation;
import com.umc.linkyou.web.dto.curation.CurationTopLogDTO;

import java.util.List;

public interface CurationTopLogService {
    // 1. 큐레이션 ID로 태그 이름만 (Top 3) 가져오기
    List<String> getTopTagNamesByCuration(Long curationId);

    // 2. [수정된 핵심 로직] 감정/상황 태그 점수 계산 및 필터링 후 DB 저장
    void calculateAndSaveTopLogs(Long userId, Curation curation);

    // 3. 큐레이션 ID로 태그 DTO (Top 3) 가져오기
    List<CurationTopLogDTO> getTopLogDtoByCuration(Long curationId);
}