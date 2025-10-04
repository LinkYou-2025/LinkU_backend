package com.umc.linkyou.service.Linku;

import com.umc.linkyou.apiPayload.ApiResponse;
import com.umc.linkyou.apiPayload.code.status.ErrorStatus;
import com.umc.linkyou.apiPayload.exception.GeneralException;
import com.umc.linkyou.converter.LinkuConverter;
import com.umc.linkyou.converter.LogConverter;
import com.umc.linkyou.domain.Linku;
import com.umc.linkyou.domain.Users;
import com.umc.linkyou.domain.classification.Domain;
import com.umc.linkyou.domain.classification.Emotion;
import com.umc.linkyou.domain.classification.Situation;
import com.umc.linkyou.domain.mapping.UsersLinku;
import com.umc.linkyou.repository.EmotionRepository;
import com.umc.linkyou.repository.LogRepository.EmotionLogRepository;
import com.umc.linkyou.repository.LogRepository.SituationLogRepository;
import com.umc.linkyou.repository.aiArticleRepository.AiArticleRepository;
import com.umc.linkyou.repository.classification.SituationRepository;
import com.umc.linkyou.repository.mapping.SituationJobRepository;
import com.umc.linkyou.repository.mapping.UsersLinkuRepository;
import com.umc.linkyou.repository.userRepository.UserRepository;
import com.umc.linkyou.utils.EmotionSimilarityUtil;
import com.umc.linkyou.web.dto.linku.LinkuInternalDTO;
import com.umc.linkyou.web.dto.linku.LinkuResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LinkuRecommendServiceImpl implements LinkuRecommendService{

    private final EmotionRepository emotionRepository;
    private final UsersLinkuRepository usersLinkuRepository;
    private final UserRepository userRepository;
    private final SituationRepository situationRepository;
    private final SituationLogRepository situationLogRepository;
    private final EmotionLogRepository emotionLogRepository;
    private final SituationJobRepository situationJobRepository;
    private final AiArticleRepository aiArticleRepository;


    private final SituationCategoryService situationCategoryService;


    @Override
    @Transactional
    public ApiResponse<List<LinkuResponseDTO.LinkuSimpleDTO>> recommendLinku(
            Long userId, Long situationId, Long emotionId, int page, int size) {

        // 1. 필수 엔티티 조회 및 검사
        EntitiesContext context = validateAndFetchEntities(userId, situationId, emotionId);

        // 2. 점수 계산 및 정렬
        List<LinkuInternalDTO.ScoredLinkuDTO> scoredList = calculateScoresAndSort(context.userLinkus, context.mappedCategories, context.selectedEmotion);

        // 3. 페이징 처리
        List<LinkuInternalDTO.ScoredLinkuDTO> pagedList = paginate(scoredList, page, size);

        // 4. 추천 링크 없으면 빈 리스트 반환
        if (pagedList.isEmpty()) {
            return ApiResponse.onSuccess(Collections.emptyList());
        }

        // 5. AI 아티클 존재 여부 일괄 조회 및 DTO 변환
        List<LinkuResponseDTO.LinkuSimpleDTO> result = mapPagedListToDto(pagedList, userId);

        // 6. 결과 반환
        return ApiResponse.onSuccess(result);
    }

    // 컨텍스트 저장용 레코드
    private record EntitiesContext(List<UsersLinku> userLinkus, List<Long> mappedCategories, Emotion selectedEmotion) {}

    // 1. 필수 엔티티 조회 및 입력 검증
    private EntitiesContext validateAndFetchEntities(Long userId, Long situationId, Long emotionId) {
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorStatus._USER_NOT_FOUND));
        Emotion selectedEmotion = emotionRepository.findById(emotionId)
                .orElseThrow(() -> new GeneralException(ErrorStatus._EMOTION_NOT_FOUND));
        Situation selectedSituation = situationRepository.findById(situationId)
                .orElseThrow(() -> new GeneralException(ErrorStatus._SITUATION_NOT_FOUND));

        Long jobId = user.getJob().getId();
        situationJobRepository.findBySituation_IdAndJob_Id(situationId, jobId)
                .orElseThrow(() -> new GeneralException(ErrorStatus._SITUATION_NOT_FOUND));

        List<UsersLinku> userLinkus = usersLinkuRepository.findByUser_Id(userId);
        if (userLinkus.isEmpty())
            throw new GeneralException(ErrorStatus._RECOMMEND_LINKU_NEW_USER);
        if (userLinkus.size() < 3)
            throw new GeneralException(ErrorStatus._RECOMMEND_LINKU_NOT_ENOUGH_LINKS);

        List<Long> mappedCategories = situationCategoryService.getCategoryIdsBySituation(situationId);

        // 행동 로그 저장
        situationLogRepository.save(LogConverter.toSituationLog(user, situationJobRepository.findBySituation_IdAndJob_Id(situationId, jobId).get()));
        emotionLogRepository.save(LogConverter.toEmotionLog(user, selectedEmotion));

        return new EntitiesContext(userLinkus, mappedCategories, selectedEmotion);
    }

    // 2. 점수 계산과 정렬
    private List<LinkuInternalDTO.ScoredLinkuDTO> calculateScoresAndSort(
            List<UsersLinku> userLinkus,
            List<Long> mappedCategories,
            Emotion selectedEmotion) {

        return userLinkus.stream()
                .map(linku -> {
                    int emotionScore = EmotionSimilarityUtil.getSimilarityScore(
                            linku.getEmotion().getEmotionId(),
                            selectedEmotion.getEmotionId());

                    Long aiCategoryId = null;
                    if (linku.getLinku() != null && linku.getLinku().getAiArticle() != null) {
                        aiCategoryId = linku.getLinku().getAiArticle().getAiCategoryId();
                    }

                    int situationScore = aiCategoryId == null ? 1 : (mappedCategories.contains(aiCategoryId) ? 2 : 0);

                    int totalScore = emotionScore + situationScore;

                    return LinkuInternalDTO.ScoredLinkuDTO.builder()
                            .userLinku(linku)
                            .emotionScore(emotionScore)
                            .situationScore(situationScore)
                            .totalScore(totalScore)
                            .build();
                })
                .sorted(Comparator.<LinkuInternalDTO.ScoredLinkuDTO>comparingInt(dto -> dto.getTotalScore() == 0 ? Integer.MIN_VALUE : dto.getTotalScore())
                        .reversed()
                        .thenComparing(dto -> dto.getUserLinku().getCreatedAt(), Comparator.reverseOrder()))
                .collect(Collectors.toList());
    }

    // 3. 페이징 처리
    private List<LinkuInternalDTO.ScoredLinkuDTO> paginate(List<LinkuInternalDTO.ScoredLinkuDTO> scoredList, int page, int size) {
        int fromIndex = page * size;
        int toIndex = Math.min(fromIndex + size, scoredList.size());
        if (fromIndex > scoredList.size()) {
            return Collections.emptyList();
        }
        return scoredList.subList(fromIndex, toIndex);
    }

    // 4. AI 아티클 존재 여부 조회 및 DTO 변환
    private List<LinkuResponseDTO.LinkuSimpleDTO> mapPagedListToDto(List<LinkuInternalDTO.ScoredLinkuDTO> pagedList, Long userId) {
        List<Long> linkuIds = pagedList.stream()
                .map(scored -> scored.getUserLinku().getLinku().getLinkuId())
                .collect(Collectors.toList());

        // 한 번에 AiArticle 조회 및 title 유효성 체크 후 존재 여부 Map 생성
        Map<Long, Boolean> aiArticleExistsMap = aiArticleRepository.existsAiArticleByLinkuIds(linkuIds);

        return pagedList.stream()
                .map(scored -> {
                    UsersLinku userLinku = scored.getUserLinku();
                    Linku linku = userLinku.getLinku();
                    Domain domain = linku.getDomain();

                    boolean aiArticleExists = Boolean.TRUE.equals(userLinku.getIsAiExist());

                    return LinkuConverter.toLinkuSimpleDTO(
                            linku,
                            userLinku,
                            domain,
                            aiArticleExists
                    );
                })
                .collect(Collectors.toList());
    }
}
