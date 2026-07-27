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
    // novelty 버킷 필터링을 하지 않는 원본 메서드다 — LinkuRecommendService는 novelty/normal 버킷을 분리하기 위해
    // findNormalRecommendCandidates를 대신 쓴다 (아래 참고).
    List<UsersLinku> findHomeRecommendCandidates(
            Long userId, Long selectedEmotionId, Long selectedSituationId, List<Long> mappedCategoryIds,
            LocalDateTime now, String profileTsqueryText, String profileText, int offset, int limit);

    // 홈화면 링크 추천 — normal 버킷. findHomeRecommendCandidates와 동일한 7축 가중합 랭킹이지만,
    // HomeRecommendScoreService#noveltyCondition에 해당하는(=최근에 안 본) 후보를 제외한다.
    // findNoveltyRecommendCandidates가 뽑아가는 후보와 서로소를 유지해서 두 버킷을 합쳤을 때 중복이 없게 한다.
    List<UsersLinku> findNormalRecommendCandidates(
            Long userId, Long selectedEmotionId, Long selectedSituationId, List<Long> mappedCategoryIds,
            LocalDateTime now, String profileTsqueryText, String profileText,
            int recencyThresholdDays, int offset, int limit);

    // 홈화면 링크 추천 — novelty(최근에 안 본 것) 버킷. 7축 가중합이 아니라 EmotionMatch/SituationMatch
    // 두 축(HomeRecommendScoreService#noveltyContextScoreExpression)만으로 정렬한다.
    List<UsersLinku> findNoveltyRecommendCandidates(
            Long userId, Long selectedEmotionId, Long selectedSituationId,
            LocalDateTime now, int recencyThresholdDays, int offset, int limit);

    // UserProfileRefreshWorker용: TextMatch 프로필(title+summary) 재계산 재료로 최근 저장 링크를 캡을 두고 가져온다.
    List<UsersLinku> findRecentContentForProfile(Long userId, int limit);
}