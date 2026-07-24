package com.umc.linkyou.repository.UserLinkuRepository;

import com.umc.linkyou.domain.mapping.UsersLinku;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import java.time.LocalDateTime;
import java.util.List;

public interface UsersLinkuRepositoryCustom {
    // 특정 유저의 링크들을 카테고리별로 조회 (AiArticle 정보 포함)
    List<UsersLinku> fetchAiArticlesByCategoryId(Long userId, Long categoryId);
    List<UsersLinku> findRecentLinkCandidatesByUser(Long userId, int limit);
    List<UsersLinku> fetchAiArticlesByCategoryIdWithCursor(Long userId, Long categoryId, Long cursorId, int limit);

    // 홈화면 링크 추천: EmotionMatch/SituationMatch/PersonalEngagement/Popularity/TextMatch/KeywordMatch를
    // 정규화해 가중합한 뒤 DB에서 정렬/페이징까지 마친 후보를 반환한다. (HomeRecommendScoreService#scoreExpression)
    // profileTsqueryText/profileText는 UserContentProfile에서 미리 조회해서 넘긴다(없으면 null — TextMatch 0 처리).
    List<UsersLinku> findHomeRecommendCandidates(
            Long userId, Long selectedEmotionId, Long selectedSituationId, List<Long> mappedCategoryIds,
            LocalDateTime now, String profileTsqueryText, String profileText, int offset, int limit);

    // UserProfileRefreshWorker용: TextMatch 프로필(title+summary) 재계산 재료로 최근 저장 링크를 캡을 두고 가져온다.
    List<UsersLinku> findRecentContentForProfile(Long userId, int limit);
}