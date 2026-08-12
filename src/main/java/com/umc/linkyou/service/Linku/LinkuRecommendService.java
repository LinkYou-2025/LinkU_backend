package com.umc.linkyou.service.Linku;

import com.umc.linkyou.apiPayload.ApiResponse;
import com.umc.linkyou.apiPayload.code.status.ErrorStatus;
import com.umc.linkyou.apiPayload.code.status.user.UserErrorStatus;
import com.umc.linkyou.apiPayload.exception.GeneralException;
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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

        // 7축 가중합 단일 랭킹 — PersonalEngagement의 staleness 항이 오래 안 본/안 만든 후보를 자연히
        // 끌어올려주므로 별도 novelty 버킷 없이 seek 커서 하나로 페이징한다.
        RecommendCursorUtil.RecommendCursor decodedCursor = RecommendCursorUtil.decode(cursor);
        CandidatePage page = fetchRecommendCandidates(
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

    // 7축 가중합 랭킹을 seek 커서로 한 페이지 조회하고 다음 커서/hasNext를 계산한다.
    private CandidatePage fetchRecommendCandidates(
            Long userId, Long selectedEmotionId, Long selectedSituationId, List<Long> mappedCategoryIds,
            String profileTsqueryText, String profileText,
            RecommendCursorUtil.RecommendCursor cursor, int size) {

        LocalDateTime now = LocalDateTime.now();

        // size+1개를 가져와 hasNext 판별
        List<RankedUsersLinku> fetched = usersLinkuRepository.findNormalRecommendCandidates(
                userId, selectedEmotionId, selectedSituationId, mappedCategoryIds,
                now, profileTsqueryText, profileText,
                cursor.scoreBucket(), cursor.lastId(), size + 1);
        boolean hasNext = fetched.size() > size;
        List<RankedUsersLinku> candidates = hasNext ? fetched.subList(0, size) : fetched;

        // 다음 seek 지점 = 이번 페이지 마지막 행의 (scoreBucket, userLinkuId). 못 뽑았으면 기존 커서 유지
        RankedUsersLinku last = candidates.isEmpty() ? null : candidates.get(candidates.size() - 1);
        RecommendCursorUtil.RecommendCursor nextCursor = last != null
                ? new RecommendCursorUtil.RecommendCursor(last.scoreBucket(), last.userLinkuId())
                : cursor;

        return new CandidatePage(candidates, nextCursor, hasNext);
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
