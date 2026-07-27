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
import com.umc.linkyou.repository.recommend.UserContentProfileRepository;
import com.umc.linkyou.repository.userRepository.UserRepository;
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
    public ApiResponse<List<LinkuResponseDTO.LinkuSimpleDTO>> recommendLinku(
            Long userId, Long situationId, Long emotionId, int page, int size) {

        // 1. 필수 엔티티 조회 및 검사 (링크 목록은 이 단계에서 로드하지 않는다)
        Emotion selectedEmotion = validateAndFetchContext(userId, situationId, emotionId);

        // 2. 상황에 매핑된 카테고리 조회
        List<Long> mappedCategories = situationCategoryService.getCategoryIdsBySituation(situationId);

        // 2-1. TextMatch/KeywordMatch용 유저 콘텐츠 프로필 조회 (없으면 두 신호는 0으로 처리됨)
        Optional<UserContentProfile> contentProfile = userContentProfileRepository.findById(userId);
        String profileTsqueryText = contentProfile.map(UserContentProfile::getProfileTsqueryText).orElse(null);
        String profileText = contentProfile.map(UserContentProfile::getProfileText).orElse(null);

        // 3. novelty(최근에 안 본 것) quota만큼 먼저 뽑고, 나머지를 기존 7축 가중합(normal)으로 채운다.
        //    quota는 목표치일 뿐이라 novelty 후보가 모자라면 normal이 초과해서 채운다(borrow).
        //    주의: 아직 cursor 페이징으로 전환하지 않아서 page*quotaCount/page*normalCount로 오프셋을
        //    근사한다 — novelty 풀이 소진되기 전까지는 정확하지만, 소진 이후 페이지에서는 중복/누락이
        //    생길 수 있다(service/common/README.md "novelty quota" 참고, 후속 트랙: 커서 페이징 전환).
        List<UsersLinku> candidates = fetchNoveltyAndNormalCandidates(
                userId, selectedEmotion.getEmotionId(), situationId, mappedCategories,
                profileTsqueryText, profileText, page, size);

        if (candidates.isEmpty()) {
            return ApiResponse.onSuccess(Collections.emptyList());
        }

        // 4. DTO 변환
        List<LinkuResponseDTO.LinkuSimpleDTO> result = mapCandidatesToDto(candidates);

        // 5. keyword count 집계
        linkuViewService.recordRecommendKeywordCount(userId, emotionId, situationId);

        return ApiResponse.onSuccess(result);
    }

    // 3. novelty(situation/emotion만으로 정렬) quota + normal(7축 가중합) 조회 결과를 합친다.
    private List<UsersLinku> fetchNoveltyAndNormalCandidates(
            Long userId, Long selectedEmotionId, Long selectedSituationId, List<Long> mappedCategoryIds,
            String profileTsqueryText, String profileText, int page, int size) {

        RecommendScoreProperties.Novelty noveltyProps = recommendScoreProperties.novelty();
        LocalDateTime now = LocalDateTime.now();

        int quotaCount = (int) Math.round(size * noveltyProps.quotaRatio());
        quotaCount = Math.min(Math.max(quotaCount, 0), size);

        List<UsersLinku> noveltyCandidates = quotaCount == 0
                ? Collections.emptyList()
                : usersLinkuRepository.findNoveltyRecommendCandidates(
                        userId, selectedEmotionId, selectedSituationId,
                        now, noveltyProps.recencyThresholdDays(), page * quotaCount, quotaCount);

        int normalTarget = size - noveltyCandidates.size();
        List<UsersLinku> normalCandidates = normalTarget == 0
                ? Collections.emptyList()
                : usersLinkuRepository.findNormalRecommendCandidates(
                        userId, selectedEmotionId, selectedSituationId, mappedCategoryIds,
                        now, profileTsqueryText, profileText,
                        noveltyProps.recencyThresholdDays(), page * (size - quotaCount), normalTarget);

        // findNormalRecommendCandidates가 이미 novelty 조건을 제외하므로(서로소) 중복이 생길 수 없지만,
        // 안전하게 userLinkuId 기준으로 한 번 더 방어한다.
        Set<Long> seen = new LinkedHashSet<>();
        List<UsersLinku> merged = new ArrayList<>(noveltyCandidates.size() + normalCandidates.size());
        for (UsersLinku candidate : noveltyCandidates) {
            if (seen.add(candidate.getUserLinkuId())) merged.add(candidate);
        }
        for (UsersLinku candidate : normalCandidates) {
            if (seen.add(candidate.getUserLinkuId())) merged.add(candidate);
        }
        return merged;
    }

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
