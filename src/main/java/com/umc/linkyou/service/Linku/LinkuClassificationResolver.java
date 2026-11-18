package com.umc.linkyou.service.Linku;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.umc.linkyou.apiPayload.code.status.ErrorStatus;
import com.umc.linkyou.apiPayload.code.status.category.CategoryErrorStatus;
import com.umc.linkyou.apiPayload.code.status.folder.FolderErrorStatus;
import com.umc.linkyou.apiPayload.exception.GeneralException;
import com.umc.linkyou.domain.Linku;
import com.umc.linkyou.domain.classification.Category;
import com.umc.linkyou.domain.classification.Emotion;
import com.umc.linkyou.domain.classification.Situation;
import com.umc.linkyou.repository.EmotionRepository;
import com.umc.linkyou.repository.classification.CategoryRepository;
import com.umc.linkyou.repository.classification.SituationRepository;
import com.umc.linkyou.repository.usersFolderRepository.UsersFolderRepository;
import com.umc.linkyou.web.dto.linku.LinkuRequestDTO;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class LinkuClassificationResolver {
    private static final Long DEFAULT_CATEGORY_ID = 16L;
    private static final Long DEFAULT_EMOTION_ID = 2L;
    private static final Long DEFAULT_SITUATION_ID = 1L;

    private final CategoryRepository categoryRepository;
    private final EmotionRepository emotionRepository;
    private final SituationRepository situationRepository;
    private final UsersFolderRepository usersFolderRepository;

    // 업로드한 이미지가 버려지지 않게 S3 업로드 전에 분류와 폴더를 검증한다.
    public void validateBeforeUpload(
            Long userId,
            LinkuRequestDTO.LinkuCreateDTO dto,
            Optional<Linku> existingLinkuOpt,
            NewLinkuAiData aiData) {
        boolean userProvidedEmotion = dto.getEmotionId() != null && dto.getEmotionId() > 0;
        boolean userProvidedSituation = dto.getSituationId() != null && dto.getSituationId() > 0;
        resolveEmotion(userProvidedEmotion ? dto.getEmotionId() : null, null);
        resolveSituation(userProvidedSituation ? dto.getSituationId() : null, null);
        Category category =
                existingLinkuOpt
                        .map(Linku::getCategory)
                        .orElseGet(
                                () ->
                                        resolveCategory(
                                                aiData != null ? aiData.aiCategoryId() : null));
        usersFolderRepository
                .findFolderByUserIdAndCategory(userId, category)
                .orElseThrow(() -> new GeneralException(FolderErrorStatus._FOLDER_NOT_FOUND));
    }

    public Category resolveCategory(Long aiCategoryId) {
        return Optional.ofNullable(aiCategoryId)
                .flatMap(categoryRepository::findById)
                .or(() -> categoryRepository.findById(DEFAULT_CATEGORY_ID))
                .orElseThrow(() -> new GeneralException(CategoryErrorStatus._CATEGORY_NOT_FOUND));
    }

    public Emotion resolveEmotion(Long userEmotionId, Long aiEmotionId) {
        Long resolvedId =
                userEmotionId != null && userEmotionId > 0
                        ? userEmotionId
                        : aiEmotionId != null ? aiEmotionId : DEFAULT_EMOTION_ID;
        return emotionRepository
                .findById(resolvedId)
                .orElseThrow(() -> new GeneralException(ErrorStatus._EMOTION_NOT_FOUND));
    }

    public Situation resolveSituation(Long userSituationId, Long aiSituationId) {
        Long resolvedId =
                userSituationId != null && userSituationId > 0
                        ? userSituationId
                        : aiSituationId != null ? aiSituationId : DEFAULT_SITUATION_ID;
        return situationRepository
                .findById(resolvedId)
                .orElseThrow(() -> new GeneralException(ErrorStatus._SITUATION_NOT_FOUND));
    }
}
