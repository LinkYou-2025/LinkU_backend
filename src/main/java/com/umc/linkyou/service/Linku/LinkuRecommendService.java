package com.umc.linkyou.service.Linku;

import com.umc.linkyou.apiPayload.ApiResponse;
import com.umc.linkyou.apiPayload.code.status.ErrorStatus;
import com.umc.linkyou.apiPayload.code.status.user.UserErrorStatus;
import com.umc.linkyou.apiPayload.exception.GeneralException;
import com.umc.linkyou.config.properties.RecommendScoreProperties;
import com.umc.linkyou.converter.LinkuConverter;
import com.umc.linkyou.domain.Linku;
import com.umc.linkyou.domain.Users;
import com.umc.linkyou.domain.classification.Emotion;
import com.umc.linkyou.domain.mapping.LinkuFolder;
import com.umc.linkyou.domain.mapping.SituationJob;
import com.umc.linkyou.domain.mapping.UsersLinku;
import com.umc.linkyou.domain.recommend.UserContentProfile;
import com.umc.linkyou.repository.EmotionRepository;
import com.umc.linkyou.repository.classification.SituationRepository;
import com.umc.linkyou.repository.mapping.SituationJobRepository;
import com.umc.linkyou.repository.mapping.linkuFolderRepository.LinkuFolderRepository;
import com.umc.linkyou.repository.UserLinkuRepository.UsersLinkuRepository;
import com.umc.linkyou.repository.dto.RankedUsersLinku;
import com.umc.linkyou.repository.recommend.UserContentProfileRepository;
import com.umc.linkyou.repository.userRepository.UserRepository;
import com.umc.linkyou.utils.RecommendCursorUtil;
import com.umc.linkyou.web.dto.linku.LinkuResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 홈화면 링크 추천.
 * 큐레이션 내부/외부 추천(service.curation.recommend.*)과는 별개의 독립된 로직이다.
 *
 * 점수 계산·정렬·페이징을 전부 DB(QueryDSL)에서 처리한다.
 * - EmotionMatch/SituationMatch/PersonalEngagement/Popularity/TextMatch/KeywordMatch 가중합(w^T x):
 *   service.common.HomeRecommendScoreService
 * - 위 스코어를 CASE WHEN/템플릿 식으로 변환해 쿼리에 적용: UsersLinkuRepositoryImpl#findHomeRecommendCandidates
 * - 정렬 + LIMIT/OFFSET: 같은 쿼리에서 처리 (애플리케이션 메모리로 전체 링크를 올리지 않음)
 * - TextMatch/KeywordMatch에 쓰이는 유저 프로필(UserContentProfile)은 UserProfileRefreshWorker가
 *   비동기로 미리 계산해둔 값을 여기서 단건 조회만 한다 (없으면 null → 두 신호는 0 처리).
 */
@Service
@RequiredArgsConstructor
public class LinkuRecommendService {

    private final EmotionRepository emotionRepository;
    private final UsersLinkuRepository usersLinkuRepository;
    private final UserRepository userRepository;
    private final SituationRepository situationRepository;
    private final SituationJobRepository situationJobRepository;
    private final LinkuViewService linkuViewService;
    private final LinkuFolderRepository linkuFolderRepository;
    private final SituationCategoryService situationCategoryService;
    private final UserContentProfileRepository userContentProfileRepository;
    private final RecommendScoreProperties recommendScoreProperties;

    @Transactional(readOnly = true)
    public ApiResponse<LinkuResponseDTO.LinkuRecommendCursorPageDTO> recommendLinku(
            Long userId, Long situationId, Long emotionId, String cursor, int size) {

        // 1. 필수 엔티티 조회 및 검사 (링크 목록은 이 단계에서 로드하지 않는다)
        Emotion selectedEmotion = validateAndFetchContext(userId, situationId, emotionId);

        // size는 API 레이어(@Min(1) @Max(20))에서 걸러지지만, 서비스를 직접 호출하는 다른 경로(테스트 등)
        // 까지 방어하기 위해 한 번 더 가드한다 (LinkuSearchService#search와 동일한 패턴).
        if (size <= 0) {
            return ApiResponse.onSuccess(LinkuResponseDTO.LinkuRecommendCursorPageDTO.builder()
                    .items(Collections.emptyList())
                    .nextCursor(null)
                    .hasNext(false)
                    .build());
        }

        // 2. 상황에 매핑된 카테고리 조회
        List<Long> mappedCategories = situationCategoryService.getCategoryIdsBySituation(situationId);

        // 2-1. TextMatch/KeywordMatch용 유저 콘텐츠 프로필 조회 (없으면 두 신호는 0으로 처리됨)
        Optional<UserContentProfile> contentProfile = userContentProfileRepository.findById(userId);
        String profileTsqueryText = contentProfile.map(UserContentProfile::getProfileTsqueryText).orElse(null);
        String profileText = contentProfile.map(UserContentProfile::getProfileText).orElse(null);

        // 3. novelty(최근에 안 본 것) quota만큼 먼저 뽑고, 나머지를 기존 7축 가중합(normal)으로 채운다.
        //    quota는 목표치일 뿐이라 novelty 후보가 모자라면 normal이 초과해서 채운다(borrow).
        //    두 버킷은 서로 다른 속도로 소진되므로, 각 버킷의 seek(keyset) 탐색 지점을 담은 커서
        //    (noveltyBucket/noveltyLastId, normalBucket/normalLastId, noveltyExhausted)로 페이징한다
        //    (docs/home-recommend-cursor-api-spec.md 참고).
        RecommendCursorUtil.RecommendCursor decodedCursor = RecommendCursorUtil.decode(cursor);
        CandidatePage page = fetchNoveltyAndNormalCandidates(
                userId, selectedEmotion.getEmotionId(), situationId, mappedCategories,
                profileTsqueryText, profileText, decodedCursor, size);

        // 4. DTO 변환 (결과가 없어도 items=[]/hasNext=false로 정상 응답한다 — 에러 아님)
        List<LinkuResponseDTO.LinkuSimpleDTO> result = mapCandidatesToDto(page.candidates());

        // 5. keyword count 집계 — 실제로 추천한 게 있을 때만 기록한다
        if (!page.candidates().isEmpty()) {
            linkuViewService.recordRecommendKeywordCount(userId, emotionId, situationId);
        }

        String nextCursor = page.hasNext() ? RecommendCursorUtil.encode(page.nextCursor()) : null;

        return ApiResponse.onSuccess(LinkuResponseDTO.LinkuRecommendCursorPageDTO.builder()
                .items(result)
                .nextCursor(nextCursor)
                .hasNext(page.hasNext())
                .build());
    }

    // novelty(situation/emotion만으로 정렬) quota + normal(7축 가중합) 조회 결과를 합치고, 다음 커서/hasNext를 계산한다.
    // 두 리포지토리 메서드 모두 OFFSET이 아니라 seek(keyset) 방식이라, 이번 페이지에서 실제로 반환한 마지막
    // 행의 (scoreBucket, userLinkuId)를 다음 커서로 그대로 들고 간다 (HomeRecommendScoreService#scoreBucketExpression 참고).
    private CandidatePage fetchNoveltyAndNormalCandidates(
            Long userId, Long selectedEmotionId, Long selectedSituationId, List<Long> mappedCategoryIds,
            String profileTsqueryText, String profileText,
            RecommendCursorUtil.RecommendCursor cursor, int size) {

        RecommendScoreProperties.Novelty noveltyProps = recommendScoreProperties.novelty();
        LocalDateTime now = LocalDateTime.now();

        int quotaCount = (int) Math.round(size * noveltyProps.quotaRatio());
        quotaCount = Math.min(Math.max(quotaCount, 0), size);

        // quotaCount+1개를 가져와서, 이번 페이지가 소비하는 quotaCount개를 넘는 novelty가 더 남아있는지
        // (hasMoreNovelty) 함께 판별한다. quota가 0이거나 커서에 이미 exhausted로 표시돼 있으면 조회 자체를 생략한다.
        boolean skipNoveltyQuery = quotaCount == 0 || cursor.noveltyExhausted();
        List<RankedUsersLinku> noveltyFetched = skipNoveltyQuery
                ? Collections.emptyList()
                : usersLinkuRepository.findNoveltyRecommendCandidates(
                        userId, selectedEmotionId, selectedSituationId,
                        now, noveltyProps.recencyThresholdDays(),
                        cursor.noveltyBucket(), cursor.noveltyLastId(), quotaCount + 1);
        boolean hasMoreNovelty = noveltyFetched.size() > quotaCount;
        List<RankedUsersLinku> noveltyCandidates =
                hasMoreNovelty ? noveltyFetched.subList(0, quotaCount) : noveltyFetched;
        // quota가 0이거나, 이미 exhausted였거나, 이번에 뽑고 나서 더 남은 게 없으면 다음 커서는 exhausted로 확정한다.
        boolean noveltyExhaustedNext = quotaCount == 0 || cursor.noveltyExhausted() || !hasMoreNovelty;

        // normal도 동일하게 target+1개를 가져와서 hasMoreNormal을 판별한다.
        int normalTarget = size - noveltyCandidates.size();
        List<RankedUsersLinku> normalFetched = normalTarget == 0
                ? Collections.emptyList()
                : usersLinkuRepository.findNormalRecommendCandidates(
                        userId, selectedEmotionId, selectedSituationId, mappedCategoryIds,
                        now, profileTsqueryText, profileText,
                        noveltyProps.recencyThresholdDays(),
                        cursor.normalBucket(), cursor.normalLastId(), normalTarget + 1);
        boolean hasMoreNormal = normalFetched.size() > normalTarget;
        List<RankedUsersLinku> normalCandidates =
                hasMoreNormal ? normalFetched.subList(0, normalTarget) : normalFetched;

        // findNormalRecommendCandidates가 이미 novelty 조건을 제외하므로(서로소) 중복이 생길 수 없지만,
        // 안전하게 userLinkuId 기준으로 한 번 더 방어한다.
        Set<Long> seen = new LinkedHashSet<>();
        List<UsersLinku> merged = new ArrayList<>(noveltyCandidates.size() + normalCandidates.size());
        for (RankedUsersLinku candidate : noveltyCandidates) {
            if (seen.add(candidate.usersLinku().getUserLinkuId())) merged.add(candidate.usersLinku());
        }
        for (RankedUsersLinku candidate : normalCandidates) {
            if (seen.add(candidate.usersLinku().getUserLinkuId())) merged.add(candidate.usersLinku());
        }

        // 다음 seek 지점 = 이번 페이지에서 실제로 반환한 마지막 행의 (scoreBucket, userLinkuId).
        // 이번에 그 버킷에서 아무 것도 못 뽑았으면(스킵/소진) 커서의 기존 값을 그대로 유지한다.
        RankedUsersLinku lastNovelty = noveltyCandidates.isEmpty()
                ? null : noveltyCandidates.get(noveltyCandidates.size() - 1);
        RankedUsersLinku lastNormal = normalCandidates.isEmpty()
                ? null : normalCandidates.get(normalCandidates.size() - 1);

        RecommendCursorUtil.RecommendCursor nextCursor = new RecommendCursorUtil.RecommendCursor(
                lastNovelty != null ? lastNovelty.scoreBucket() : cursor.noveltyBucket(),
                lastNovelty != null ? lastNovelty.usersLinku().getUserLinkuId() : cursor.noveltyLastId(),
                lastNormal != null ? lastNormal.scoreBucket() : cursor.normalBucket(),
                lastNormal != null ? lastNormal.usersLinku().getUserLinkuId() : cursor.normalLastId(),
                noveltyExhaustedNext);
        boolean hasNext = !noveltyExhaustedNext || hasMoreNormal;

        return new CandidatePage(merged, nextCursor, hasNext);
    }

    private record CandidatePage(
            List<UsersLinku> candidates, RecommendCursorUtil.RecommendCursor nextCursor, boolean hasNext) {}

    // 1. 필수 엔티티 조회 및 입력 검증 (링크 개수는 count 쿼리로만 확인, 전체 로드하지 않음)
    private Emotion validateAndFetchContext(Long userId, Long situationId, Long emotionId) {
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(UserErrorStatus._USER_NOT_FOUND));
        Emotion selectedEmotion = emotionRepository.findById(emotionId)
                .orElseThrow(() -> new GeneralException(ErrorStatus._EMOTION_NOT_FOUND));
        situationRepository.findById(situationId)
                .orElseThrow(() -> new GeneralException(ErrorStatus._SITUATION_NOT_FOUND));

        if (user.getJob() == null) {
            throw new GeneralException(UserErrorStatus._JOB_NOT_SET);
        }
        Long jobId = user.getJob().getId();
        SituationJob situationJob = situationJobRepository.findBySituation_IdAndJob_Id(situationId, jobId)
                .orElseThrow(() -> new GeneralException(ErrorStatus._SITUATION_NOT_FOUND));

        long savedLinkuCount = usersLinkuRepository.countByUser_Id(userId);
        if (savedLinkuCount == 0)
            throw new GeneralException(ErrorStatus._RECOMMEND_LINKU_NEW_USER);
        if (savedLinkuCount < 3)
            throw new GeneralException(ErrorStatus._RECOMMEND_LINKU_NOT_ENOUGH_LINKS);

        return selectedEmotion;
    }

    // 2. DTO 변환 (AiArticle 존재 여부는 UsersLinku.aiExist 플래그를 그대로 사용 — 별도 조회 없음)
    private List<LinkuResponseDTO.LinkuSimpleDTO> mapCandidatesToDto(List<UsersLinku> candidates) {
        List<Long> userLinkuIds = candidates.stream()
                .map(UsersLinku::getUserLinkuId)
                .collect(Collectors.toList());
        Map<Long, LinkuFolder> latestFolderByUserLinkuId = fetchLatestLinkuFolders(userLinkuIds);

        return candidates.stream()
                .map(userLinku -> {
                    Linku linku = userLinku.getLinku();
                    boolean aiArticleExists = Boolean.TRUE.equals(userLinku.getAiExist());
                    LinkuFolder linkuFolder = latestFolderByUserLinkuId.get(userLinku.getUserLinkuId());

                    return LinkuConverter.toLinkuSimpleDTO(
                            linku,
                            userLinku,
                            linku.getDomain(),
                            aiArticleExists,
                            linkuFolder
                    );
                })
                .collect(Collectors.toList());
    }

    // 여러 UsersLinku의 최신 LinkuFolder를 한 번에 조회한다.
    // linkuFolderId desc로 조회되므로, 같은 userLinkuId가 여러 번 나와도 먼저 만난(가장 큰 id = 최신) 것을 유지한다.
    private Map<Long, LinkuFolder> fetchLatestLinkuFolders(List<Long> userLinkuIds) {
        if (userLinkuIds.isEmpty()) return Map.of();
        return linkuFolderRepository.findByUsersLinku_UserLinkuIdIn(userLinkuIds).stream()
                .collect(Collectors.toMap(
                        lf -> lf.getUsersLinku().getUserLinkuId(),
                        lf -> lf,
                        (existing, replacement) -> existing
                ));
    }
}
