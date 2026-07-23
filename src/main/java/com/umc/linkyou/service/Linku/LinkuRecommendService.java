package com.umc.linkyou.service.Linku;

import com.umc.linkyou.apiPayload.ApiResponse;
import com.umc.linkyou.apiPayload.code.status.ErrorStatus;
import com.umc.linkyou.apiPayload.code.status.user.UserErrorStatus;
import com.umc.linkyou.apiPayload.exception.GeneralException;
import com.umc.linkyou.converter.LinkuConverter;
import com.umc.linkyou.domain.Linku;
import com.umc.linkyou.domain.Users;
import com.umc.linkyou.domain.classification.Emotion;
import com.umc.linkyou.domain.mapping.LinkuFolder;
import com.umc.linkyou.domain.mapping.SituationJob;
import com.umc.linkyou.domain.mapping.UsersLinku;
import com.umc.linkyou.repository.EmotionRepository;
import com.umc.linkyou.repository.classification.SituationRepository;
import com.umc.linkyou.repository.mapping.SituationJobRepository;
import com.umc.linkyou.repository.mapping.linkuFolderRepository.LinkuFolderRepository;
import com.umc.linkyou.repository.UserLinkuRepository.UsersLinkuRepository;
import com.umc.linkyou.repository.userRepository.UserRepository;
import com.umc.linkyou.web.dto.linku.LinkuResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 홈화면 링크 추천.
 * 큐레이션 내부/외부 추천(service.curation.recommend.*)과는 별개의 독립된 로직이다.
 *
 * 점수 계산·정렬·페이징을 전부 DB(QueryDSL)에서 처리한다.
 * - 감정 유사도 / 상황-카테고리 매칭 점수: UsersLinkuRepositoryImpl#findHomeRecommendCandidates 의 CASE WHEN 식
 * - 정렬 + LIMIT/OFFSET: 같은 쿼리에서 처리 (애플리케이션 메모리로 전체 링크를 올리지 않음)
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

    @Transactional(readOnly = true)
    public ApiResponse<List<LinkuResponseDTO.LinkuSimpleDTO>> recommendLinku(
            Long userId, Long situationId, Long emotionId, int page, int size) {

        // 1. 필수 엔티티 조회 및 검사 (링크 목록은 이 단계에서 로드하지 않는다)
        Emotion selectedEmotion = validateAndFetchContext(userId, situationId, emotionId);

        // 2. 상황에 매핑된 카테고리 조회
        List<Long> mappedCategories = situationCategoryService.getCategoryIdsBySituation(situationId);

        // 3. DB에서 점수 계산 + 정렬 + 페이징까지 마친 후보 조회
        List<UsersLinku> candidates = usersLinkuRepository.findHomeRecommendCandidates(
                userId, selectedEmotion.getEmotionId(), mappedCategories, page * size, size);

        if (candidates.isEmpty()) {
            return ApiResponse.onSuccess(Collections.emptyList());
        }

        // 4. DTO 변환
        List<LinkuResponseDTO.LinkuSimpleDTO> result = mapCandidatesToDto(candidates);

        // 5. keyword count 집계
        linkuViewService.recordRecommendKeywordCount(userId, emotionId, situationId);

        return ApiResponse.onSuccess(result);
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
