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

    // 홈화면 추천 원본(OFFSET). 7축 가중합 정렬 — HomeRecommendScoreService#scoreExpression.
    // novelty/normal 분리 없이 전체를 다루던 옛 메서드, LinkuRecommendService는 아래 seek 버전을 쓴다.
    List<UsersLinku> findHomeRecommendCandidates(
            Long userId, Long selectedEmotionId, Long selectedSituationId, List<Long> mappedCategoryIds,
            LocalDateTime now, String profileTsqueryText, String profileText, int offset, int limit);

    // normal 버킷 — 7축 가중합, novelty 대상(최근 안 본 것)은 제외해 서로소 유지.
    // seek(keyset) 방식: after* 둘 다 null이면 처음부터, 아니면 (scoreBucket, userLinkuId) 이전부터.
    // scoreBucket은 HomeRecommendScoreService#scoreBucketExpression 참고.
    List<RankedUsersLinku> findNormalRecommendCandidates(
            Long userId, Long selectedEmotionId, Long selectedSituationId, List<Long> mappedCategoryIds,
            LocalDateTime now, String profileTsqueryText, String profileText,
            int recencyThresholdDays, Integer afterScoreBucket, Long afterUserLinkuId, int limit);

    // novelty 버킷 — EmotionMatch/SituationMatch 2축만 정렬(noveltyContextScoreExpression). seek 방식 동일.
    List<RankedUsersLinku> findNoveltyRecommendCandidates(
            Long userId, Long selectedEmotionId, Long selectedSituationId,
            LocalDateTime now, int recencyThresholdDays, Integer afterScoreBucket, Long afterUserLinkuId, int limit);

    // UserProfileRefreshWorker용: TextMatch 프로필(title+summary) 재계산 재료로 최근 저장 링크를 캡을 두고 가져온다.
    List<UsersLinku> findRecentContentForProfile(Long userId, int limit);
}