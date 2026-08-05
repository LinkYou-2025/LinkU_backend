package com.umc.linkyou.service.Linku;

import com.umc.linkyou.apiPayload.ApiResponse;
import com.umc.linkyou.apiPayload.code.status.ErrorStatus;
import com.umc.linkyou.apiPayload.code.status.user.UserErrorStatus;
import com.umc.linkyou.apiPayload.exception.GeneralException;
import com.umc.linkyou.config.properties.RecommendScoreProperties;
import com.umc.linkyou.converter.LinkuConverter;
import com.umc.linkyou.domain.Users;
import com.umc.linkyou.domain.classification.Emotion;
import com.umc.linkyou.domain.mapping.LinkuFolder;
import com.umc.linkyou.domain.mapping.SituationJob;
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
 * 홈화면 링크 추천 (curation.recommend.*와는 별개). 스코어링/정렬/페이징 전부 DB(QueryDSL)에서 처리.
 * 스코어 계산은 HomeRecommendScoreService, 쿼리 적용은 UsersLinkuRepositoryImpl 참고.
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

        Emotion selectedEmotion = validateAndFetchContext(userId, situationId, emotionId);

        // API 레이어에서 걸러지지만 서비스 직접 호출 경로(테스트 등)도 방어
        if (size <= 0) {
            return ApiResponse.onSuccess(LinkuResponseDTO.LinkuRecommendCursorPageDTO.builder()
                    .items(Collections.emptyList())
                    .nextCursor(null)
                    .hasNext(false)
                    .build());
        }

        List<Long> mappedCategories = situationCategoryService.getCategoryIdsBySituation(situationId);

        // TextMatch/KeywordMatch용 프로필. 없으면 두 신호는 0 처리
        Optional<UserContentProfile> contentProfile = userContentProfileRepository.findById(userId);
        String profileTsqueryText = contentProfile.map(UserContentProfile::getProfileTsqueryText).orElse(null);
        String profileText = contentProfile.map(UserContentProfile::getProfileText).orElse(null);

        // novelty quota 우선 채우고 나머지는 normal(7축 가중합)로. 두 버킷을 각자 seek 커서로 페이징
        RecommendCursorUtil.RecommendCursor decodedCursor = RecommendCursorUtil.decode(cursor);
        CandidatePage page = fetchNoveltyAndNormalCandidates(
                userId, selectedEmotion.getEmotionId(), situationId, mappedCategories,
                profileTsqueryText, profileText, decodedCursor, size);

        List<LinkuResponseDTO.LinkuSimpleDTO> result = mapCandidatesToDto(page.candidates());

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

    // novelty quota + normal 조회 결과를 합치고 다음 seek 커서/hasNext를 계산한다.
    private CandidatePage fetchNoveltyAndNormalCandidates(
            Long userId, Long selectedEmotionId, Long selectedSituationId, List<Long> mappedCategoryIds,
            String profileTsqueryText, String profileText,
            RecommendCursorUtil.RecommendCursor cursor, int size) {

        RecommendScoreProperties.Novelty noveltyProps = recommendScoreProperties.novelty();
        LocalDateTime now = LocalDateTime.now();

        int quotaCount = (int) Math.round(size * noveltyProps.quotaRatio());
        quotaCount = Math.min(Math.max(quotaCount, 0), size);

        // quota+1개를 가져와 hasMoreNovelty 판별. quota 0이거나 이미 exhausted면 조회 생략
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
        boolean noveltyExhaustedNext = quotaCount == 0 || cursor.noveltyExhausted() || !hasMoreNovelty;

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

        // normal/novelty는 서로소라 중복 없지만 userLinkuId 기준으로 한 번 더 방어
        Set<Long> seen = new LinkedHashSet<>();
        List<RankedUsersLinku> merged = new ArrayList<>(noveltyCandidates.size() + normalCandidates.size());
        for (RankedUsersLinku candidate : noveltyCandidates) {
            if (seen.add(candidate.userLinkuId())) merged.add(candidate);
        }
        for (RankedUsersLinku candidate : normalCandidates) {
            if (seen.add(candidate.userLinkuId())) merged.add(candidate);
        }

        // 다음 seek 지점 = 이번 페이지 마지막 행의 (scoreBucket, userLinkuId). 못 뽑았으면 기존 값 유지
        RankedUsersLinku lastNovelty = noveltyCandidates.isEmpty()
                ? null : noveltyCandidates.get(noveltyCandidates.size() - 1);
        RankedUsersLinku lastNormal = normalCandidates.isEmpty()
                ? null : normalCandidates.get(normalCandidates.size() - 1);

        // 삼항연산자 한쪽이 primitive int(scoreBucket()), 다른 쪽이 Integer(cursor.xxxBucket())이면
        // 자바가 전체 식의 타입을 int로 정해서 null인 Integer 쪽도 강제로 언박싱하다 NPE가 난다
        // (첫 페이지처럼 lastNovelty/lastNormal이 null이라 cursor 쪽 값을 쓰는데, 그 cursor 값 자체도
        // null인 경우 — RecommendCursor.FIRST_PAGE 참고). Integer.valueOf로 양쪽을 박싱 타입으로
        // 맞춰서 언박싱이 안 일어나게 한다.
        RecommendCursorUtil.RecommendCursor nextCursor = new RecommendCursorUtil.RecommendCursor(
                lastNovelty != null ? Integer.valueOf(lastNovelty.scoreBucket()) : cursor.noveltyBucket(),
                lastNovelty != null ? lastNovelty.userLinkuId() : cursor.noveltyLastId(),
                lastNormal != null ? Integer.valueOf(lastNormal.scoreBucket()) : cursor.normalBucket(),
                lastNormal != null ? lastNormal.userLinkuId() : cursor.normalLastId(),
                noveltyExhaustedNext);
        boolean hasNext = !noveltyExhaustedNext || hasMoreNormal;

        return new CandidatePage(merged, nextCursor, hasNext);
    }

    private record CandidatePage(
            List<RankedUsersLinku> candidates, RecommendCursorUtil.RecommendCursor nextCursor, boolean hasNext) {}

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
    private List<LinkuResponseDTO.LinkuSimpleDTO> mapCandidatesToDto(List<RankedUsersLinku> candidates) {
        List<Long> userLinkuIds = candidates.stream()
                .map(RankedUsersLinku::userLinkuId)
                .collect(Collectors.toList());
        Map<Long, LinkuFolder> latestFolderByUserLinkuId = fetchLatestLinkuFolders(userLinkuIds);

        return candidates.stream()
                .map(candidate -> LinkuConverter.toLinkuSimpleDTO(
                        candidate,
                        latestFolderByUserLinkuId.get(candidate.userLinkuId())))
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
