package com.umc.linkyou.repository.UserLinkuRepository;

import com.umc.linkyou.domain.mapping.UsersLinku;
import com.umc.linkyou.repository.dto.RankedUsersLinku;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import java.time.LocalDateTime;
import java.util.List;

public interface UsersLinkuRepositoryCustom {
    // 특정 유저의 링크들을 카테고리별로 조회 (AiArticle 정보 포함)
    List<UsersLinku> fetchAiArticlesByCategoryId(Long userId, Long categoryId);
    List<UsersLinku> findRecentLinkCandidatesByUser(Long userId, int limit);
    // categoryId가 null이면 카테고리 필터 없이 전체 카테고리를 조회한다 ("전체" 탭)
    List<UsersLinku> fetchAiArticlesByCategoryIdWithCursor(Long userId, Long categoryId, Long cursorId, int limit);

    // 7축 가중합 랭킹 (novelty 버킷 없이 단일 랭킹) — PersonalEngagement가 staleness(오래 안 본/안 만든
    // 정도)를 포함하므로 오래된 후보도 이 랭킹 안에서 자연히 떠오른다.
    // seek(keyset) 방식: after* 둘 다 null이면 처음부터, 아니면 (scoreBucket, userLinkuId) 이전부터.
    List<RankedUsersLinku> findNormalRecommendCandidates(
            Long userId, Long selectedEmotionId, Long selectedSituationId, List<Long> mappedCategoryIds,
            LocalDateTime now, String profileTsqueryText, String profileText,
            Integer afterScoreBucket, Long afterUserLinkuId, int limit);

    // UserProfileRefreshWorker용: TextMatch 프로필(title+summary) 재계산 재료로 최근 저장 링크를 캡을 두고 가져온다.
    List<UsersLinku> findRecentContentForProfile(Long userId, int limit);
}