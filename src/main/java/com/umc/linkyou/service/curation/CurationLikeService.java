package com.umc.linkyou.service.curation;

import com.umc.linkyou.web.dto.curation.CurationListResponse;
import com.umc.linkyou.web.dto.curation.LikedCurationResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CurationLikeService {
    void likeCuration(Long userId, Long curationId);
    void unlikeCuration(Long userId, Long curationId);
    boolean isLiked(Long userId, Long curationId);

    // [기존] 기본 화면용 (최근 6개)
    List<LikedCurationResponse> getRecentLikedCurations(Long userId);

    // 좋아요한 큐레이션 전체 리스트 (페이징 포함)
    Page<CurationListResponse> getLikedCurationList(Long userId, Pageable pageable);
}
