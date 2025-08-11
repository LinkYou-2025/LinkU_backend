package com.umc.linkyou.service;

import com.umc.linkyou.apiPayload.code.status.ErrorStatus;
import com.umc.linkyou.apiPayload.exception.GeneralException;
import com.umc.linkyou.converter.AiArticleConverter;
import com.umc.linkyou.domain.*;
import com.umc.linkyou.domain.classification.*;
import com.umc.linkyou.domain.Linku;
import com.umc.linkyou.domain.mapping.SituationJob;
import com.umc.linkyou.domain.mapping.UsersLinku;
import com.umc.linkyou.googleImgParser.LinkToImageService;
import com.umc.linkyou.openApi.OpenAISummaryUtil;
import com.umc.linkyou.openApi.SummaryAnalysisResultDTO;
import com.umc.linkyou.repository.*;
import com.umc.linkyou.repository.linkuRepository.LinkuRepository;
import com.umc.linkyou.repository.classification.CategoryRepository;
import com.umc.linkyou.repository.classification.SituationRepository;
import com.umc.linkyou.repository.mapping.SituationJobRepository;
import com.umc.linkyou.repository.mapping.UsersLinkuRepository;
import com.umc.linkyou.web.dto.AiArticleResponsetDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiArticleServiceImpl implements AiArticleService {

    private final UserRepository userRepository;
    private final LinkuRepository linkuRepository;
    private final SituationJobRepository situationJobRepository;
    private final EmotionRepository emotionRepository;
    private final CategoryRepository categoryRepository;
    private final SituationRepository situationRepository;
    private final AiArticleRepository aiArticleRepository;
    private final UsersLinkuRepository usersLinkuRepository;
    private final OpenAISummaryUtil openAISummaryUtil;
    private final LinkToImageService linkToImageService;

    /**
     * 생성
     * [memo, 유저 감정 등은 users_linku 쿼리해서] 같이 dto로 보냄
     */
    @Override
    @Transactional
    public AiArticleResponsetDTO.AiArticleResultDTO saveAiArticle(Long linkuId, Long userId) {

        // 1. linku, 유저 조회
        Linku linku = linkuRepository.findById(linkuId)
                .orElseThrow(() -> new GeneralException(ErrorStatus._BAD_REQUEST));
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorStatus._USER_NOT_FOUND));
        Job job = user.getJob();

        // 2. Job에 해당하는 Situation 조회
        List<Situation> situations = situationJobRepository.findAllByJob(job).stream()
                .map(SituationJob::getSituation)
                .toList();
        if (situations.isEmpty()) throw new GeneralException(ErrorStatus._DOMAIN_NOT_FOUND);

        // 3. Emotion, Category 전체 조회
        List<Emotion> emotions = emotionRepository.findAll();
        List<Category> categories = categoryRepository.findAll();

        if (emotions.isEmpty()) throw new GeneralException(ErrorStatus._EMOTION_NOT_FOUND);
        if (categories.isEmpty()) throw new GeneralException(ErrorStatus._CATEGORY_NOT_FOUND);

        // 4. OpenAI 호출
        SummaryAnalysisResultDTO result;
        try {
            result = openAISummaryUtil.getFullAnalysis(
                    linku.getLinku(), situations, emotions, categories
            );
        } catch (IOException e) {
            log.error("[AI JSON 파싱 실패 또는 응답 오류]: {}", e.getMessage(), e);
            throw new GeneralException(ErrorStatus._AI_INVALID_RESPONSE);
        }


        //이미지 받아오기
        String imageUrl = linkToImageService.getRelatedImageFromUrl(linku.getLinku(),linku.getTitle());

        // 5. id 기반 Entity 조인
        Situation selectedSituation = situationRepository.findById(result.getSituationId())
                .orElseThrow(() -> new GeneralException(ErrorStatus._SITUATION_NOT_FOUND));
        Emotion selectedEmotion = emotionRepository.findById(result.getEmotionId())
                .orElseThrow(() -> new GeneralException(ErrorStatus._EMOTION_NOT_FOUND));
        Category selectedCategory = categoryRepository.findById(result.getCategoryId())
                .orElseThrow(() -> new GeneralException(ErrorStatus._CATEGORY_NOT_FOUND));

        // 6. ai_article 저장 (usersLinku 더이상 저장 안 함, linku 기준)
        AiArticle article = AiArticleConverter.toEntity(
                result,
                selectedSituation,
                selectedEmotion,
                selectedCategory,
                linku,
                imageUrl
        );
        AiArticle saved = aiArticleRepository.save(article);

        // 7. 유저 개별정보(memo 등)는 users_linku에서 조회 (없으면 null 가능)
        UsersLinku usersLinku = usersLinkuRepository.findByUserAndLinku(user, linku)
                .orElse(null);

        // 8. 반환 DTO 생성 (memo 등 usersLinku 정보도 포함)
        return AiArticleConverter.toDto(
                saved,
                linku,
                usersLinku,
                selectedSituation,
                selectedEmotion,
                selectedCategory
        );
    }

    /**
     * ai_article 단순조회 (user 개별 메모/감정 포함)
     */
    public AiArticleResponsetDTO.AiArticleResultDTO showAiArticle(Long linkuId, Long userId) {
        Linku linku = linkuRepository.findById(linkuId)
                .orElseThrow(() -> new GeneralException(ErrorStatus._BAD_REQUEST));
        AiArticle article = aiArticleRepository.findByLinku(linku)
                .orElseThrow(() -> new GeneralException(ErrorStatus._AI_ARTICLE_NOT_FOUND));
        // 유저 개별 정보(memo, 감정 등)는 users_linku에서 별도 조회
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorStatus._USER_NOT_FOUND));
        UsersLinku usersLinku = usersLinkuRepository.findByUserAndLinku(user, linku)
                .orElse(null);

        Situation situation = article.getSituation();
        Emotion emotion = null;
        if (article.getAiFeelingId() != null)
            emotion = emotionRepository.findById(article.getAiFeelingId()).orElse(null);
        Category category = null;
        if (article.getAiCategoryId() != null)
            category = categoryRepository.findById(article.getAiCategoryId()).orElse(null);

        return AiArticleConverter.toDto(
                article,
                linku,
                usersLinku,
                situation,
                emotion,
                category
        );
    }
}
